import SwiftUI
import FirebaseFirestore
import UIKit

struct SettingsView: View {
    @EnvironmentObject private var auth: AuthService
    @ObservedObject private var localization = LocalizationManager.shared
    private let exportService = ExportService()

    @State private var isExporting = false
    @State private var exportedText: String?
    @State private var showingShareSheet = false
    @State private var notificationPrefs: [String: Bool] = ["added": true, "closed": true, "edited": false]
    @State private var notificationsListener: ListenerRegistration?

    var body: some View {
        NavigationStack {
            Form {
                Section(L("settings_language")) {
                    languageRow(LocalizationManager.languageSystem, L("settings_language_system"))
                    languageRow(LocalizationManager.languagePT, L("settings_language_pt"))
                    languageRow(LocalizationManager.languageEN, L("settings_language_en"))
                }

                Section(L("settings_notifications_title")) {
                    notificationToggle("added", L("notification_pref_added"))
                    notificationToggle("closed", L("notification_pref_closed"))
                    notificationToggle("edited", L("notification_pref_edited"))
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
            .onAppear {
                guard let uid = auth.currentUser?.uid else { return }
                notificationsListener = auth.observeNotificationPrefs(uid: uid) { notificationPrefs = $0 }
            }
            .onDisappear {
                notificationsListener?.remove()
                notificationsListener = nil
            }
        }
    }

    @ViewBuilder
    private func languageRow(_ value: String, _ label: String) -> some View {
        Button {
            localization.setLanguage(value)
            Task { await auth.setPreferredLanguage(localization.language) }
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

    @ViewBuilder
    private func notificationToggle(_ type: String, _ label: String) -> some View {
        Toggle(label, isOn: Binding(
            get: { notificationPrefs[type] ?? true },
            set: { newValue in
                notificationPrefs[type] = newValue
                Task { await auth.setNotificationPref(type: type, enabled: newValue) }
            }
        ))
        .foregroundColor(.sslText)
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
