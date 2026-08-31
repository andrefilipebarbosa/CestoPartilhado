import SwiftUI
import UIKit

@MainActor
final class InviteViewModel: ObservableObject {
    @Published var list: ShoppingList?
    @Published var inviteResult: String?
    @Published var isInviting = false

    private let service = ListsService()
    private var observation: ListObservation?
    private var currentUid: String?

    var isOwner: Bool { list?.ownerId == currentUid }

    func start(listId: String, uid: String) {
        currentUid = uid
        observation?.stop()
        observation = service.observeListWithStores(listId: listId) { [weak self] result in
            self?.list = result.list
        }
    }

    func inviteLink() -> String {
        guard let listId = list?.id else { return "" }
        return service.inviteLink(for: listId)
    }

    func invite(email: String) {
        guard let listId = list?.id, !email.isEmpty else { return }
        isInviting = true
        Task {
            inviteResult = (try? await service.inviteMemberByEmail(listId: listId, email: email)) ?? "error"
            isInviting = false
        }
    }

    func stop() { observation?.stop() }
}

struct InviteView: View {
    let listId: String
    @EnvironmentObject private var auth: AuthService
    @StateObject private var viewModel = InviteViewModel()
    @Environment(\.dismiss) private var dismiss
    @State private var email = ""

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text(L("invite_subtitle")).font(.subheadline).foregroundColor(.sslText3)

                Button {
                    let activity = UIActivityViewController(activityItems: [viewModel.inviteLink()], applicationActivities: nil)
                    UIApplication.shared.topMostViewController()?.present(activity, animated: true)
                } label: {
                    Text(L("invite_share_link")).fontWeight(.semibold).frame(maxWidth: .infinity).frame(height: 48)
                }
                .buttonStyle(.borderedProminent).tint(.sslGreen)

                if viewModel.isOwner {
                    HStack {
                        TextField(L("invite_email_placeholder"), text: $email)
                            .textFieldStyle(.roundedBorder)
                            .keyboardType(.emailAddress)
                            .textInputAutocapitalization(.never)
                        Button {
                            viewModel.invite(email: email)
                            email = ""
                        } label: {
                            Text(L("invite_button"))
                        }
                        .buttonStyle(.borderedProminent).tint(.sslText)
                        .disabled(viewModel.isInviting || email.isEmpty)
                    }

                    if let result = viewModel.inviteResult {
                        Text(result == "error" ? L("error_generic") : L("invite_sent"))
                            .foregroundColor(result == "error" ? .sslOrangeDark : .sslGreenDark)
                    }
                } else {
                    Text(L("invite_error_owner_only")).font(.footnote).foregroundColor(.sslText3)
                }

                if let list = viewModel.list {
                    Text(String(format: L("invite_people_with_access"), list.memberIds.count))
                        .font(.headline).foregroundColor(.sslText2).padding(.top, 12)

                    ForEach(list.memberIds, id: \.self) { uid in
                        Text(uid == list.ownerId ? L("invite_role_owner") : L("invite_role_editor"))
                            .foregroundColor(.sslText)
                    }
                    ForEach(list.pendingInvites, id: \.self) { pendingEmail in
                        HStack {
                            Text(pendingEmail).foregroundColor(.sslText)
                            Spacer()
                            Text(L("invite_role_pending"))
                                .font(.caption).foregroundColor(.sslOrangeDark)
                                .padding(.horizontal, 10).padding(.vertical, 4)
                                .background(Capsule().fill(Color.sslOrangeTint))
                        }
                    }
                }
            }
            .padding(20)
        }
        .background(Color.sslBg)
        .navigationTitle(L("invite_title"))
        .toolbar {
            ToolbarItem(placement: .cancellationAction) { Button(L("action_cancel")) { dismiss() } }
        }
        .onAppear {
            if let uid = auth.currentUser?.uid { viewModel.start(listId: listId, uid: uid) }
        }
        .onDisappear { viewModel.stop() }
    }
}

private extension UIApplication {
    func topMostViewController() -> UIViewController? {
        guard let scene = connectedScenes.first as? UIWindowScene,
              var top = scene.windows.first(where: { $0.isKeyWindow })?.rootViewController else { return nil }
        while let presented = top.presentedViewController { top = presented }
        return top
    }
}
