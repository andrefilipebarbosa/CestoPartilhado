package pt.cestopartilhado.app.ui.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pt.cestopartilhado.app.AppContainer
import pt.cestopartilhado.app.R
import pt.cestopartilhado.app.ui.theme.CestoPartilhadoTheme

/**
 * Cobre o ecrã inicial em estado vazio e as duas formas de criar uma lista
 * (link "+ Nova lista" e o menu do FAB com as duas opções).
 */
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setHomeScreen(onCreateList: () -> Unit = {}, onCreateSplitList: () -> Unit = {}) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val container = AppContainer(context)
        composeRule.setContent {
            CestoPartilhadoTheme {
                HomeScreen(
                    viewModel = HomeViewModel(container.authRepository, container.listsRepository, container.splitListsRepository),
                    onOpenList = {},
                    onCreateList = onCreateList,
                    onOpenSplitList = {},
                    onCreateSplitList = onCreateSplitList,
                    onNavigate = {},
                )
            }
        }
    }

    @Test
    fun emptyState_showsBothEmptyMessagesAndNewListLink() {
        setHomeScreen()
        val res = composeRule.activity

        composeRule.onNodeWithText(res.getString(R.string.home_title)).assertIsDisplayed()
        composeRule.onNodeWithText(res.getString(R.string.home_empty)).assertIsDisplayed()
        composeRule.onNodeWithText(res.getString(R.string.home_split_lists_title)).assertIsDisplayed()
        composeRule.onNodeWithText(res.getString(R.string.home_split_lists_empty)).assertIsDisplayed()
        composeRule.onNodeWithText(res.getString(R.string.home_new_list)).assertIsDisplayed()
        composeRule.onNodeWithText(res.getString(R.string.home_new_split_list)).assertIsDisplayed()
    }

    @Test
    fun newListLink_invokesCreateListCallback() {
        var created = false
        setHomeScreen(onCreateList = { created = true })
        val res = composeRule.activity

        composeRule.onNodeWithText(res.getString(R.string.home_new_list)).performClick()
        assertTrue("O link \"+ Nova lista\" deveria ter chamado onCreateList", created)
    }

    @Test
    fun fabMenu_showsBothListTypeOptions() {
        setHomeScreen()
        val res = composeRule.activity

        composeRule.onNodeWithContentDescription(res.getString(R.string.new_list_title)).performClick()

        composeRule.onNodeWithText(res.getString(R.string.new_list_title)).assertIsDisplayed()
        composeRule.onNodeWithText(res.getString(R.string.split_new_title)).assertIsDisplayed()
    }
}
