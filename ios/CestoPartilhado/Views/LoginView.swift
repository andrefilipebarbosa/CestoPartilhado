import AuthenticationServices
import SwiftUI
import UIKit

struct LoginView: View {
    @EnvironmentObject private var auth: AuthService
    @State private var errorMessage: String?
    @State private var isSigningIn = false
    @State private var currentAppleNonce: String?

    var body: some View {
        VStack(spacing: 0) {
            ZStack {
                Color.sslGreenTint
                Image("BrandMark")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 180, height: 180)
            }
            .frame(maxWidth: .infinity)
            .frame(maxHeight: .infinity)

            VStack(alignment: .leading, spacing: 8) {
                HStack(spacing: 10) {
                    Image("BrandMark")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 30, height: 30)
                    Text(L("app_name"))
                        .font(.title3.bold())
                        .foregroundColor(.sslText)
                }
                .padding(.bottom, 14)

                Text(L("login_welcome"))
                    .font(.title2.weight(.semibold))
                    .foregroundColor(.sslText)
                Text(L("login_subtitle"))
                    .font(.subheadline)
                    .foregroundColor(.sslText2)
                    .padding(.bottom, 20)

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
                    HStack(spacing: 12) {
                        Image("GoogleLogo").resizable().frame(width: 20, height: 20)
                        Text(L("action_sign_in_google")).fontWeight(.semibold).foregroundColor(.sslText)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(Color.sslSurface)
                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color.sslBorder, lineWidth: 1))
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                }
                .buttonStyle(.plain)
                .disabled(isSigningIn)

                Button {
                    Task {
                        isSigningIn = true
                        do {
                            try await auth.signInWithFacebook()
                        } catch {
                            errorMessage = error.localizedDescription
                        }
                        isSigningIn = false
                    }
                } label: {
                    HStack(spacing: 12) {
                        FacebookMark()
                        Text(L("action_sign_in_facebook")).fontWeight(.semibold).foregroundColor(.sslText)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(Color.sslSurface)
                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color.sslBorder, lineWidth: 1))
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                }
                .buttonStyle(.plain)
                .disabled(isSigningIn)
                .padding(.top, 12)

                SignInWithAppleButton(.signIn) { request in
                    let nonce = AuthService.randomNonceString()
                    currentAppleNonce = nonce
                    request.requestedScopes = [.fullName, .email]
                    request.nonce = AuthService.sha256(nonce)
                } onCompletion: { result in
                    Task {
                        isSigningIn = true
                        switch result {
                        case .success(let authorization):
                            do {
                                guard let nonce = currentAppleNonce else {
                                    throw NSError(domain: "LoginView", code: 1, userInfo: [NSLocalizedDescriptionKey: "Falha ao iniciar sessão com a Apple"])
                                }
                                try await auth.completeAppleSignIn(authorization: authorization, rawNonce: nonce)
                            } catch {
                                errorMessage = error.localizedDescription
                            }
                        case .failure(let error):
                            errorMessage = error.localizedDescription
                        }
                        isSigningIn = false
                    }
                }
                .signInWithAppleButtonStyle(.black)
                .frame(height: 52)
                .clipShape(RoundedRectangle(cornerRadius: 14))
                .disabled(isSigningIn)
                .padding(.top, 12)

                if let errorMessage {
                    Text(errorMessage).foregroundColor(.sslOrangeDark).font(.footnote)
                }
            }
            .padding(.horizontal, 28)
            .padding(.vertical, 34)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.sslSurface)
            .clipShape(RoundedCorner(radius: 32, corners: [.topLeft, .topRight]))
        }
        .background(Color.sslBg)
        .ignoresSafeArea(edges: .top)
    }
}

private struct FacebookMark: View {
    var body: some View {
        ZStack {
            Circle().fill(Color(red: 0.094, green: 0.467, blue: 0.949))
            Text("f").font(.system(size: 14, weight: .bold)).foregroundColor(.white)
        }
        .frame(width: 20, height: 20)
    }
}

private struct RoundedCorner: Shape {
    var radius: CGFloat
    var corners: UIRectCorner
    func path(in rect: CGRect) -> Path {
        Path(UIBezierPath(roundedRect: rect, byRoundingCorners: corners, cornerRadii: CGSize(width: radius, height: radius)).cgPath)
    }
}
