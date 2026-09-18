import SwiftUI

struct MainTabView: View {
    var body: some View {
        TabView {
            HomeNavigationView()
                .tabItem { Label(L("nav_lists"), systemImage: "house.fill") }
            ArchivedView()
                .tabItem { Label(L("nav_archive"), systemImage: "archivebox.fill") }
            SettingsView()
                .tabItem { Label(L("nav_settings"), systemImage: "gearshape.fill") }
        }
        .tint(.sslGreen)
    }
}

/// A aba "Listas" tem a sua própria navegação (para abrir Nova Lista / Detalhe / Convidar).
struct HomeNavigationView: View {
    var body: some View {
        NavigationStack {
            HomeView()
        }
    }
}
