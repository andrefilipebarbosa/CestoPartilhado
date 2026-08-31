import Foundation
import FirebaseAuth
import FirebaseFirestore
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
        try await upsertUserProfile(authResult.user)
    }

    func signOut() throws {
        GIDSignIn.sharedInstance.signOut()
        try Auth.auth().signOut()
    }

    private func upsertUserProfile(_ user: User) async throws {
        let data: [String: Any] = [
            "displayName": user.displayName ?? "",
            "email": user.email ?? "",
            "photoUrl": user.photoURL?.absoluteString as Any,
        ]
        try await Firestore.firestore().collection("users").document(user.uid).setData(data)
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
