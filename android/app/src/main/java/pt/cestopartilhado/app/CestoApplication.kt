package pt.cestopartilhado.app

import android.app.Application
import android.content.Context
import pt.cestopartilhado.app.util.LocaleManager

class CestoApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleManager.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
