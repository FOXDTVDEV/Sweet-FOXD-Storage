package fr.rhaz.ipfs.sweet

import android.Manifest.permission.*
import android.app.ProgressDialog
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.View.VISIBLE
import com.github.florent37.runtimepermission.RuntimePermission.askPermission
import com.github.florent37.runtimepermission.kotlin.askPermission
import kotlinx.android.synthetic.main.activity_main.*
import org.ligi.tracedroid.sending.TraceDroidEmailSender

class MainActivity : AppCompatActivity() {

    fun refresh() = check({
        Intent(this, ConsoleActivity::class.java).apply {
            flags += FLAG_ACTIVITY_NO_ANIMATION
            startActivity(this)
        }
    }, {
        listOf(text, startbtn).forEach{ (it as View).visibility = VISIBLE }
    })

    override fun onResume() = super.onResume().also{refresh()}

    override fun onCreate(state: Bundle?) = super.onCreate(state).also {

        setContentView(R.layout.activity_main)

        askPermission{
            ipfsd.check({
                startbtn.setOnClickListener {
                    ipfsd.init{

                        startService(Intent(this, DaemonService::class.java))

                        val progress = ProgressDialog(this).apply {
                            setMessage("Starting...")
                            setCancelable(false)
                            show()
                        }

                        Thread{

                            while(true) try {
                                ipfsd.version.writeText(
                                        ipfs.info.version()?.Version ?: continue
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

            }, {println("ipfsd error"); text.apply{text = it}; refresh()})
        }

    }
}