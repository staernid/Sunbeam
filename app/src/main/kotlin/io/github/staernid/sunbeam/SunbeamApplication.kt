package io.github.staernid.sunbeam

import android.app.Application
import dev.zacsweers.metro.createGraphFactory
import io.github.staernid.sunbeam.di.AppGraph

class SunbeamApplication : Application() {
    lateinit var appGraph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        appGraph = createGraphFactory<AppGraph.Factory>().create(this)
    }
}
