import { initializeApp } from "firebase-admin/app";
import { getFirestore, FieldValue, Timestamp } from "firebase-admin/firestore";
import { getAuth } from "firebase-admin/auth";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as functionsV1 from "firebase-functions/v1";
import * as logger from "firebase-functions/logger";

initializeApp();
const db = getFirestore();

export { onListWritten, onSplitListWritten } from "./notifications";

const THIRTY_DAYS_MS = 30 * 24 * 60 * 60 * 1000;

/**
 * Corre uma vez por dia. Apaga definitivamente (lista + lojas + artigos)
 * qualquer lista fechada há mais de 30 dias.
 */
export const purgeClosedLists = onSchedule(
  { schedule: "every 24 hours", region: "europe-west1" },
  async () => {
    const cutoff = Timestamp.fromMillis(Date.now() - THIRTY_DAYS_MS);
    const snapshot = await db
      .collection("lists")
      .where("status", "==", "closed")
      .where("closedAt", "<=", cutoff)
      .get();

    if (snapshot.empty) {
      logger.info("purgeClosedLists: nada para eliminar");
      return;
    }

    await Promise.all(
      snapshot.docs.map((doc) => db.recursiveDelete(doc.ref))
    );
    logger.info(`purgeClosedLists: eliminadas ${snapshot.size} lista(s)`);
  }
);

/** As duas coleções que suportam convite por email — validado antes de usar em queries. */
const INVITABLE_COLLECTIONS = ["lists", "splitLists"] as const;

/**
 * Chamada pelo dono da lista (de compras ou dividida) para convidar alguém
 * por email. Se a pessoa já tiver conta, é adicionada de imediato aos
 * membros. Caso contrário fica em "pendingInvites" até criar conta (ver
 * trigger abaixo).
 */
export const inviteMemberByEmail = onCall(
  { region: "europe-west1" },
  async (request) => {
    const uid = request.auth?.uid;
    if (!uid) {
      throw new HttpsError("unauthenticated", "É necessário iniciar sessão.");
    }

    const listId = request.data?.listId as string | undefined;
    const email = (request.data?.email as string | undefined)?.trim().toLowerCase();
    const collectionName = (request.data?.collection as string) === "splitLists" ? "splitLists" : "lists";
    if (!listId || !email) {
      throw new HttpsError("invalid-argument", "listId e email são obrigatórios.");
    }

    const listRef = db.collection(collectionName).doc(listId);
    const listSnap = await listRef.get();
    if (!listSnap.exists) {
      throw new HttpsError("not-found", "Lista não encontrada.");
    }
    const list = listSnap.data()!;
    if (list.ownerId !== uid) {
      throw new HttpsError(
        "permission-denied",
        "Só o criador da lista pode convidar pessoas."
      );
    }

    if (collectionName === "splitLists" && (list.paidMemberIds ?? []).length > 0) {
      throw new HttpsError(
        "failed-precondition",
        "A lista está bloqueada — reverte os pagamentos antes de convidar mais pessoas."
      );
    }

    if ((list.memberIds ?? []).includes(uid) && email === request.auth?.token.email) {
      throw new HttpsError("failed-precondition", "Já fazes parte desta lista.");
    }

    try {
      const invitedUser = await getAuth().getUserByEmail(email);
      await listRef.update({
        memberIds: FieldValue.arrayUnion(invitedUser.uid),
        pendingInvites: FieldValue.arrayRemove(email),
        updatedAt: FieldValue.serverTimestamp(),
      });
      return { status: "added" as const };
    } catch (err: any) {
      if (err?.code === "auth/user-not-found") {
        await listRef.update({
          pendingInvites: FieldValue.arrayUnion(email),
          updatedAt: FieldValue.serverTimestamp(),
        });
        return { status: "pending" as const };
      }
      logger.error("inviteMemberByEmail falhou", err);
      throw new HttpsError("internal", "Não foi possível convidar esta pessoa.");
    }
  }
);

/**
 * Quando alguém cria conta pela primeira vez, resolve automaticamente
 * qualquer convite pendente (em listas de compras ou divididas) feito para
 * o email dessa pessoa.
 */
export const resolvePendingInvitesOnSignUp = functionsV1
  .region("europe-west1")
  .auth.user()
  .onCreate(async (user) => {
    if (!user.email) return;
    const email = user.email.toLowerCase();

    for (const collectionName of INVITABLE_COLLECTIONS) {
      const snapshot = await db
        .collection(collectionName)
        .where("pendingInvites", "array-contains", email)
        .get();

      if (snapshot.empty) continue;

      const batch = db.batch();
      snapshot.docs.forEach((doc) => {
        batch.update(doc.ref, {
          memberIds: FieldValue.arrayUnion(user.uid),
          pendingInvites: FieldValue.arrayRemove(email),
          updatedAt: FieldValue.serverTimestamp(),
        });
      });
      await batch.commit();
      logger.info(
        `resolvePendingInvitesOnSignUp: ${user.uid} adicionado a ${snapshot.size} lista(s) em ${collectionName}`
      );
    }
  });
