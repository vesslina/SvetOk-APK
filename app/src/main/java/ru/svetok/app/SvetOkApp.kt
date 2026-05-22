package ru.svetok.app

import android.app.Application
import java.io.File
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.osmdroid.config.Configuration
import ru.svetok.app.di.appModule
import ru.svetok.app.ui.nav.SvetOkNavGraph

class SvetOkApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val osmdroidBase = File(cacheDir, "osmdroid").apply { mkdirs() }
        val osmdroidTiles = File(osmdroidBase, "tiles").apply { mkdirs() }
        val osmdroidPrefs = getSharedPreferences("osmdroid", MODE_PRIVATE)

        Configuration.getInstance().apply {
            load(this@SvetOkApplication, osmdroidPrefs)
            userAgentValue = "SvetOk/0.1 ($packageName)"
            osmdroidBasePath = osmdroidBase
            osmdroidTileCache = osmdroidTiles
        }

        startKoin {
            androidContext(this@SvetOkApplication)
            modules(appModule)
        }
    }
}

@androidx.compose.runtime.Composable
fun SvetOkApp() {
    SvetOkNavGraph()
}
