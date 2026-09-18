import XCTest

/// Cobre os ecrãs de Definições e Listas quando já há sessão iniciada.
///
/// Ao contrário do login, não há forma de simular um login Google/Apple sem
/// interação humana real, por isso estes testes usam a sessão já existente no
/// simulador/dispositivo onde correm. Se não houver sessão iniciada, saltam
/// com uma mensagem clara em vez de falhar de forma confusa — inicia sessão
/// uma vez manualmente neste simulador antes de correr esta suite.
final class SettingsAndHomeUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments += ["-AppleLanguages", "(pt)", "-AppleLocale", "pt_PT"]
        app.launch()

        if app.staticTexts["loginWelcomeText"].waitForExistence(timeout: 5) {
            throw XCTSkip("Sem sessão iniciada neste simulador — inicia sessão manualmente (Google ou Apple) e volta a correr esta suite.")
        }
    }

    /// Regressão do bug reportado no Android (Definições sem scroll tornava
    /// "Terminar sessão" inalcançável). Em iOS o ecrã usa `Form`, que já tem
    /// scroll nativo, mas isto garante que a opção continua a existir e que
    /// chegamos lá a partir do separador de Definições.
    func test_settingsScreen_signOutIsReachable() {
        app.buttons["tabSettings"].tap()

        XCTAssertTrue(app.navigationBars["Definições"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["Idioma"].exists)
        XCTAssertTrue(app.staticTexts["Automático (idioma do telefone)"].exists)

        let signOut = app.buttons["settingsSignOutButton"]
        signOut.scrollToElement()
        XCTAssertTrue(signOut.waitForExistence(timeout: 5))
        XCTAssertTrue(signOut.isHittable, "\"Terminar sessão\" devia estar alcançável fazendo scroll")

        XCTAssertTrue(app.staticTexts["Termos de Serviço"].exists)
        XCTAssertTrue(app.staticTexts["Política de Privacidade"].exists)
        XCTAssertTrue(app.staticTexts["Exportar os meus dados"].exists)
    }

    func test_homeScreen_emptyStateAndCreateMenu_workCorrectly() {
        app.buttons["tabLists"].tap()

        XCTAssertTrue(app.staticTexts["As tuas listas"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["homeNewListLinkButton"].exists)

        app.buttons["homeCreateMenuButton"].tap()
        XCTAssertTrue(app.buttons["homeMenuNewListItem"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.buttons["homeMenuNewSplitListItem"].exists)
        // Fecha o menu sem navegar, para não deixar o ecrã "Nova lista" aberto.
        app.staticTexts["As tuas listas"].tap()
    }
}

private extension XCUIElement {
    /// Faz swipe-up repetido até o elemento existir e ficar visível, ou desiste ao fim de algumas tentativas.
    func scrollToElement(maxSwipes: Int = 6) {
        var attempts = 0
        while !exists || !isHittable {
            guard attempts < maxSwipes else { return }
            XCUIApplication().swipeUp()
            attempts += 1
        }
    }
}
