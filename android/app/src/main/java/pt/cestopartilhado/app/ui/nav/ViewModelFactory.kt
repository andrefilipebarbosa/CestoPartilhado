package pt.cestopartilhado.app.ui.nav

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pt.cestopartilhado.app.AppContainer
import pt.cestopartilhado.app.ui.archived.ArchivedViewModel
import pt.cestopartilhado.app.ui.home.HomeViewModel
import pt.cestopartilhado.app.ui.invite.InviteViewModel
import pt.cestopartilhado.app.ui.listdetail.ListDetailViewModel
import pt.cestopartilhado.app.ui.login.LoginViewModel
import pt.cestopartilhado.app.ui.newlist.NewListViewModel
import pt.cestopartilhado.app.ui.settings.SettingsViewModel

/** Fábrica única e explícita de ViewModels — sem Hilt, para manter as dependências visíveis. */
class CestoViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        LoginViewModel::class.java -> LoginViewModel(container.authRepository)
        HomeViewModel::class.java -> HomeViewModel(container.authRepository, container.listsRepository)
        NewListViewModel::class.java -> NewListViewModel(
            container.authRepository, container.listsRepository, container.storeCatalogRepository
        )
        ListDetailViewModel::class.java -> ListDetailViewModel(container.authRepository, container.listsRepository)
        InviteViewModel::class.java -> InviteViewModel(container.authRepository, container.listsRepository)
        ArchivedViewModel::class.java -> ArchivedViewModel(container.authRepository, container.listsRepository)
        SettingsViewModel::class.java -> SettingsViewModel(container.authRepository, container.exportRepository)
        else -> throw IllegalArgumentException("ViewModel desconhecido: $modelClass")
    } as T
}
