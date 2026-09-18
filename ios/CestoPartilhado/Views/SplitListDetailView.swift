import SwiftUI

@MainActor
final class SplitListDetailViewModel: ObservableObject {
    @Published var state: SplitListWithItems?
    @Published var inviteResult: String?
    @Published var memberLabels: [String: String] = [:]

    private let service = SplitListsService()
    private var observation: ListObservation?
    private(set) var currentUid: String?

    var isOwner: Bool { state?.list.ownerId == currentUid }

    func start(listId: String, uid: String) {
        currentUid = uid
        observation?.stop()
        observation = service.observeListWithItems(listId: listId) { [weak self] result in
            self?.state = result
            self?.loadMissingLabels(result.list.memberIds)
        }
        Task { await AuthService.setActiveListRef("splitLists/\(listId)") }
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

    func addItem(name: String, value: Double) {
        guard let listId = state?.list.id, let uid = currentUid else { return }
        Task { try? await service.addItem(listId: listId, name: name, value: value, uid: uid) }
    }

    func removeItem(_ item: SplitItem) {
        guard let listId = state?.list.id, let uid = currentUid else { return }
        Task { try? await service.removeItem(listId: listId, item: item, uid: uid) }
    }

    func setMemberPaid(_ memberUid: String, paid: Bool) {
        guard let listId = state?.list.id else { return }
        Task { try? await service.setMemberPaid(listId: listId, memberUid: memberUid, paid: paid) }
    }

    func invite(email: String) {
        guard let listId = state?.list.id, !email.isEmpty else { return }
        Task {
            inviteResult = (try? await service.inviteMemberByEmail(listId: listId, email: email)) ?? "error"
        }
    }

    func deleteList(onDeleted: @escaping () -> Void) {
        guard let listId = state?.list.id, isOwner else { return }
        Task {
            try? await service.deleteList(listId: listId)
            onDeleted()
        }
    }

    func stop() {
        observation?.stop()
        Task { await AuthService.setActiveListRef(nil) }
    }
}

private func formatValue(_ value: Double) -> String { String(format: "%.2f €", value) }

struct SplitListDetailView: View {
    let listId: String
    @EnvironmentObject private var auth: AuthService
    @StateObject private var viewModel = SplitListDetailViewModel()
    @Environment(\.dismiss) private var dismiss

    @State private var newItemName = ""
    @State private var newItemValue = ""
    @State private var inviteEmail = ""

    var body: some View {
        Group {
            if let state = viewModel.state {
                content(state)
            } else {
                ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity).background(Color.sslBg)
            }
        }
        .onAppear { if let uid = auth.currentUser?.uid { viewModel.start(listId: listId, uid: uid) } }
        .onDisappear { viewModel.stop() }
    }

    @ViewBuilder
    private func content(_ state: SplitListWithItems) -> some View {
        let list = state.list
        let locked = list.isLocked

        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(String(format: L("split_detail_total"), formatValue(list.totalValue)))
                        .font(.headline).foregroundColor(.sslText)
                    Text(String(format: L("split_detail_your_share"), formatValue(list.shareValue)))
                        .font(.subheadline).foregroundColor(.sslGreenDark)
                }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.sslGreenTint)
                .clipShape(RoundedRectangle(cornerRadius: 18))

                if locked {
                    Text(L("split_detail_locked_notice"))
                        .font(.footnote).foregroundColor(.sslOrangeDark)
                        .padding(14)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color.sslOrangeTint)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                }

                ForEach(state.items) { item in
                    HStack {
                        Text(item.name).foregroundColor(.sslText)
                        Spacer()
                        Text(formatValue(item.value)).foregroundColor(.sslText2)
                        if !locked {
                            Button { viewModel.removeItem(item) } label: {
                                Image(systemName: "xmark").foregroundColor(.sslText3)
                            }
                        }
                    }
                    .padding(14)
                    .background(Color.sslSurface)
                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color.sslBorder, lineWidth: 1))
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                }

                if !locked {
                    HStack {
                        TextField(L("split_item_name_placeholder"), text: $newItemName).textFieldStyle(.roundedBorder)
                        TextField(L("split_item_value_placeholder"), text: $newItemValue)
                            .textFieldStyle(.roundedBorder)
                            .keyboardType(.decimalPad)
                            .frame(width: 90)
                        Button {
                            let value = Double(newItemValue.replacingOccurrences(of: ",", with: "."))
                            if !newItemName.trimmingCharacters(in: .whitespaces).isEmpty, let value {
                                viewModel.addItem(name: newItemName.trimmingCharacters(in: .whitespaces), value: value)
                                newItemName = ""; newItemValue = ""
                            }
                        } label: { Image(systemName: "checkmark.circle.fill").foregroundColor(.sslGreenDark) }
                    }
                }

                Text(String(format: L("split_people_title"), list.memberIds.count))
                    .font(.headline).foregroundColor(.sslText2).padding(.top, 8)

                ForEach(list.memberIds, id: \.self) { memberUid in
                    let paid = list.paidMemberIds.contains(memberUid)
                    let isSelf = memberUid == viewModel.currentUid
                    let label = isSelf ? (auth.displayName.isEmpty ? "?" : auth.displayName) : (viewModel.memberLabels[memberUid] ?? String(memberUid.prefix(8)))
                    HStack(spacing: 12) {
                        AvatarCircle(label: label,
                                     background: memberUid == list.ownerId ? .sslGreen : .sslOrange)
                        Text(isSelf ? "\(label) (tu)" : label)
                            .foregroundColor(.sslText)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        Text(formatValue(list.shareValue)).foregroundColor(.sslText2)
                        Button {
                            if viewModel.isOwner { viewModel.setMemberPaid(memberUid, paid: !paid) }
                        } label: {
                            Text(paid ? L("split_status_paid") : L("split_status_owing"))
                                .font(.caption.weight(.semibold))
                                .foregroundColor(paid ? .sslGreenDark : .sslOrangeDark)
                                .padding(.horizontal, 10).padding(.vertical, 4)
                                .background(Capsule().fill(paid ? Color.sslGreenTint : Color.sslOrangeTint))
                        }
                        .disabled(!viewModel.isOwner)
                    }
                }

                ForEach(list.pendingInvites, id: \.self) { email in
                    HStack {
                        Text(email).foregroundColor(.sslText3).frame(maxWidth: .infinity, alignment: .leading)
                        Text(L("invite_role_pending")).font(.caption).foregroundColor(.sslOrangeDark)
                            .padding(.horizontal, 10).padding(.vertical, 4)
                            .background(Capsule().fill(Color.sslOrangeTint))
                    }
                }

                if viewModel.isOwner && !locked {
                    HStack {
                        TextField(L("split_invite_email_placeholder"), text: $inviteEmail)
                            .textFieldStyle(.roundedBorder)
                            .keyboardType(.emailAddress)
                            .textInputAutocapitalization(.never)
                        Button {
                            if !inviteEmail.isEmpty { viewModel.invite(email: inviteEmail); inviteEmail = "" }
                        } label: { Image(systemName: "checkmark.circle.fill").foregroundColor(.sslGreenDark) }
                    }
                    if let result = viewModel.inviteResult {
                        Text(result == "error" ? L("error_generic") : L("invite_sent"))
                            .font(.footnote)
                            .foregroundColor(result == "error" ? .sslOrangeDark : .sslGreenDark)
                    }
                } else if !viewModel.isOwner {
                    Text(L("split_owner_only_payments")).font(.footnote).foregroundColor(.sslText3)
                }
            }
            .padding(20)
        }
        .background(Color.sslBg)
        .navigationTitle(list.name)
        .toolbar {
            if viewModel.isOwner {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Menu {
                        Button(L("split_delete_list"), role: .destructive) { viewModel.deleteList { dismiss() } }
                    } label: {
                        Image(systemName: "ellipsis.circle").foregroundColor(.sslText)
                    }
                }
            }
        }
    }
}
