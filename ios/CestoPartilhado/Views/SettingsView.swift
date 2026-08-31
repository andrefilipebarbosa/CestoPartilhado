import SwiftUI
import UIKit

struct SettingsView: View {
    @EnvironmentObject private var auth: AuthService
    @ObservedObject private var localization = LocalizationManager.shared
    private let exportService = ExportService()

    @State private var isExporting = false
    @State private var exportedText: String?
    @State private var showingShareSheet = false

    var body: some View {
        NavigationStack {
            Form {
                Section(L("settings_language")) {
                    languageRow(LocalizationManager.languageSystem, L("settings_language_system"))
                    languageRow(LocalizationManager.languagePT, L("settings_language_pt"))
                    languageRow(LocalizationManager.languageEN, L("settings_language_en"))
                }

                Section(L("settings_export_title")) {
                    Text(L("settings_export_subtitle")).font(.footnote).foregroundColor(.sslText3)
                    Button {
                        exportData()
                    } label: {
                        if isExporting { ProgressView() } else { Text(L("settings_export_button")) }
                    }
                    .disabled(isExporting)
                }

                Section(L("settings_legal")) {
                    NavigationLink(L("terms_of_service")) { LegalDocView(assetBaseName: "terms-of-service") }
                    NavigationLink(L("privacy_policy")) { LegalDocView(assetBaseName: "privacy-policy") }
                }

                Section {
                    Button(role: .destructive) {
                        try? auth.signOut()
                    } label: {
                        Text(L("settings_sign_out"))
                    }
                }
            }
            .navigationTitle(L("settings_title"))
            .sheet(isPresented: $showingShareSheet) {
                if let exportedText {
                    ActivityView(activityItems: [exportedText])
                }
            }
        }
    }

    @ViewBuilder
    private func languageRow(_ value: String, _ label: String) -> some View {
        Button {
            localization.setLanguage(value)
        } label: {
            HStack {
                Text(label).foregroundColor(.sslText)
                Spacer()
                if localization.currentSetting == value {
                    Image(systemName: "checkmark").foregroundColor(.sslGreen)
                }
            }
        }
    }

    private func exportData() {
        guard let uid = auth.currentUser?.uid else { return }
        isExporting = true
        Task {
            exportedText = try? await exportService.buildExportText(uid: uid, accountEmail: auth.accountEmail)
            isExporting = false
            showingShareSheet = true
        }
    }
}

/// Wrapper do menu de partilha nativo do iOS (equivalente ao Intent.ACTION_SEND do Android).
struct ActivityView: UIViewControllerRepresentable {
    let activityItems: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: activityItems, applicationActivities: nil)
    }
    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
