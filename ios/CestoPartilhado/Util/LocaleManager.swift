import Foundation

/// Por omissão a app segue o idioma do telefone automaticamente (pt se o telefone
/// estiver em português, inglês caso contrário). Esta classe trata da escolha manual
/// nas Definições, que substitui esse comportamento automático enquanto estiver
/// definida, e é a única fonte de traduções da app (não usamos NSLocalizedString
/// diretamente, porque esse segue sempre o idioma do sistema, nunca a escolha manual).
final class LocalizationManager: ObservableObject {
    static let shared = LocalizationManager()

    static let languageSystem = "system"
    static let languagePT = "pt"
    static let languageEN = "en"

    private static let storageKey = "cesto_app_language"

    @Published private(set) var language: String
    private var bundle: Bundle

    private init() {
        let resolved = Self.resolveLanguage(setting: UserDefaults.standard.string(forKey: Self.storageKey))
        language = resolved
        bundle = Self.bundle(for: resolved)
    }

    /// `nil`/`"system"` volta a seguir o idioma do telefone.
    func setLanguage(_ setting: String) {
        if setting == Self.languageSystem {
            UserDefaults.standard.removeObject(forKey: Self.storageKey)
        } else {
            UserDefaults.standard.set(setting, forKey: Self.storageKey)
        }
        language = Self.resolveLanguage(setting: setting == Self.languageSystem ? nil : setting)
        bundle = Self.bundle(for: language)
    }

    var currentSetting: String { UserDefaults.standard.string(forKey: Self.storageKey) ?? Self.languageSystem }

    func t(_ key: String) -> String {
        bundle.localizedString(forKey: key, value: nil, table: nil)
    }

    private static func resolveLanguage(setting: String?) -> String {
        if let setting { return setting }
        let deviceIsPortuguese = Locale.preferredLanguages.first?.hasPrefix("pt") ?? false
        return deviceIsPortuguese ? languagePT : languageEN
    }

    private static func bundle(for language: String) -> Bundle {
        guard let path = Bundle.main.path(forResource: language, ofType: "lproj"),
              let bundle = Bundle(path: path) else {
            return .main
        }
        return bundle
    }
}

/// Atalho global para traduções — usa sempre isto em vez de `NSLocalizedString`.
func L(_ key: String) -> String { LocalizationManager.shared.t(key) }
