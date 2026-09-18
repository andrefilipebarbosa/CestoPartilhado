import SwiftUI

struct MainTabView: View {
    var body: some View {
        TabView {
            HomeNavigationView()
                .tabItem { Label(L("nav_lists"), systemImage: "house.fill") }
                .accessibilityIdentifier("tabLists")
            ArchivedView()
                .tabItem { Label(L("nav_archive"), systemImage: "archivebox.fill") }
                .accessibilityIdentifier("tabArchive")
            SettingsView()
                .tabItem { Label(L("nav_settings"), systemImage: "gearshape.fill") }
                .accessibilityIdentifier("tabSettings")
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
