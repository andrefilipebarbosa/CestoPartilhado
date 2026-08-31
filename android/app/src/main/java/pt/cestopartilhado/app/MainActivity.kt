package pt.cestopartilhado.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import pt.cestopartilhado.app.ui.archived.ArchivedScreen
import pt.cestopartilhado.app.ui.archived.ArchivedViewModel
import pt.cestopartilhado.app.ui.components.BottomDestination
import pt.cestopartilhado.app.ui.home.HomeScreen
import pt.cestopartilhado.app.ui.home.HomeViewModel
import pt.cestopartilhado.app.ui.invite.InviteScreen
import pt.cestopartilhado.app.ui.invite.InviteViewModel
import pt.cestopartilhado.app.ui.legal.LegalDocScreen
import pt.cestopartilhado.app.ui.listdetail.ListDetailScreen
import pt.cestopartilhado.app.ui.listdetail.ListDetailViewModel
import pt.cestopartilhado.app.ui.login.LoginScreen
import pt.cestopartilhado.app.ui.login.LoginViewModel
import pt.cestopartilhado.app.ui.nav.CestoViewModelFactory
import pt.cestopartilhado.app.ui.newlist.NewListScreen
import pt.cestopartilhado.app.ui.newlist.NewListViewModel
import pt.cestopartilhado.app.ui.settings.SettingsScreen
import pt.cestopartilhado.app.ui.settings.SettingsViewModel
import pt.cestopartilhado.app.ui.theme.CestoPartilhadoTheme
import pt.cestopartilhado.app.util.LocaleManager

private object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val NEW_LIST = "newList"
    const val LIST_DETAIL = "listDetail/{listId}"
    const val INVITE = "invite/{listId}"
    const val ARCHIVED = "archived"
    const val SETTINGS = "settings"
    const val LEGAL = "legal/{doc}"

    fun listDetail(listId: String) = "listDetail/$listId"
    fun invite(listId: String) = "invite/$listId"
    fun legal(doc: String) = "legal/$doc"
}

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as CestoApplication).container
        setContent {
            CestoPartilhadoTheme {
                CestoApp(container)
            }
        }
    }
}

@Composable
private fun CestoApp(container: AppContainer) {
    val navController = rememberNavController()
    val factory = CestoViewModelFactory(container)
    val startDestination = if (container.authRepository.currentUser != null) Routes.HOME else Routes.LOGIN

    fun goTo(destination: BottomDestination) {
        val route = when (destination) {
            BottomDestination.LISTS -> Routes.HOME
            BottomDestination.ARCHIVE -> Routes.ARCHIVED
            BottomDestination.SETTINGS -> Routes.SETTINGS
        }
        navController.navigate(route) {
            popUpTo(Routes.HOME) { inclusive = false }
            launchSingleTop = true
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            val vm: LoginViewModel = viewModel(factory = factory)
            LoginScreen(vm) {
                navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
            }
        }
        composable(Routes.HOME) {
            val vm: HomeViewModel = viewModel(factory = factory)
            HomeScreen(
                viewModel = vm,
                onOpenList = { listId -> navController.navigate(Routes.listDetail(listId)) },
                onCreateList = { navController.navigate(Routes.NEW_LIST) },
                onNavigate = ::goTo,
            )
        }
        composable(Routes.NEW_LIST) {
            val vm: NewListViewModel = viewModel(factory = factory)
            NewListScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onCreated = { listId ->
                    navController.navigate(Routes.listDetail(listId)) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                },
            )
        }
        composable(Routes.LIST_DETAIL) { backStackEntry ->
            val listId = backStackEntry.arguments?.getString("listId") ?: return@composable
            val vm: ListDetailViewModel = viewModel(factory = factory)
            ListDetailScreen(
                viewModel = vm,
                listId = listId,
                onBack = { navController.popBackStack() },
                onInvite = { navController.navigate(Routes.invite(it)) },
                onDeleted = { navController.popBackStack(Routes.HOME, inclusive = false) },
            )
        }
        composable(Routes.INVITE) { backStackEntry ->
            val listId = backStackEntry.arguments?.getString("listId") ?: return@composable
            val vm: InviteViewModel = viewModel(factory = factory)
            InviteScreen(viewModel = vm, listId = listId, onBack = { navController.popBackStack() })
        }
        composable(Routes.ARCHIVED) {
            val vm: ArchivedViewModel = viewModel(factory = factory)
            ArchivedScreen(viewModel = vm, onNavigate = ::goTo)
        }
        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = viewModel(factory = factory)
            SettingsScreen(
                viewModel = vm,
                onNavigate = ::goTo,
                onOpenTerms = { navController.navigate(Routes.legal("terms-of-service")) },
                onOpenPrivacy = { navController.navigate(Routes.legal("privacy-policy")) },
                onSignedOut = {
                    navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                },
            )
        }
        composable(Routes.LEGAL) { backStackEntry ->
            val doc = backStackEntry.arguments?.getString("doc") ?: return@composable
            LegalDocScreen(assetBaseName = doc, onBack = { navController.popBackStack() })
        }
    }
}
