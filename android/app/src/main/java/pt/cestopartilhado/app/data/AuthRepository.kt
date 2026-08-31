package pt.cestopartilhado.app.data

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
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
        user
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
        )
        firestore.collection("users").document(user.uid).set(profile).await()
    }

    fun currentUserProfile(): UserProfile? = auth.currentUser?.let {
        UserProfile(
            uid = it.uid,
            displayName = it.displayName ?: it.email ?: "",
            email = it.email ?: "",
            photoUrl = it.photoUrl?.toString(),
        )
    }
}
