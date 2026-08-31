import SwiftUI

struct NewListView: View {
    @EnvironmentObject private var auth: AuthService
    @Environment(\.dismiss) private var dismiss

    private let listsService = ListsService()
    private let catalogService = StoreCatalogService()

    @State private var name = ""
    @State private var storeQuery = ""
    @State private var suggestions: [StoreCatalogEntry] = []
    @State private var addedStores: [String] = []
    @State private var isCreating = false

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(L("new_list_name_label")).font(.caption).foregroundColor(.sslText2)
            TextField(L("new_list_name_placeholder"), text: $name)
                .textFieldStyle(.roundedBorder)

            Text(L("new_list_stores_label")).font(.headline).foregroundColor(.sslText).padding(.top, 20)
            Text(L("new_list_stores_subtitle")).font(.caption).foregroundColor(.sslText3)

            TextField(L("new_list_search_placeholder"), text: $storeQuery)
                .textFieldStyle(.roundedBorder)
                .onChange(of: storeQuery) { newValue in
                    Task {
                        suggestions = (try? await catalogService.search(newValue))?.filter { !addedStores.contains($0.name) } ?? []
                    }
                }

            if !storeQuery.isEmpty {
                VStack(alignment: .leading, spacing: 0) {
                    if suggestions.isEmpty {
                        Text(L("new_list_no_matches")).font(.footnote).foregroundColor(.sslText3).padding(12)
                        Button {
                            addStore(storeQuery)
                        } label: {
                            Text(String(format: L("new_list_add_store"), storeQuery))
                                .foregroundColor(.sslGreenDark)
                                .fontWeight(.semibold)
                                .padding(12)
                        }
                    } else {
                        ForEach(suggestions) { entry in
                            Button { addStore(entry.name) } label: {
                                Text(entry.name).foregroundColor(.sslText).padding(12)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }
                        }
                    }
                }
                .background(Color.sslSurface)
                .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color.sslBorder, lineWidth: 1))
            }

            Text(String(format: L("new_list_stores_added"), addedStores.count))
                .font(.caption).foregroundColor(.sslText2).padding(.top, 16)

            FlowLayout(spacing: 8) {
                ForEach(addedStores, id: \.self) { store in
                    HStack(spacing: 6) {
                        Text(store).font(.footnote).fontWeight(.semibold).foregroundColor(.sslText)
                        Button { addedStores.removeAll { $0 == store } } label: {
                            Image(systemName: "xmark").font(.caption2).foregroundColor(.sslText3)
                        }
                    }
                    .padding(.horizontal, 12).padding(.vertical, 8)
                    .background(Capsule().fill(Color.sslSurface2))
                }
            }

            Spacer()

            Button {
                createList()
            } label: {
                Text(L("new_list_create_button")).fontWeight(.bold)
                    .frame(maxWidth: .infinity).frame(height: 52)
            }
            .buttonStyle(.borderedProminent)
            .tint(.sslGreen)
            .disabled(isCreating || name.trimmingCharacters(in: .whitespaces).isEmpty)
        }
        .padding(20)
        .background(Color.sslBg)
        .navigationTitle(L("new_list_title"))
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button(L("action_cancel")) { dismiss() }
            }
        }
    }

    private func addStore(_ storeName: String) {
        let trimmed = storeName.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty, !addedStores.contains(trimmed) else { return }
        addedStores.append(trimmed)
        storeQuery = ""
        suggestions = []
    }

    private func createList() {
        guard let uid = auth.currentUser?.uid else { return }
        isCreating = true
        Task {
            do {
                let listId = try await listsService.createList(name: name.trimmingCharacters(in: .whitespaces), ownerUid: uid)
                for (index, store) in addedStores.enumerated() {
                    try await listsService.addStore(listId: listId, name: store, order: index)
                    try? await catalogService.registerUsage(store)
                }
            } catch {}
            isCreating = false
            dismiss()
        }
    }
}
