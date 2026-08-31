import SwiftUI

/// Documento legal muito simples (título "# "/"## " a negrito, resto como parágrafos).
struct LegalDocView: View {
    let assetBaseName: String
    @State private var content = ""

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 6) {
                ForEach(Array(content.components(separatedBy: "\n").enumerated()), id: \.offset) { _, line in
                    if line.hasPrefix("## ") {
                        Text(line.replacingOccurrences(of: "## ", with: ""))
                            .font(.headline).foregroundColor(.sslText).padding(.top, 12)
                    } else if line.hasPrefix("# ") {
                        Text(line.replacingOccurrences(of: "# ", with: ""))
                            .font(.title2).bold().foregroundColor(.sslText)
                    } else if !line.isEmpty {
                        Text(line).font(.body).foregroundColor(.sslText2)
                    }
                }
            }
            .padding(20)
        }
        .background(Color.sslBg)
        .onAppear(perform: load)
    }

    private func load() {
        let lang = LocalizationManager.shared.language
        guard let url = Bundle.main.url(forResource: "\(assetBaseName).\(lang)", withExtension: "md", subdirectory: "Legal"),
              let text = try? String(contentsOf: url, encoding: .utf8) else { return }
        content = text
    }
}
