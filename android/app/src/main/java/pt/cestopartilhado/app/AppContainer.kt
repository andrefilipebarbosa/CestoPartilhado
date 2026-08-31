package pt.cestopartilhado.app

import android.content.Context
import pt.cestopartilhado.app.data.AuthRepository
import pt.cestopartilhado.app.data.ExportRepository
import pt.cestopartilhado.app.data.ListsRepository
import pt.cestopartilhado.app.data.StoreCatalogRepository

/**
 * Localizador de serviços simples (sem Hilt/Dagger) — mantém as coisas explícitas
 * e fáceis de seguir. Cada dependência é criada uma vez e partilhada.
 */
class AppContainer(context: Context) {
    val authRepository = AuthRepository(context.applicationContext)
    val listsRepository = ListsRepository()
    val storeCatalogRepository = StoreCatalogRepository()
    val exportRepository = ExportRepository(context.applicationContext)
}
