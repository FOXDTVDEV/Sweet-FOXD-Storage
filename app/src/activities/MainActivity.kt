package fr.rhaz.ipfs.sweet.activities

import android.app.ProgressDialog
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import fr.rhaz.ipfs.sweet.*
import fr.rhaz.ipfs.sweet.State.running
import io.ipfs.kotlin.IPFS
import kotlinx.android.synthetic.main.activity_main.*
import org.ligi.kaxt.setVisibility
import org.ligi.tracedroid.sending.TraceDroidEmailSender
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var ipfs: IPFS

    val ipfsd = IPFSDaemon(this)

    fun refresh() {
        if(running)
            Intent(this, DetailsActivity::class.java).apply {
                flags += FLAG_ACTIVITY_NO_ANIMATION
                startActivity(this)
            }
        else listOf(text, startbtn).forEach{(it as View).setVisibility(true)}
    }


    override fun onResume() = super.onResume().also{refresh()}

    override fun onCreate(state: Bundle?) = super.onCreate(state).also {

        App.component()?.inject(this)
        setContentView(R.layout.activity_main)

        ipfsd.apply {

            check({

                startbtn.setOnClickListener {
                    init{
                        startService(Intent(this@MainActivity, IPFSDaemonService::class.java))

                        val progress = ProgressDialog(this@MainActivity).apply {
                            setMessage("Starting...")
                            show()
                        }

                        Thread{
                            while(true) try {
                                version.writeText(
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
            }, {text.apply {setVisibility(true); text = it}})

        }
    }
}
