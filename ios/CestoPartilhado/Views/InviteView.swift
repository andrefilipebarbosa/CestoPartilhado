import SwiftUI
import UIKit

@MainActor
final class InviteViewModel: ObservableObject {
    @Published var list: ShoppingList?
    @Published var inviteResult: String?
    @Published var isInviting = false
    @Published var memberLabels: [String: String] = [:]

    private let service = ListsService()
    private var observation: ListObservation?
    private var currentUid: String?

    var isOwner: Bool { list?.ownerId == currentUid }
    var uid: String? { currentUid }

    func start(listId: String, uid: String) {
        currentUid = uid
        observation?.stop()
        observation = service.observeListWithStores(listId: listId) { [weak self] result in
            self?.list = result.list
            self?.loadMissingLabels(result.list.memberIds)
        }
    }

    /// Busca o nome a mostrar (publicProfiles) para cada uid novo e guarda em cache.
    private func loadMissingLabels(_ memberIds: [String]) {
        let missing = memberIds.filter { $0 != currentUid && memberLabels[$0] == nil }
        guard !missing.isEmpty else { return }
        Task {
            for uid in missing {
                memberLabels[uid] = await AuthService.getDisplayLabel(uid)
            }
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

                VStack(spacing: 14) {
                    HStack(spacing: 12) {
                        Circle().fill(Color.sslSurface).frame(width: 36, height: 36)
                            .overlay(Image(systemName: "link").font(.system(size: 15)).foregroundColor(.sslText2))
                        Text(viewModel.inviteLink())
                            .font(.footnote.weight(.medium))
                            .foregroundColor(.sslText2)
                            .lineLimit(1)
                            .truncationMode(.middle)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    Button {
                        let activity = UIActivityViewController(activityItems: [viewModel.inviteLink()], applicationActivities: nil)
                        UIApplication.shared.topMostViewController()?.present(activity, animated: true)
                    } label: {
                        Text(L("invite_share_link")).fontWeight(.semibold).frame(maxWidth: .infinity).frame(height: 46)
                    }
                    .buttonStyle(.borderedProminent).tint(.sslGreen)
                }
                .padding(14)
                .background(Color.sslSurface2)
                .clipShape(RoundedRectangle(cornerRadius: 16))

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

                    ForEach(list.memberIds, id: \.self) { memberUid in
                        let isSelf = memberUid == viewModel.uid
                        let label = isSelf ? (auth.displayName.isEmpty ? "?" : auth.displayName) : (viewModel.memberLabels[memberUid] ?? String(memberUid.prefix(8)))
                        HStack(spacing: 12) {
                            AvatarCircle(
                                label: label,
                                size: 40,
                                background: memberUid == list.ownerId ? .sslGreen : .sslOrange
                            )
                            Text(isSelf ? "\(label) (tu)" : label)
                                .foregroundColor(.sslText)
                                .frame(maxWidth: .infinity, alignment: .leading)
                            Text(memberUid == list.ownerId ? L("invite_role_owner") : L("invite_role_editor"))
                                .font(.caption.weight(.semibold))
                                .foregroundColor(memberUid == list.ownerId ? .sslGreenDark : .sslText2)
                                .padding(.horizontal, 10).padding(.vertical, 4)
                                .background(Capsule().fill(memberUid == list.ownerId ? Color.sslGreenTint : Color.sslSurface2))
                        }
                        .padding(.vertical, 4)
                    }
                    ForEach(list.pendingInvites, id: \.self) { pendingEmail in
                        HStack(spacing: 12) {
                            Circle().fill(Color.sslSurface2).frame(width: 40, height: 40)
                                .overlay(Circle().stroke(Color.sslText3, lineWidth: 1))
                                .overlay(Image(systemName: "envelope").font(.system(size: 14)).foregroundColor(.sslText3))
                            Text(pendingEmail).foregroundColor(.sslText).frame(maxWidth: .infinity, alignment: .leading)
                            Text(L("invite_role_pending"))
                                .font(.caption.weight(.semibold)).foregroundColor(.sslOrangeDark)
                                .padding(.horizontal, 10).padding(.vertical, 4)
                                .background(Capsule().fill(Color.sslOrangeTint))
                        }
                        .padding(.vertical, 4)
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
