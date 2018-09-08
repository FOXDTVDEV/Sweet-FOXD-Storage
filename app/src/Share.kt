package fr.rhaz.ipfs.sweet

import android.content.Intent
import android.content.Intent.*
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.support.v7.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.jakewharton.rxbinding2.view.RxView.clicks
import io.ipfs.kotlin.model.NamedHash
import kotlinx.android.synthetic.main.activity_share.*
import net.glxn.qrgen.android.MatrixToImageConfig
import net.glxn.qrgen.android.MatrixToImageWriter
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit


class ShareActivity : AppCompatActivity() {

    override fun onCreate(state: Bundle?) = super.onCreate(state).also {
        setContentView(R.layout.activity_share)
        check({
            intent.apply {
                if(action != ACTION_SEND) return@apply
                if(type == "text/plain") text() // Handle text being sent
                else stream(this)
            }
        }, { hashtxt.text = "Daemon not running" })
    }

    fun Intent.text(){
        val text = getStringExtra(EXTRA_TEXT) ?: return
        title = text
        show {ipfs.add.string(text)}
    }

    fun Uri.inputStream(): InputStream = contentResolver.openInputStream(this)

    val Uri.name: String get() = contentResolver.query(this, null, null, null, null).run {
        val index = getColumnIndex(OpenableColumns.DISPLAY_NAME)
        moveToFirst()
        getString(index).also{close()}
    }

    fun qr(text: String, width: Int, height: Int) = QRCodeWriter()
        .encode(text, BarcodeFormat.QR_CODE, width, height, mapOf(EncodeHintType.MARGIN to 0))
        .let{ MatrixToImageWriter.toBitmap(it, MatrixToImageConfig(0xFF000000.toInt(), 0x00000000))}

    fun stream(intent: Intent) {
        val uri = intent.data
        title = uri.name
        val temp = File.createTempFile(uri.hashCode().toString(), null, cacheDir)
        val input = uri.inputStream(); val output = temp.outputStream()
        try {input.copyTo(output)} finally { input.close(); output.close() }
        show {ipfs.add.file(temp)}
    }

    fun show(action: () -> NamedHash?) = Thread {
        val error = {runOnUiThread{hashtxt.text = "An error occured"}}
        val result = try { action() ?: return@Thread error()} catch (e: Exception) { return@Thread error()}
        runOnUiThread {
            val hash = result.Hash
            val url = "https://ipfs.io/ipfs/$hash"
            qrimg.setImageBitmap(qr(url, 400, 400))
            hashtxt.apply {
                text = hash
                var index = 0
                fun switch() = when(++index%4){
                    1 -> url
                    2 -> "ipfs://$hash"
                    3 -> "/ipfs/$hash"
                    else -> hash
                }
                clicks(this).buffer(500, TimeUnit.MILLISECONDS, 2).subscribe {
                    when(it.size){
                        2 -> startActivity(Intent(ACTION_VIEW, Uri.parse(url)))
                        1 -> runOnUiThread {text = switch()}
                    }
                }
            }
         }
    }.start()
}
