import XCTest

/// Cobre o ecrã de login (Google + "Sign in with Apple" nativo). Serve para
/// apanhar texto/botões em falta e como guarda contra o Facebook Login voltar
/// a aparecer (foi removido de propósito — ver commit "Remove Facebook Login
/// from Android and iOS").
final class LoginScreenUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    /// Força a app a arrancar sem sessão e em português, para os testes serem
    /// determinísticos independentemente do estado/idioma do simulador.
    private func launchSignedOut() -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments += ["-uiTestSignedOut", "-AppleLanguages", "(pt)", "-AppleLocale", "pt_PT"]
        app.launch()
        return app
    }

    func test_welcomeTextAndButtons_areDisplayed() {
        let app = launchSignedOut()

        let appName = app.staticTexts["loginAppNameText"]
        XCTAssertTrue(appName.waitForExistence(timeout: 10))
        XCTAssertEqual(appName.label, "Cesto Partilhado")

        let welcome = app.staticTexts["loginWelcomeText"]
        XCTAssertTrue(welcome.exists)
        XCTAssertEqual(welcome.label, "Bem-vindo(a)")

        let subtitle = app.staticTexts["loginSubtitleText"]
        XCTAssertTrue(subtitle.exists)
        XCTAssertTrue(subtitle.label.contains("Cria listas de compras"), "Texto inesperado: \(subtitle.label)")

        let googleButton = app.buttons["loginContinueWithGoogleButton"]
        XCTAssertTrue(googleButton.exists)
        XCTAssertTrue(googleButton.isHittable, "O botão do Google devia estar visível e clicável, não cortado pela barra de navegação")

        let appleButton = app.buttons["loginSignInWithAppleButton"]
        XCTAssertTrue(appleButton.exists)
        XCTAssertTrue(appleButton.isHittable, "O botão \"Sign in with Apple\" devia estar visível e clicável")
    }

    /// Regressão: o Facebook Login foi removido de propósito — não pode voltar a aparecer.
    func test_facebookLogin_isNotPresent() {
        let app = launchSignedOut()
        XCTAssertTrue(app.staticTexts["loginAppNameText"].waitForExistence(timeout: 10))

        let facebookMatches = app.descendants(matching: .any)
            .matching(NSPredicate(format: "label CONTAINS[c] 'Facebook'"))
        XCTAssertEqual(facebookMatches.count, 0, "Não deve existir nenhum botão/texto de Facebook no ecrã de login")
    }
}
