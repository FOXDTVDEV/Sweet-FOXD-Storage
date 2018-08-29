package fr.rhaz.ipfs.sweet

import android.app.Application
import fr.rhaz.ipfs.sweet.di.AppComponent
import fr.rhaz.ipfs.sweet.di.AppModule
import fr.rhaz.ipfs.sweet.di.DaggerAppComponent

import org.ligi.tracedroid.TraceDroid

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        component = DaggerAppComponent.builder().appModule(AppModule()).build()
        TraceDroid.init(this)
    }

    companion object {
        private var component: AppComponent? = null
        fun component() = component
    }
}
