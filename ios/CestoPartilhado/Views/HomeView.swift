import SwiftUI
import FirebaseFirestore

@MainActor
final class HomeViewModel: ObservableObject {
    @Published var lists: [ShoppingList] = []
    private var listener: ListenerRegistration?
    private let service = ListsService()

    func start(uid: String) {
        listener?.remove()
        listener = service.observeLists(uid: uid, status: ShoppingList.statusActive) { [weak self] lists in
            self?.lists = lists
        }
    }

    deinit { listener?.remove() }
}

struct HomeView: View {
    @EnvironmentObject private var auth: AuthService
    @StateObject private var viewModel = HomeViewModel()
    @State private var showingNewList = false

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
                }

                if viewModel.lists.isEmpty {
                    Text(L("home_empty"))
                        .foregroundColor(.sslText3)
                        .padding(.top, 24)
                } else {
                    ForEach(viewModel.lists) { list in
                        NavigationLink(value: list.id ?? "") {
                            ListCardView(list: list, selfLabel: auth.displayName)
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
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button { showingNewList = true } label: {
                    Image(systemName: "plus.circle.fill").foregroundColor(.sslGreen)
                }
            }
        }
        .sheet(isPresented: $showingNewList) {
            NavigationStack { NewListView() }
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
