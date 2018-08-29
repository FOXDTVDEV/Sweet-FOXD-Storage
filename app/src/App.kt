package fr.rhaz.ipfs.sweet

import android.app.Activity
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.support.v7.app.AppCompatActivity

import org.ligi.tracedroid.TraceDroid

class App : Application() {
    override fun onCreate() = super.onCreate().also{TraceDroid.init(this)}
}

object State {
    var running = false

    fun Activity.clipboard(text: String){
        val clipboard = getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager;
        clipboard.primaryClip = ClipData.newPlainText("text", text)
    }
}