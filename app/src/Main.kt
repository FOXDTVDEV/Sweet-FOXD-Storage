package fr.rhaz.ipfs.sweet

import android.Manifest
import android.Manifest.permission.*
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Build.*
import android.os.Bundle
import android.support.v4.app.ActivityCompat.requestPermissions
import android.view.View
import fr.rhaz.ipfs.sweet.R.layout.activity_main
import kotlinx.android.synthetic.main.activity_main.*
import org.ligi.tracedroid.sending.TraceDroidEmailSender.sendStackTraces

class MainActivity: ScopedActivity() {

    fun ready() { checkAPI(::redirect){show()}  }
    fun redirect() = startActivityNoAnimation<ConsoleActivity>()
    fun show() {
        UI { listOf(text, startbtn).forEach(View::visible) }
    }

    override fun onCreate(state: Bundle?){
        super.onCreate(state)
        setContentView(activity_main)
        sendStackTraces("hazae41@gmail.com", this)

        startbtn.onClick{
            UI {
                Daemon.all()
                redirect()
            }
        }

        requestPermissions(this, permissions, 1)
    }

    override fun onRequestPermissionsResult(req: Int, perms: Array<out String>, grants: IntArray) {
        if(grants.all { it == PERMISSION_GRANTED })
            ready()
    }

    override fun onBackPressed() {}
    override fun onResume() {
        super.onResume()
        checkAPI(::redirect){}
    }
}