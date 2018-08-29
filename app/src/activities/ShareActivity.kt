package fr.rhaz.ipfs.sweet.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import fr.rhaz.ipfs.sweet.R
import io.ipfs.api.IPFS
import kotlinx.android.synthetic.main.activity_add.*
import net.glxn.qrgen.android.QRCode
import org.ligi.tracedroid.logging.Log
import java.net.ConnectException

abstract class ShareActivity : AppCompatActivity() {

    val ipfs = IPFS("/ip4/127.0.0.1/tcp/5001")

    override fun onCreate(state: Bundle?) = super.onCreate(state).also {
        setContentView(R.layout.activity_add)
        hashInfoText.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.add, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?) =
        super.onOptionsItemSelected(item).also{
            when (item!!.itemId) {
                R.id.copy -> {}
            }
        }

    /*fun addWithUI(callback: () -> NamedHash?) {

        val show = LoadToast(this).show()

        Thread(Runnable {
            addResult = try {
                callback()
            } catch (e: ConnectException) {
                null
            }
            runOnUiThread {
                val displayString: String
                if (addResult == null) {
                    show.error()
                    displayString = "could not execute ( daemon running? )" + ipfs.lastError?.Message
                } else {
                    show.success()
                    displayString = getSuccessDisplayHTML()

                    qr_src.setImageBitmap(QRCode.from(getSuccessURL()).bitmap())
                }

                Log.i(displayString)

                hashInfoText.text = Html.fromHtml(displayString)

            }
        }).start()

    }*/
}
