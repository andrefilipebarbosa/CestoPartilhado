package pt.cestopartilhado.app.data

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import pt.cestopartilhado.app.model.UserProfile

/**
 * Autenticação com o Google via Firebase Auth.
 *
 * Precisa do "Web client ID" (o do tipo "3" no google-services.json / o client OAuth
 * "Web application" criado automaticamente pela Firebase) para o pedido de idToken —
 * usa-se aqui a string de recursos gerada automaticamente pelo plugin google-services
 * (default_web_client_id), por isso não há nada a configurar à mão neste ficheiro.
 */
class AuthRepository(
    private val context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val googleSignInClient: GoogleSignInClient by lazy {
        val webClientId = context.resources.getIdentifier(
            "default_web_client_id", "string", context.packageName
        )
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(webClientId))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, options)
    }

    val currentUser: FirebaseUser? get() = auth.currentUser

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun signInIntent(): Intent = googleSignInClient.signInIntent

    suspend fun handleSignInResult(data: Intent?): Result<FirebaseUser> = runCatching {
        val account = GoogleSignIn.getSignedInAccountFromIntent(data).await()
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        val user = auth.signInWithCredential(credential).await().user
            ?: error("Sign-in sem utilizador devolvido")
        upsertUserProfile(user)
        runCatching { FirebaseMessaging.getInstance().token.await() }
            .onSuccess { registerFcmToken(it) }
        user
    }

    /**
     * Chamar no arranque da app quando já há sessão iniciada — garante que o
     * `publicProfiles/{uid}` existe mesmo para contas que iniciaram sessão antes
     * desta funcionalidade existir (sem isto, ficariam para sempre a mostrar o
     * uid em vez do nome aos outros membros, já que `upsertUserProfile` só
     * corre durante o fluxo de login interativo).
     */
    suspend fun refreshPublicProfileIfSignedIn() {
        val user = auth.currentUser ?: return
        runCatching { upsertUserProfile(user) }
    }

    suspend fun signOut() {
        auth.signOut()
        googleSignInClient.signOut().await()
    }

    private suspend fun upsertUserProfile(user: FirebaseUser) {
        val profile = mapOf(
            "displayName" to (user.displayName ?: ""),
            "email" to (user.email ?: ""),
            "photoUrl" to (user.photoUrl?.toString()),
            "preferredLanguage" to pt.cestopartilhado.app.util.LocaleManager.resolvedLanguage(context),
        )
        // merge: um novo início de sessão nunca deve apagar fcmTokens/notificationPrefs
        // /activeListRef já guardados no documento.
        firestore.collection("users").document(user.uid).set(profile, SetOptions.merge()).await()

        // Subconjunto público (sem fcmTokens/prefs/activeListRef) — é o que outros
        // membros de uma lista partilhada conseguem ler, para mostrar o nome em vez do uid.
        val publicProfile = mapOf(
            "displayName" to (user.displayName ?: ""),
            "email" to (user.email ?: ""),
            "photoUrl" to (user.photoUrl?.toString()),
        )
        firestore.collection("publicProfiles").document(user.uid).set(publicProfile, SetOptions.merge()).await()
    }

    /** Nome a mostrar para outro membro de uma lista: nome de perfil, senão a parte antes do @ do email, senão o uid. */
    suspend fun getDisplayLabel(uid: String): String {
        return runCatching {
            val snap = firestore.collection("publicProfiles").document(uid).get().await()
            val name = snap.getString("displayName")?.takeIf { it.isNotBlank() }
            val email = snap.getString("email")?.takeIf { it.isNotBlank() }
            name ?: email?.substringBefore("@") ?: uid.take(8)
        }.getOrDefault(uid.take(8))
    }

    fun currentUserProfile(): UserProfile? = auth.currentUser?.let {
        UserProfile(
            uid = it.uid,
            displayName = it.displayName ?: it.email ?: "",
            email = it.email ?: "",
            photoUrl = it.photoUrl?.toString(),
        )
    }

    private fun userDoc(uid: String) = firestore.collection("users").document(uid)

    suspend fun registerFcmToken(token: String) {
        val uid = currentUser?.uid ?: return
        userDoc(uid).set(mapOf("fcmTokens" to FieldValue.arrayUnion(token)), SetOptions.merge()).await()
    }

    /** `ref` é "lists/{id}" ou "splitLists/{id}" enquanto o ecrã da lista está visível, ou null ao sair. */
    suspend fun setActiveListRef(ref: String?) {
        val uid = currentUser?.uid ?: return
        userDoc(uid).set(mapOf("activeListRef" to ref), SetOptions.merge()).await()
    }

    suspend fun setPreferredLanguage(language: String) {
        val uid = currentUser?.uid ?: return
        userDoc(uid).set(mapOf("preferredLanguage" to language), SetOptions.merge()).await()
    }

    suspend fun setNotificationPref(type: String, enabled: Boolean) {
        val uid = currentUser?.uid ?: return
        userDoc(uid).set(mapOf("notificationPrefs" to mapOf(type to enabled)), SetOptions.merge()).await()
    }

    /** Emite os valores atuais sempre que o documento do utilizador muda; usa os defaults do produto quando o campo ainda não existe. */
    fun observeNotificationPrefs(uid: String): Flow<Map<String, Boolean>> = callbackFlow {
        val defaults = mapOf("added" to true, "closed" to true, "edited" to false)
        val registration = userDoc(uid).addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            @Suppress("UNCHECKED_CAST")
            val stored = snapshot?.get("notificationPrefs") as? Map<String, Boolean> ?: emptyMap()
            trySend(defaults + stored)
        }
        awaitClose { registration.remove() }
    }
}
