package pt.cestopartilhado.app.ui.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pt.cestopartilhado.app.AppContainer
import pt.cestopartilhado.app.R
import pt.cestopartilhado.app.ui.components.BottomDestination
import pt.cestopartilhado.app.ui.theme.CestoPartilhadoTheme

/**
 * Regressão do bug reportado: o ecrã de Definições não tinha scroll, o que
 * tornava "Terminar sessão" e os documentos legais inalcançáveis em ecrãs
 * mais pequenos. Estes testes falham se alguém remover o `verticalScroll`
 * do Column em SettingsScreen.kt.
 */
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setSettingsScreen() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val container = AppContainer(context)
        composeRule.setContent {
            CestoPartilhadoTheme {
                SettingsScreen(
                    viewModel = SettingsViewModel(container.authRepository, container.exportRepository),
                    onNavigate = { _: BottomDestination -> },
                    onOpenTerms = {},
                    onOpenPrivacy = {},
                    onSignedOut = {},
                )
            }
        }
    }

    @Test
    fun topContent_isDisplayedWithoutScrolling() {
        setSettingsScreen()
        val res = composeRule.activity

        // "settings_title" e "nav_settings" partilham o mesmo texto ("Definições"/"Settings"),
        // por isso há sempre 2 nós (título do ecrã + item da barra de navegação) — pegamos no primeiro.
        composeRule.onAllNodesWithText(res.getString(R.string.settings_title)).onFirst().assertIsDisplayed()
        composeRule.onNodeWithText(res.getString(R.string.settings_language)).assertIsDisplayed()
        composeRule.onNodeWithText(res.getString(R.string.settings_language_system)).assertIsDisplayed()
    }

    @Test
    fun signOutButton_isReachableByScrollingAndClickable() {
        setSettingsScreen()
        val res = composeRule.activity

        val signOut = composeRule.onNodeWithText(res.getString(R.string.settings_sign_out))
        signOut.performScrollTo()
        signOut.assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun legalDocuments_areReachableByScrolling() {
        setSettingsScreen()
        val res = composeRule.activity

        val terms = composeRule.onNodeWithText(res.getString(R.string.terms_of_service))
        terms.performScrollTo()
        terms.assertIsDisplayed().assertHasClickAction()

        val privacy = composeRule.onNodeWithText(res.getString(R.string.privacy_policy))
        privacy.performScrollTo()
        privacy.assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun exportDataButton_isReachableByScrolling() {
        setSettingsScreen()
        val res = composeRule.activity

        val export = composeRule.onNodeWithText(res.getString(R.string.settings_export_button))
        export.performScrollTo()
        export.assertIsDisplayed()
    }
}
