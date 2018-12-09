package fr.rhaz.ipfs.sweet

import android.os.Bundle
import android.view.View
import fr.rhaz.ipfs.sweet.R.layout.activity_main
import kotlinx.android.synthetic.main.activity_main.*
import org.ligi.tracedroid.sending.TraceDroidEmailSender.sendStackTraces

class MainActivity: ScopedActivity() {

    override fun onResume() {
        super.onResume()
        checkPermissions(::ready, ::finish)
    }

    fun ready() { checkAPI(::redirect, ::show)  }
    fun redirect() = startActivityNoAnimation<ConsoleActivity>()
    fun show(ex: Exception? = null) = listOf(text, startbtn).forEach(View::visible)

    fun error(ex: Exception) {
        val msg = ex.message ?: return
        text.apply{text = msg}.visible()
    }

    override fun onCreate(state: Bundle?){
        super.onCreate(state)
        setContentView(activity_main)
        sendStackTraces("hazae41@gmail.com", ctx)

        startbtn.onClick{
            catchUI(::error){
                Daemon.all()
                redirect()
            }
        }
    }
}