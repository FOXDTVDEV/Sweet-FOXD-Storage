package fr.rhaz.ipfs.sweet

import android.Manifest
import android.Manifest.permission.*
import android.app.ProgressDialog
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.View.VISIBLE
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_main.*
import org.ligi.tracedroid.sending.TraceDroidEmailSender

class MainActivity : AppCompatActivity() {

    fun redirect() = Intent(this, ConsoleActivity::class.java).run{
        flags += FLAG_ACTIVITY_NO_ANIMATION
        startActivity(this)
    }

    fun show() = listOf(text, startbtn).forEach{(it as View).visibility = VISIBLE }.also {
        println("showed")
    }

    var refresh:() -> Unit = {}

    override fun onResume() = super.onResume().also{refresh()}

    override fun onCreate(state: Bundle?) = super.onCreate(state).also {

        setContentView(R.layout.activity_main)

        TraceDroidEmailSender.sendStackTraces("hazae41@gmail.com", this)

        fun error(msg: String) = text.apply{
            text = msg
            visibility = VISIBLE
        }

        chain(
            {
                RxPermissions(this)
                    .request(INTERNET, WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE)
                    .subscribe { granted -> if(granted) it()}
            },
            {ipfsd.check(it, ::error)},
            {
                startbtn.setOnClickListener{chain(ipfsd::init, ::start, {redirect()})}
                refresh = {check(::redirect, ::show)}.also{it()}
            }
        )
    }

    fun start(callback: () -> Unit) {
        startService(Intent(this, DaemonService::class.java))

        val progress = ProgressDialog(this).apply {
            setMessage("Starting...")
            setCancelable(false)
            show()
        }

        Thread{

            while(true.also { Thread.sleep(1000) }) try {
                ipfsd.version.writeText(
                    ipfs.info.version()?.Version ?: continue
                ); break
            } catch(ex: Exception){}

            runOnUiThread {
                progress.dismiss()
                callback()
            }
        }.start()
    }

}