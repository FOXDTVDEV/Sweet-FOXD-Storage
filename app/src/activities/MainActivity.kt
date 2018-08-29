package fr.rhaz.ipfs.sweet.activities

import android.app.ProgressDialog
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.View.VISIBLE
import fr.rhaz.ipfs.sweet.*
import fr.rhaz.ipfs.sweet.State.running
import io.ipfs.api.IPFS
import kotlinx.android.synthetic.main.activity_main.*
import org.ligi.tracedroid.sending.TraceDroidEmailSender

class MainActivity : AppCompatActivity() {

    val ipfs = IPFS("/ip4/127.0.0.1/tcp/5001")
    val ipfsd = Daemon(this)

    fun refresh() {
        if(running)
            Intent(this, ConsoleActivity::class.java).apply {
                flags += FLAG_ACTIVITY_NO_ANIMATION
                startActivity(this)
            }
        else listOf(text, startbtn)
                .forEach{ (it as View).visibility = VISIBLE }
    }


    override fun onResume() = super.onResume().also{refresh()}

    override fun onCreate(state: Bundle?) = super.onCreate(state).also {

        setContentView(R.layout.activity_main)

        ipfsd.check({

            startbtn.setOnClickListener {
                ipfsd.init{
                    startService(Intent(this@MainActivity, IPFSDaemonService::class.java))

                    val progress = ProgressDialog(this@MainActivity).apply {
                        setMessage("Starting...")
                        show()
                    }

                    Thread{
                        while(true) try {
                            ipfsd.version.writeText(
                                ipfs.version() ?: continue
                            ); break
                        } catch(ex: Exception){}

                        runOnUiThread {
                            progress.dismiss()
                            refresh()
                        }
                    }.start()
                }
            }

            TraceDroidEmailSender.sendStackTraces("hazae41@gmail.com", this@MainActivity)
            refresh()
        }) {text.apply { visibility = VISIBLE; text = it}}
    }
}
