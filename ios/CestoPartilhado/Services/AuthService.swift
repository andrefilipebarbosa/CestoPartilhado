import AuthenticationServices
import CryptoKit
import FacebookLogin
import Foundation
import FirebaseAuth
import FirebaseFirestore
import FirebaseMessaging
import GoogleSignIn
import UIKit

@MainActor
final class AuthService: ObservableObject {
    @Published var currentUser: User?

    private var handle: AuthStateDidChangeListenerHandle?

    init() {
        currentUser = Auth.auth().currentUser
        handle = Auth.auth().addStateDidChangeListener { [weak self] _, user in
            self?.currentUser = user
        }
        // Garante que publicProfiles/{uid} existe mesmo para contas que iniciaram
        // sessão antes desta funcionalidade existir (upsertUserProfile só corre
        // durante o fluxo de login interativo) — sem isto, ficariam para sempre
        // a mostrar o uid em vez do nome aos outros membros.
        if let user = currentUser {
            Task { try? await upsertUserProfile(user) }
        }
    }

    deinit {
        if let handle { Auth.auth().removeStateDidChangeListener(handle) }
    }

    var accountEmail: String { currentUser?.email ?? "" }
    var displayName: String { currentUser?.displayName ?? currentUser?.email ?? "" }

    func signIn() async throws {
        guard let rootViewController = Self.topViewController() else {
            throw NSError(domain: "AuthService", code: 1, userInfo: [NSLocalizedDescriptionKey: "No root view controller"])
        }
        let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: rootViewController)
        guard let idToken = result.user.idToken?.tokenString else {
            throw NSError(domain: "AuthService", code: 2, userInfo: [NSLocalizedDescriptionKey: "Missing Google ID token"])
        }
        let credential = GoogleAuthProvider.credential(withIDToken: idToken, accessToken: result.user.accessToken.tokenString)
        let authResult = try await Auth.auth().signIn(with: credential)
        try await finishSignIn(authResult)
    }

    func signInWithFacebook() async throws {
        guard let rootViewController = Self.topViewController() else {
            throw NSError(domain: "AuthService", code: 1, userInfo: [NSLocalizedDescriptionKey: "No root view controller"])
        }
        let loginResult = try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<LoginManagerLoginResult, Error>) in
            LoginManager().logIn(permissions: ["email", "public_profile"], from: rootViewController) { result, error in
                if let error {
                    continuation.resume(throwing: error)
                } else if let result, !result.isCancelled {
                    continuation.resume(returning: result)
                } else {
                    continuation.resume(throwing: NSError(domain: "AuthService", code: 4, userInfo: [NSLocalizedDescriptionKey: "Login cancelado"]))
                }
            }
        }
        guard let accessToken = loginResult.token else {
            throw NSError(domain: "AuthService", code: 5, userInfo: [NSLocalizedDescriptionKey: "Missing Facebook access token"])
        }
        let credential = FacebookAuthProvider.credential(withAccessToken: accessToken.tokenString)
        let authResult = try await Auth.auth().signIn(with: credential)
        try await finishSignIn(authResult)
    }

    /// Chamar a partir de `SignInWithAppleButton(onCompletion:)` — `rawNonce` tem de
    /// ser o mesmo (não codificado) que foi passado a `sha256` em `onRequest`.
    func completeAppleSignIn(authorization: ASAuthorization, rawNonce: String) async throws {
        guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
              let idTokenData = credential.identityToken,
              let idTokenString = String(data: idTokenData, encoding: .utf8) else {
            throw NSError(domain: "AuthService", code: 6, userInfo: [NSLocalizedDescriptionKey: "Falha ao obter credenciais da Apple"])
        }
        let firebaseCredential = OAuthProvider.credential(providerID: .apple, idToken: idTokenString, rawNonce: rawNonce)
        let authResult = try await Auth.auth().signIn(with: firebaseCredential)

        // A Apple só devolve o nome no primeiro login desta app — se vier, guarda-o
        // no perfil, porque o Firebase Auth não o preenche sozinho a partir disto.
        if let fullName = credential.fullName {
            let displayName = PersonNameComponentsFormatter().string(from: fullName)
                .trimmingCharacters(in: .whitespaces)
            if !displayName.isEmpty {
                let changeRequest = authResult.user.createProfileChangeRequest()
                changeRequest.displayName = displayName
                try? await changeRequest.commitChanges()
            }
        }
        try await finishSignIn(authResult)
    }

    static func randomNonceString(length: Int = 32) -> String {
        precondition(length > 0)
        var randomBytes = [UInt8](repeating: 0, count: length)
        let status = SecRandomCopyBytes(kSecRandomDefault, randomBytes.count, &randomBytes)
        if status != errSecSuccess {
            fatalError("Unable to generate nonce (OSStatus \(status)).")
        }
        let charset: [Character] = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")
        return String(randomBytes.map { charset[Int($0) % charset.count] })
    }

    static func sha256(_ input: String) -> String {
        SHA256.hash(data: Data(input.utf8)).compactMap { String(format: "%02x", $0) }.joined()
    }

    private func finishSignIn(_ authResult: AuthDataResult) async throws {
        try await upsertUserProfile(authResult.user)
        if let token = try? await Messaging.messaging().token() {
            try? await registerFcmToken(token)
        }
    }

    func signOut() throws {
        GIDSignIn.sharedInstance.signOut()
        LoginManager().logOut()
        try Auth.auth().signOut()
    }

    private func upsertUserProfile(_ user: User) async throws {
        let data: [String: Any] = [
            "displayName": user.displayName ?? "",
            "email": user.email ?? "",
            "photoUrl": user.photoURL?.absoluteString as Any,
            "preferredLanguage": LocalizationManager.shared.language,
        ]
        // merge: um novo início de sessão nunca deve apagar fcmTokens/notificationPrefs
        // /activeListRef já guardados no documento.
        try await Firestore.firestore().collection("users").document(user.uid).setData(data, merge: true)

        // Subconjunto público (sem fcmTokens/prefs/activeListRef) — é o que outros
        // membros de uma lista partilhada conseguem ler, para mostrar o nome em vez do uid.
        let publicProfile: [String: Any] = [
            "displayName": user.displayName ?? "",
            "email": user.email ?? "",
            "photoUrl": user.photoURL?.absoluteString as Any,
        ]
        try await Firestore.firestore().collection("publicProfiles").document(user.uid).setData(publicProfile, merge: true)
    }

    /// Nome a mostrar para outro membro de uma lista: nome de perfil, senão a parte antes do @ do email, senão o uid.
    /// `static` pelo mesmo motivo de `setActiveListRef` — chamável de qualquer ViewModel sem guardar uma instância.
    static func getDisplayLabel(_ uid: String) async -> String {
        guard let snap = try? await Firestore.firestore().collection("publicProfiles").document(uid).getDocument() else {
            return String(uid.prefix(8))
        }
        let name = (snap.data()?["displayName"] as? String)?.trimmingCharacters(in: .whitespaces)
        let email = (snap.data()?["email"] as? String)?.trimmingCharacters(in: .whitespaces)
        if let name, !name.isEmpty { return name }
        if let email, !email.isEmpty { return String(email.split(separator: "@").first ?? Substring(email)) }
        return String(uid.prefix(8))
    }

    private func userDoc(_ uid: String) -> DocumentReference { Firestore.firestore().collection("users").document(uid) }

    func registerFcmToken(_ token: String) async throws {
        guard let uid = currentUser?.uid else { return }
        try await userDoc(uid).setData(["fcmTokens": FieldValue.arrayUnion([token])], merge: true)
    }

    /// `ref` é "lists/{id}" ou "splitLists/{id}" enquanto o ecrã da lista está visível, ou nil ao sair.
    /// `static` (em vez de usar `currentUser`) para poder ser chamado a partir de qualquer
    /// ViewModel de ecrã de lista sem precisar de guardar uma referência à instância partilhada.
    static func setActiveListRef(_ ref: String?) async {
        guard let uid = Auth.auth().currentUser?.uid else { return }
        try? await Firestore.firestore().collection("users").document(uid)
            .setData(["activeListRef": ref as Any], merge: true)
    }

    func setPreferredLanguage(_ language: String) async {
        guard let uid = currentUser?.uid else { return }
        try? await userDoc(uid).setData(["preferredLanguage": language], merge: true)
    }

    func setNotificationPref(type: String, enabled: Bool) async {
        guard let uid = currentUser?.uid else { return }
        try? await userDoc(uid).setData(["notificationPrefs": [type: enabled]], merge: true)
    }

    /// Emite os valores atuais sempre que o documento do utilizador muda; usa os defaults do produto quando o campo ainda não existe.
    func observeNotificationPrefs(uid: String, onChange: @escaping ([String: Bool]) -> Void) -> ListenerRegistration {
        let defaults: [String: Bool] = ["added": true, "closed": true, "edited": false]
        return userDoc(uid).addSnapshotListener { snapshot, _ in
            let stored = snapshot?.data()?["notificationPrefs"] as? [String: Bool] ?? [:]
            onChange(defaults.merging(stored) { _, new in new })
        }
    }

    private static func topViewController() -> UIViewController? {
        guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let root = scene.windows.first(where: { $0.isKeyWindow })?.rootViewController else {
            return nil
        }
        var top = root
        while let presented = top.presentedViewController { top = presented }
        return top
    }
}
