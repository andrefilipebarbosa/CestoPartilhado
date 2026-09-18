package pt.cestopartilhado.app.ui.login

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pt.cestopartilhado.app.AppContainer
import pt.cestopartilhado.app.R
import pt.cestopartilhado.app.ui.theme.CestoPartilhadoTheme

/**
 * Cobre o ecrã de login (Google; "Sign in with Apple" é só iOS). Serve para apanhar
 * botões/texto em falta e como guarda contra o Facebook Login voltar a aparecer
 * (foi removido de propósito — ver commit "Remove Facebook Login from Android and iOS").
 */
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setLoginScreen() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val container = AppContainer(context)
        composeRule.setContent {
            CestoPartilhadoTheme {
                LoginScreen(viewModel = LoginViewModel(container.authRepository), onSignedIn = {})
            }
        }
    }

    @Test
    fun welcomeTextAndGoogleButton_areFullyDisplayed() {
        setLoginScreen()
        val res = composeRule.activity

        composeRule.onNodeWithText(res.getString(R.string.app_name)).assertIsDisplayed()
        composeRule.onNodeWithText(res.getString(R.string.login_welcome)).assertIsDisplayed()
        composeRule.onNodeWithText(res.getString(R.string.login_subtitle)).assertIsDisplayed()

        composeRule.onNodeWithText(res.getString(R.string.action_sign_in_google))
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertHasClickAction()
    }

    /** Regressão: o Facebook Login foi removido de propósito — não pode voltar a aparecer. */
    @Test
    fun facebookButton_isNotPresent() {
        setLoginScreen()
        composeRule.onAllNodesWithText("Facebook", substring = true, ignoreCase = true).assertCountEquals(0)
    }
}
