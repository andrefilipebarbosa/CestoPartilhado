import SwiftUI

struct LoginView: View {
    @EnvironmentObject private var auth: AuthService
    @State private var errorMessage: String?
    @State private var isSigningIn = false

    var body: some View {
        VStack(spacing: 12) {
            Text(L("app_name"))
                .font(.title).bold()
                .foregroundColor(.sslText)
            Text(L("login_welcome"))
                .font(.title2)
                .foregroundColor(.sslText)
            Text(L("login_subtitle"))
                .font(.subheadline)
                .foregroundColor(.sslText2)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)

            Spacer().frame(height: 16)

            Button {
                Task {
                    isSigningIn = true
                    do {
                        try await auth.signIn()
                    } catch {
                        errorMessage = error.localizedDescription
                    }
                    isSigningIn = false
                }
            } label: {
                Text(L("action_sign_in_google"))
                    .fontWeight(.semibold)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
            }
            .buttonStyle(.bordered)
            .disabled(isSigningIn)
            .padding(.horizontal, 32)

            if let errorMessage {
                Text(errorMessage).foregroundColor(.sslOrangeDark).font(.footnote)
            }
        }
        .padding(32)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.sslBg)
    }
}
