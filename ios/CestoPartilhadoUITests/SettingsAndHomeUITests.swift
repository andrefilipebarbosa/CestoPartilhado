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
        app.tabBars.buttons["Definições"].tap()

        XCTAssertTrue(app.navigationBars["Definições"].waitForExistence(timeout: 5))
        // Verificar cada linha COM `scrollToElement` (nunca `.exists` cru) e
        // pela ordem em que aparecem no ecrã: um `Form` (UITableView por baixo)
        // recicla células fora do ecrã, tanto por estarem ainda por montar como
        // por já termos passado por elas ao fazer scroll para a seguinte.
        // "Exportar os meus dados" é um `Button` simples — a SwiftUI funde o
        // texto no próprio botão — por isso sai por `app.buttons`, não
        // `app.staticTexts` (ao contrário dos `NavigationLink` abaixo, que
        // continuam a expor o texto como elemento próprio).
        for text in ["Idioma", "Automático (idioma do telefone)"] {
            let element = app.staticTexts[text]
            element.scrollToElement()
            XCTAssertTrue(element.exists, "\"\(text)\" não apareceu no ecrã de Definições")
        }

        let exportButton = app.buttons["Exportar os meus dados"]
        exportButton.scrollToElement()
        XCTAssertTrue(exportButton.exists, "\"Exportar os meus dados\" não apareceu no ecrã de Definições")

        for text in ["Termos de Serviço", "Política de Privacidade"] {
            let element = app.staticTexts[text]
            element.scrollToElement()
            XCTAssertTrue(element.exists, "\"\(text)\" não apareceu no ecrã de Definições")
        }

        let signOut = app.buttons["settingsSignOutButton"]
        signOut.scrollToElement()
        XCTAssertTrue(signOut.exists)
        XCTAssertTrue(signOut.isHittable, "\"Terminar sessão\" devia estar alcançável fazendo scroll")
    }

    /// Regressão: os .md dos Termos de Serviço / Política de Privacidade estavam
    /// a ser achatados para a raiz do bundle pelo xcodegen (o "Legal" era um
    /// group, não uma referência de pasta), por isso `Bundle.main.url(...,
    /// subdirectory: "Legal")` nunca encontrava o ficheiro e o ecrã ficava em
    /// branco. Corrigido em project.yml (Legal agora é `type: folder`).
    func test_legalDocuments_actuallyLoadContent() {
        app.tabBars.buttons["Definições"].tap()

        let terms = app.staticTexts["Termos de Serviço"]
        XCTAssertTrue(terms.waitForExistence(timeout: 5))
        terms.tap()
        XCTAssertTrue(
            app.staticTexts["Termos de Serviço — Cesto Partilhado"].waitForExistence(timeout: 5),
            "Ecrã de Termos de Serviço apareceu em branco — falha a carregar o .md do bundle"
        )
        app.navigationBars.buttons.element(boundBy: 0).tap()

        let privacy = app.staticTexts["Política de Privacidade"]
        XCTAssertTrue(privacy.waitForExistence(timeout: 5))
        privacy.tap()
        XCTAssertTrue(
            app.staticTexts["Política de Privacidade — Cesto Partilhado"].waitForExistence(timeout: 5),
            "Ecrã de Política de Privacidade apareceu em branco — falha a carregar o .md do bundle"
        )
    }

    func test_homeScreen_newListLinkAndCreateMenu_workCorrectly() {
        app.tabBars.buttons["Listas"].tap()

        XCTAssertTrue(app.staticTexts["As tuas listas"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["homeNewListLinkButton"].exists)

        app.buttons["homeCreateMenuButton"].tap()
        XCTAssertTrue(app.buttons["homeMenuNewListItem"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.buttons["homeMenuNewSplitListItem"].exists)
        // Fecha o menu sem navegar (tocar num elemento tapado pelo popover falha
        // com "not hittable") — retocar no separador atual dispensa-o de forma segura.
        app.tabBars.buttons["Listas"].tap()
    }
}

private extension XCUIElement {
    /// Faz swipe-up repetido até o elemento existir e ficar visível, ou desiste ao fim de algumas tentativas.
    /// Usa `waitForExistence` (não `.exists`) em cada iteração — uma célula de um
    /// `Form`/`UITableView` pode levar um instante a montar mesmo sem ser preciso
    /// scroll nenhum, especialmente logo a seguir a mudar de separador.
    func scrollToElement(maxSwipes: Int = 10) {
        var attempts = 0
        while !waitForExistence(timeout: 2) || !isHittable {
            guard attempts < maxSwipes else { return }
            XCUIApplication().swipeUp()
            attempts += 1
        }
    }
}
