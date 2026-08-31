import SwiftUI

@MainActor
final class ListDetailViewModel: ObservableObject {
    @Published var state: ListWithStores?
    @Published var errorMessage: String?

    private let service = ListsService()
    private var observation: ListObservation?
    private(set) var currentUid: String?

    var isOwner: Bool { state?.list.ownerId == currentUid }

    func start(listId: String, uid: String) {
        currentUid = uid
        observation?.stop()
        observation = service.observeListWithStores(listId: listId) { [weak self] result in
            self?.state = result
        }
    }

    func addStore(_ name: String) {
        guard let listId = state?.list.id else { return }
        let nextOrder = state?.stores.count ?? 0
        Task { try? await service.addStore(listId: listId, name: name, order: nextOrder) }
    }

    func removeStore(_ storeId: String) {
        guard let listId = state?.list.id else { return }
        Task { try? await service.removeStore(listId: listId, storeId: storeId) }
    }

    func addItem(storeId: String, name: String) {
        guard let listId = state?.list.id, let uid = currentUid else { return }
        Task { try? await service.addItem(listId: listId, storeId: storeId, name: name, note: "", uid: uid) }
    }

    func toggleItem(storeId: String, itemId: String, bought: Bool) {
        guard let listId = state?.list.id, let uid = currentUid else { return }
        Task { try? await service.setItemBought(listId: listId, storeId: storeId, itemId: itemId, bought: bought, uid: uid) }
    }

    func removeItem(storeId: String, itemId: String, wasBought: Bool) {
        guard let listId = state?.list.id else { return }
        Task { try? await service.removeItem(listId: listId, storeId: storeId, itemId: itemId, wasBought: wasBought) }
    }

    func closeList() {
        guard let listId = state?.list.id else { return }
        Task { try? await service.setListStatus(listId: listId, status: ShoppingList.statusClosed) }
    }

    func reopenList() {
        guard let listId = state?.list.id else { return }
        Task { try? await service.setListStatus(listId: listId, status: ShoppingList.statusActive) }
    }

    /// As regras do Firestore também impedem isto — aqui só evitamos mostrar a opção a quem não é dono.
    func deleteList(onDeleted: @escaping () -> Void) {
        guard let listId = state?.list.id, isOwner else { return }
        Task {
            try? await service.deleteList(listId: listId)
            onDeleted()
        }
    }

    func stop() { observation?.stop() }
}

struct ListDetailView: View {
    let listId: String
    @EnvironmentObject private var auth: AuthService
    @StateObject private var viewModel = ListDetailViewModel()
    @Environment(\.dismiss) private var dismiss

    @State private var addingStore = false
    @State private var newStoreName = ""
    @State private var showingInvite = false

    var body: some View {
        Group {
            if let state = viewModel.state {
                content(state)
            } else {
                ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity).background(Color.sslBg)
            }
        }
        .onAppear {
            if let uid = auth.currentUser?.uid { viewModel.start(listId: listId, uid: uid) }
        }
        .onDisappear { viewModel.stop() }
        .sheet(isPresented: $showingInvite) {
            NavigationStack { InviteView(listId: listId) }
        }
    }

    @ViewBuilder
    private func content(_ state: ListWithStores) -> some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    Text("\(String(format: L("list_detail_stores_count"), state.stores.count))  ·  \(String(format: L("home_items_progress"), state.boughtItems, state.totalItems))")
                        .font(.footnote).foregroundColor(.sslText2)

                    if addingStore {
                        HStack {
                            TextField(L("list_detail_add_store"), text: $newStoreName).textFieldStyle(.roundedBorder)
                            Button {
                                if !newStoreName.trimmingCharacters(in: .whitespaces).isEmpty {
                                    viewModel.addStore(newStoreName.trimmingCharacters(in: .whitespaces))
                                    newStoreName = ""; addingStore = false
                                }
                            } label: { Image(systemName: "checkmark.circle.fill").foregroundColor(.sslGreen) }
                        }
                    }

                    ForEach(state.stores) { storeWithItems in
                        StoreSectionView(
                            storeWithItems: storeWithItems,
                            onToggle: { itemId, bought in viewModel.toggleItem(storeId: storeWithItems.store.id ?? "", itemId: itemId, bought: bought) },
                            onRemoveItem: { item in viewModel.removeItem(storeId: storeWithItems.store.id ?? "", itemId: item.id ?? "", wasBought: item.bought) },
                            onAddItem: { name in viewModel.addItem(storeId: storeWithItems.store.id ?? "", name: name) },
                            onRemoveStore: { viewModel.removeStore(storeWithItems.store.id ?? "") }
                        )
                    }
                }
                .padding(20)
            }

            Divider()
            VStack {
                if state.list.status == ShoppingList.statusActive {
                    Button {
                        viewModel.closeList()
                    } label: {
                        Label(L("action_close_list"), systemImage: "checkmark")
                            .fontWeight(.bold).frame(maxWidth: .infinity).frame(height: 52)
                    }
                    .buttonStyle(.borderedProminent).tint(.sslGreen)
                } else {
                    Button {
                        viewModel.reopenList()
                    } label: {
                        Text(L("action_recover")).fontWeight(.bold).frame(maxWidth: .infinity).frame(height: 52)
                    }
                    .buttonStyle(.bordered).tint(.sslGreen)
                }
            }
            .padding(20)
            .background(Color.sslSurface)
        }
        .background(Color.sslBg)
        .navigationTitle(state.list.name)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Menu {
                    Button(L("invite_title")) { showingInvite = true }
                    Button(L("list_detail_add_store")) { addingStore = true }
                    if viewModel.isOwner {
                        Button(L("list_detail_delete_button"), role: .destructive) {
                            viewModel.deleteList { dismiss() }
                        }
                    }
                } label: {
                    Image(systemName: "ellipsis.circle").foregroundColor(.sslText)
                }
            }
        }
    }
}

private struct StoreSectionView: View {
    let storeWithItems: StoreWithItems
    let onToggle: (String, Bool) -> Void
    let onRemoveItem: (ShoppingItem) -> Void
    let onAddItem: (String) -> Void
    let onRemoveStore: () -> Void

    @State private var expanded = true
    @State private var newItemName = ""

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Button {
                expanded.toggle()
            } label: {
                HStack {
                    Text(storeWithItems.store.name).font(.headline).foregroundColor(.sslText)
                    Spacer()
                    Text("\(storeWithItems.boughtCount)/\(storeWithItems.items.count)").font(.footnote).foregroundColor(.sslText3)
                    Image(systemName: expanded ? "chevron.up" : "chevron.down").foregroundColor(.sslText3)
                }
            }
            .buttonStyle(.plain)

            if expanded {
                ForEach(storeWithItems.items) { item in
                    Button {
                        onToggle(item.id ?? "", !item.bought)
                    } label: {
                        HStack(spacing: 12) {
                            ZStack {
                                Circle().fill(item.bought ? Color.sslGreen : Color.clear)
                                Circle().stroke(Color.sslBorder, lineWidth: item.bought ? 0 : 2)
                                if item.bought {
                                    Image(systemName: "checkmark").font(.caption).foregroundColor(.white)
                                }
                            }
                            .frame(width: 26, height: 26)

                            Text(item.name)
                                .foregroundColor(item.bought ? .sslText3 : .sslText)
                                .strikethrough(item.bought)
                            Spacer()
                            Button { onRemoveItem(item) } label: {
                                Image(systemName: "xmark").foregroundColor(.sslText3)
                            }
                        }
                    }
                    .buttonStyle(.plain)
                }

                HStack {
                    TextField(L("item_name_placeholder"), text: $newItemName).textFieldStyle(.roundedBorder)
                    Button {
                        if !newItemName.trimmingCharacters(in: .whitespaces).isEmpty {
                            onAddItem(newItemName.trimmingCharacters(in: .whitespaces))
                            newItemName = ""
                        }
                    } label: { Image(systemName: "plus.circle.fill").foregroundColor(.sslGreenDark) }
                }

                Button(role: .destructive) { onRemoveStore() } label: {
                    Text("\(L("action_remove")) \(storeWithItems.store.name)").font(.footnote).foregroundColor(.sslOrangeDark)
                }
            }
        }
        .padding(16)
        .background(Color.sslSurface)
        .overlay(RoundedRectangle(cornerRadius: 18).stroke(Color.sslBorder, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }
}
