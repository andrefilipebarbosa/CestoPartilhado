import SwiftUI
import FirebaseFirestore

@MainActor
final class ArchivedViewModel: ObservableObject {
    @Published var lists: [ShoppingList] = []
    private var listener: ListenerRegistration?
    private let service = ListsService()
    private(set) var currentUid: String?

    func start(uid: String) {
        currentUid = uid
        listener?.remove()
        listener = service.observeLists(uid: uid, status: ShoppingList.statusClosed) { [weak self] lists in
            self?.lists = lists
        }
    }

    func daysUntilDeletion(_ list: ShoppingList) -> Int {
        guard let closedAt = list.closedAt?.dateValue() else { return 30 }
        let elapsedDays = Calendar.current.dateComponents([.day], from: closedAt, to: Date()).day ?? 0
        return max(0, 30 - elapsedDays)
    }

    func recover(_ listId: String) {
        Task { try? await service.setListStatus(listId: listId, status: ShoppingList.statusActive) }
    }

    func deleteNow(_ listId: String) {
        Task { try? await service.deleteList(listId: listId) }
    }

    deinit { listener?.remove() }
}

struct ArchivedView: View {
    @EnvironmentObject private var auth: AuthService
    @StateObject private var viewModel = ArchivedViewModel()

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    Text(L("archived_subtitle")).font(.subheadline).foregroundColor(.sslText2)
                    Text(L("archived_notice"))
                        .font(.footnote).foregroundColor(.sslText2)
                        .padding(14)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color.sslSurface2)
                        .clipShape(RoundedRectangle(cornerRadius: 14))

                    if viewModel.lists.isEmpty {
                        Text(L("archived_empty")).foregroundColor(.sslText3).padding(.top, 24)
                    } else {
                        ForEach(viewModel.lists) { list in
                            ArchivedCardView(
                                list: list,
                                daysLeft: viewModel.daysUntilDeletion(list),
                                isOwner: list.ownerId == viewModel.currentUid,
                                onRecover: { viewModel.recover(list.id ?? "") },
                                onDeleteNow: { viewModel.deleteNow(list.id ?? "") }
                            )
                        }
                    }
                }
                .padding(20)
            }
            .background(Color.sslBg)
            .navigationTitle(L("archived_title"))
            .onAppear { if let uid = auth.currentUser?.uid { viewModel.start(uid: uid) } }
        }
    }
}

private struct ArchivedCardView: View {
    let list: ShoppingList
    let daysLeft: Int
    let isOwner: Bool
    let onRecover: () -> Void
    let onDeleteNow: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(list.name).font(.headline).foregroundColor(.sslText)
                Spacer()
                Text(String(format: L("archived_deletes_in"), daysLeft))
                    .font(.footnote)
                    .foregroundColor(daysLeft <= 5 ? .sslOrangeDark : .sslText2)
            }
            HStack {
                Button(L("action_recover")) { onRecover() }
                    .buttonStyle(.bordered).tint(.sslGreen)
                if isOwner {
                    Spacer()
                    Button(L("archived_delete_now")) { onDeleteNow() }
                        .foregroundColor(.sslText3)
                }
            }
        }
        .padding(18)
        .background(Color.sslSurface)
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.sslBorder, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}
