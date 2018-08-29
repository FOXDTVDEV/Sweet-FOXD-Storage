package fr.rhaz.ipfs.sweet

import android.app.Application

import org.ligi.tracedroid.TraceDroid

class App : Application() {
    override fun onCreate() = super.onCreate().also{TraceDroid.init(this)}
}
