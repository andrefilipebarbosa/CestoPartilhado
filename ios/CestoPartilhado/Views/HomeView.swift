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
                Text(String(format: L("home_greeting"), auth.displayName))
                    .font(.title2).bold()
                    .foregroundColor(.sslText)
                Text(L("home_title"))
                    .font(.headline)
                    .foregroundColor(.sslText2)

                if viewModel.lists.isEmpty {
                    Text(L("home_empty"))
                        .foregroundColor(.sslText3)
                        .padding(.top, 24)
                } else {
                    ForEach(viewModel.lists) { list in
                        NavigationLink(value: list.id ?? "") {
                            ListCardView(list: list)
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

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(list.name).font(.headline).foregroundColor(.sslText)
            Text("\(String(format: L("home_stores_count"), list.storeCount))  ·  \(String(format: L("home_items_progress"), list.boughtCount, list.itemCount))")
                .font(.footnote)
                .foregroundColor(.sslText3)
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.sslSurface)
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.sslBorder, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}
