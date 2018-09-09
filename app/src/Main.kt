package fr.rhaz.ipfs.sweet

import android.Manifest.permission.*
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION
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

    fun show() = listOf(text, startbtn).forEach{(it as View).visibility = VISIBLE }

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
            permission@{
                RxPermissions(this)
                    .request(INTERNET, WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE)
                    .subscribe { granted -> if(granted) it()}
            },
            check@{ipfsd.check(it, ::error)},
            refresh@{
                startbtn.setOnClickListener{chain(ipfsd::init, ipfsd::start, {redirect()})}
                refresh = {check(::redirect, ::show)}.also{it()}
            }
        )
    }

}