import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { onDocumentWritten } from "firebase-functions/v2/firestore";
import * as logger from "firebase-functions/logger";

const db = getFirestore();
const REGION = "europe-west1";

type NotificationType = "added" | "closed" | "edited";
type ListCollection = "lists" | "splitLists";

interface LocalizedText {
  pt: string;
  en: string;
}

/** Por omissão o utilizador só recebe "adicionado" e "concluída" — "editada" fica desligada. */
const DEFAULT_PREFS: Record<NotificationType, boolean> = {
  added: true,
  closed: true,
  edited: false,
};

/**
 * Envia uma notificação push aos membros indicados, respeitando:
 * - as preferências de cada um (`users/{uid}.notificationPrefs`);
 * - quem fez a alteração (nunca se notifica a si próprio — `excludeUid`);
 * - quem está a ver essa lista neste preciso momento
 *   (`users/{uid}.activeListRef` == "<coleção>/<listId>").
 * Limpa também tokens que o FCM reporte como inválidos/expirados.
 */
async function notifyMembers(params: {
  collectionName: ListCollection;
  listId: string;
  recipientUids: string[];
  excludeUid?: string;
  type: NotificationType;
  title: LocalizedText;
  body: LocalizedText;
}) {
  const { collectionName, listId, recipientUids, excludeUid, type, title, body } = params;
  const targets = [...new Set(recipientUids)].filter((uid) => uid !== excludeUid);
  if (targets.length === 0) return;

  const activeRef = `${collectionName}/${listId}`;
  const userSnaps = await db.getAll(...targets.map((uid) => db.collection("users").doc(uid)));

  const messages: { token: string; uid: string; notification: { title: string; body: string } }[] = [];

  for (const snap of userSnaps) {
    if (!snap.exists) continue;
    const data = snap.data() ?? {};
    const prefs = (data.notificationPrefs ?? {}) as Partial<Record<NotificationType, boolean>>;
    const enabled = prefs[type] ?? DEFAULT_PREFS[type];
    if (!enabled) continue;
    if (data.activeListRef === activeRef) continue; // está a ver a lista agora mesmo

    const tokens: string[] = Array.isArray(data.fcmTokens) ? data.fcmTokens : [];
    const lang = data.preferredLanguage === "en" ? "en" : "pt";
    for (const token of tokens) {
      messages.push({
        token,
        uid: snap.id,
        notification: { title: title[lang], body: body[lang] },
      });
    }
  }

  if (messages.length === 0) return;

  const response = await getMessaging().sendEach(
    messages.map((m) => ({
      token: m.token,
      notification: m.notification,
      data: { type, collection: collectionName, listId },
    }))
  );

  const staleByUser = new Map<string, string[]>();
  response.responses.forEach((r, i) => {
    if (r.success) return;
    const code = r.error?.code;
    if (code === "messaging/registration-token-not-registered" || code === "messaging/invalid-argument") {
      const { uid, token } = messages[i];
      staleByUser.set(uid, [...(staleByUser.get(uid) ?? []), token]);
    }
  });
  await Promise.all(
    Array.from(staleByUser.entries()).map(([uid, tokens]) =>
      db.collection("users").doc(uid).update({ fcmTokens: FieldValue.arrayRemove(...tokens) })
    )
  );

  logger.info(
    `notifyMembers: ${type} em ${activeRef} — ${response.successCount}/${messages.length} entregues`
  );
}

function newMemberIds(before: FirebaseFirestore.DocumentData, after: FirebaseFirestore.DocumentData): string[] {
  const beforeMembers: string[] = before.memberIds ?? [];
  const afterMembers: string[] = after.memberIds ?? [];
  return afterMembers.filter((uid) => !beforeMembers.includes(uid));
}

export const onListWritten = onDocumentWritten(
  { document: "lists/{listId}", region: REGION },
  async (event) => {
    const before = event.data?.before?.data();
    const after = event.data?.after?.data();
    if (!after || !before) return; // ignora criação e eliminação

    const listId = event.params.listId;
    const actorUid = after.updatedBy as string | undefined;
    const listName = (after.name as string) ?? "";
    const added = newMemberIds(before, after);

    if (added.length > 0) {
      await notifyMembers({
        collectionName: "lists",
        listId,
        recipientUids: added,
        type: "added",
        title: { pt: "Foste adicionado a uma lista", en: "You were added to a list" },
        body: {
          pt: `"${listName}" foi partilhada contigo.`,
          en: `"${listName}" was shared with you.`,
        },
      });
    }

    const otherMembers = ((after.memberIds as string[]) ?? []).filter((uid) => !added.includes(uid));
    const justClosed = before.status !== "closed" && after.status === "closed";
    if (justClosed) {
      await notifyMembers({
        collectionName: "lists",
        listId,
        recipientUids: otherMembers,
        excludeUid: actorUid,
        type: "closed",
        title: { pt: "Lista concluída", en: "List completed" },
        body: { pt: `"${listName}" foi fechada.`, en: `"${listName}" was closed.` },
      });
      return;
    }

    const contentChanged =
      before.name !== after.name ||
      before.storeCount !== after.storeCount ||
      before.itemCount !== after.itemCount ||
      before.boughtCount !== after.boughtCount;
    if (contentChanged) {
      await notifyMembers({
        collectionName: "lists",
        listId,
        recipientUids: otherMembers,
        excludeUid: actorUid,
        type: "edited",
        title: { pt: "Lista atualizada", en: "List updated" },
        body: { pt: `"${listName}" foi atualizada.`, en: `"${listName}" was updated.` },
      });
    }
  }
);

export const onSplitListWritten = onDocumentWritten(
  { document: "splitLists/{listId}", region: REGION },
  async (event) => {
    const before = event.data?.before?.data();
    const after = event.data?.after?.data();
    if (!after || !before) return;

    const listId = event.params.listId;
    const actorUid = after.updatedBy as string | undefined;
    const listName = (after.name as string) ?? "";
    const added = newMemberIds(before, after);

    if (added.length > 0) {
      await notifyMembers({
        collectionName: "splitLists",
        listId,
        recipientUids: added,
        type: "added",
        title: { pt: "Foste adicionado a uma lista dividida", en: "You were added to a split list" },
        body: {
          pt: `"${listName}" foi partilhada contigo.`,
          en: `"${listName}" was shared with you.`,
        },
      });
    }

    const otherMembers = ((after.memberIds as string[]) ?? []).filter((uid) => !added.includes(uid));
    const contentChanged =
      before.name !== after.name ||
      before.itemCount !== after.itemCount ||
      before.totalValue !== after.totalValue;
    if (contentChanged) {
      await notifyMembers({
        collectionName: "splitLists",
        listId,
        recipientUids: otherMembers,
        excludeUid: actorUid,
        type: "edited",
        title: { pt: "Lista dividida atualizada", en: "Split list updated" },
        body: { pt: `"${listName}" foi atualizada.`, en: `"${listName}" was updated.` },
      });
    }
  }
);
