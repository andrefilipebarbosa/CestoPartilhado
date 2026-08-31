import SwiftUI
import FirebaseCore
import GoogleSignIn

@main
struct CestoPartilhadoApp: App {
    @StateObject private var auth = AuthService()

    init() {
        FirebaseApp.configure()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(auth)
                .onOpenURL { url in
                    GIDSignIn.sharedInstance.handle(url)
                }
        }
    }
}

struct RootView: View {
    @EnvironmentObject private var auth: AuthService

    var body: some View {
        if auth.currentUser != nil {
            MainTabView()
        } else {
            LoginView()
        }
    }
}
