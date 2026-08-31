import SwiftUI

struct NewSplitListView: View {
    @EnvironmentObject private var auth: AuthService
    @Environment(\.dismiss) private var dismiss
    private let service = SplitListsService()

    @State private var name = ""
    @State private var isCreating = false

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(L("new_list_name_label")).font(.caption).foregroundColor(.sslText2)
            TextField(L("split_new_name_placeholder"), text: $name)
                .textFieldStyle(.roundedBorder)
            Spacer()
            Button {
                createList()
            } label: {
                Text(L("split_new_create_button")).fontWeight(.bold)
                    .frame(maxWidth: .infinity).frame(height: 52)
            }
            .buttonStyle(.borderedProminent)
            .tint(.sslGreen)
            .disabled(isCreating || name.trimmingCharacters(in: .whitespaces).isEmpty)
        }
        .padding(20)
        .background(Color.sslBg)
        .navigationTitle(L("split_new_title"))
        .toolbar {
            ToolbarItem(placement: .cancellationAction) { Button(L("action_cancel")) { dismiss() } }
        }
    }

    private func createList() {
        guard let uid = auth.currentUser?.uid else { return }
        isCreating = true
        Task {
            _ = try? await service.createList(name: name.trimmingCharacters(in: .whitespaces), ownerUid: uid)
            isCreating = false
            dismiss()
        }
    }
}
