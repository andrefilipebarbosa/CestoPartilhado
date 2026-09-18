import SwiftUI
import GoogleSignIn

@main
struct CestoPartilhadoApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @StateObject private var auth = AuthService()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(auth)
                .onOpenURL { url in
                    GIDSignIn.sharedInstance.handle(url)
                }
                .onAppear { AppDelegate.requestAuthorization() }
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
