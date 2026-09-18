import SwiftUI
import FirebaseFirestore

/// Wrapper de navegação para distinguir o id de uma lista dividida do id de
/// uma lista de compras normal (ambos String) no `navigationDestination`.
struct SplitListRoute: Hashable {
    let id: String
}

@MainActor
final class HomeViewModel: ObservableObject {
    @Published var lists: [ShoppingList] = []
    @Published var splitLists: [SplitList] = []
    private var listener: ListenerRegistration?
    private var splitListener: ListenerRegistration?
    private let service = ListsService()
    private let splitService = SplitListsService()

    func start(uid: String) {
        listener?.remove()
        listener = service.observeLists(uid: uid, status: ShoppingList.statusActive) { [weak self] lists in
            self?.lists = lists
        }
        splitListener?.remove()
        splitListener = splitService.observeLists(uid: uid) { [weak self] lists in
            self?.splitLists = lists
        }
    }

    deinit { listener?.remove(); splitListener?.remove() }
}

struct HomeView: View {
    @EnvironmentObject private var auth: AuthService
    @StateObject private var viewModel = HomeViewModel()
    @State private var showingNewList = false
    @State private var showingNewSplitList = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                HStack {
                    Text(String(format: L("home_greeting"), auth.displayName))
                        .font(.title2).bold()
                        .foregroundColor(.sslText)
                    Spacer()
                    AvatarCircle(label: auth.displayName.isEmpty ? "?" : auth.displayName, size: 44)
                }

                HStack {
                    Text(L("home_title"))
                        .font(.title3.weight(.semibold))
                        .foregroundColor(.sslText)
                    Spacer()
                    if !viewModel.lists.isEmpty {
                        Text(String(format: L("home_active_count"), viewModel.lists.count))
                            .font(.caption.weight(.semibold))
                            .foregroundColor(.sslText2)
                            .padding(.horizontal, 10).padding(.vertical, 4)
                            .background(Capsule().fill(Color.sslSurface2))
                    }
                    Button(L("home_new_list")) { showingNewList = true }
                        .font(.caption.weight(.semibold))
                        .foregroundColor(.sslGreenDark)
                        .accessibilityIdentifier("homeNewListLinkButton")
                }

                if viewModel.lists.isEmpty {
                    Text(L("home_empty"))
                        .foregroundColor(.sslText3)
                } else {
                    ForEach(viewModel.lists) { list in
                        NavigationLink(value: list.id ?? "") {
                            ListCardView(list: list, selfLabel: auth.displayName)
                        }
                        .buttonStyle(.plain)
                    }
                }

                HStack {
                    Text(L("home_split_lists_title"))
                        .font(.title3.weight(.semibold))
                        .foregroundColor(.sslText)
                    Spacer()
                    Button(L("home_new_split_list")) { showingNewSplitList = true }
                        .font(.caption.weight(.semibold))
                        .foregroundColor(.sslGreenDark)
                }
                .padding(.top, 8)

                if viewModel.splitLists.isEmpty {
                    Text(L("home_split_lists_empty")).foregroundColor(.sslText3)
                } else {
                    ForEach(viewModel.splitLists) { list in
                        NavigationLink(value: SplitListRoute(id: list.id ?? "")) {
                            SplitListCardView(list: list, currentUid: auth.currentUser?.uid)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .padding(20)
        }
        .background(Color.sslBg)
        .navigationDestination(for: String.self) { listId in
            ListDetailView(listId: listId)
        }
        .navigationDestination(for: SplitListRoute.self) { route in
            SplitListDetailView(listId: route.id)
        }
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Menu {
                    Button(L("new_list_title")) { showingNewList = true }
                        .accessibilityIdentifier("homeMenuNewListItem")
                    Button(L("split_new_title")) { showingNewSplitList = true }
                        .accessibilityIdentifier("homeMenuNewSplitListItem")
                } label: {
                    Image(systemName: "plus.circle.fill").foregroundColor(.sslGreen)
                }
                .accessibilityIdentifier("homeCreateMenuButton")
            }
        }
        .sheet(isPresented: $showingNewList) {
            NavigationStack { NewListView() }
        }
        .sheet(isPresented: $showingNewSplitList) {
            NavigationStack { NewSplitListView() }
        }
        .onAppear {
            if let uid = auth.currentUser?.uid { viewModel.start(uid: uid) }
        }
    }
}

private struct ListCardView: View {
    let list: ShoppingList
    let selfLabel: String

    private var progress: Double {
        list.itemCount > 0 ? Double(list.boughtCount) / Double(list.itemCount) : 0
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(list.name).font(.headline).foregroundColor(.sslText)

            HStack {
                MemberAvatarStack(selfLabel: selfLabel.isEmpty ? "?" : selfLabel, extraMembers: max(0, list.memberIds.count - 1))
                Spacer()
                Text(String(format: L("home_stores_count"), list.storeCount))
                    .font(.caption.weight(.semibold))
                    .foregroundColor(.sslOrangeDark)
                    .padding(.horizontal, 10).padding(.vertical, 4)
                    .background(Capsule().fill(Color.sslOrangeTint))
            }

            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(Color.sslSurface2).frame(height: 6)
                    Capsule().fill(Color.sslGreen).frame(width: geo.size.width * progress, height: 6)
                }
            }
            .frame(height: 6)

            HStack {
                Text(String(format: L("home_items_progress"), list.boughtCount, list.itemCount))
                    .font(.footnote).foregroundColor(.sslText2)
                Spacer()
                Text(relativeTimeText(list.updatedAt?.dateValue()))
                    .font(.caption2).foregroundColor(.sslText3)
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.sslSurface)
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.sslBorder, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}

private struct SplitListCardView: View {
    let list: SplitList
    let currentUid: String?

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 6) {
                Text(list.name).font(.headline).foregroundColor(.sslText)
                Text(String(format: L("split_card_summary"), String(format: "%.2f €", list.totalValue), list.memberIds.count))
                    .font(.footnote).foregroundColor(.sslText2)
            }
            Spacer()
            if let currentUid, list.paidMemberIds.contains(currentUid) {
                Text(L("split_status_paid"))
                    .font(.caption.weight(.semibold))
                    .foregroundColor(.sslGreenDark)
                    .padding(.horizontal, 10).padding(.vertical, 4)
                    .background(Capsule().fill(Color.sslGreenTint))
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.sslSurface)
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.sslBorder, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}
