package io.github.staernid.sunbeam

import android.app.Application
import android.widget.Toast
import dev.zacsweers.metro.createGraphFactory
import io.github.staernid.sunbeam.di.AppGraph
import io.github.staernid.sunbeam.profiles.ProfilesManager

class SunbeamApplication : Application() {
    lateinit var appGraph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        appGraph = createGraphFactory<AppGraph.Factory>().create(this)

        if (baseContext != null) {
            val profilesManager = ProfilesManager.getInstance()
            if (!profilesManager.load(this)) {
                Toast.makeText(this, R.string.profile_manager_failed_to_load, Toast.LENGTH_LONG).show()
            }
        }
    }
}
