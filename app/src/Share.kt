package fr.rhaz.ipfs.sweet

import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.EXTRA_TEXT
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.support.v7.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import io.ipfs.kotlin.model.NamedHash
import kotlinx.android.synthetic.main.activity_share.*
import net.glxn.qrgen.android.MatrixToImageConfig
import net.glxn.qrgen.android.MatrixToImageWriter
import java.io.File
import java.io.InputStream


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
        show({ipfs.add.string(text)}, {
            val hash = it.Hash.also{hashtxt.text = it}
            val url = "https://ipfs.io/ipfs/$hash"
            qrimg.setImageBitmap(qr(url, 400, 400))
        })
    }

    fun Uri.inputStream(): InputStream = contentResolver.openInputStream(this)

    val Uri.name: String get() = contentResolver.query(this, null, null, null, null).run {
        val index = getColumnIndex(OpenableColumns.DISPLAY_NAME)
        moveToFirst()
        getString(index).also{close()}
    }

    fun qr(text: String, width: Int, height: Int) = QRCodeWriter()
        .encode(text, BarcodeFormat.QR_CODE, width, height, mapOf(EncodeHintType.MARGIN to 0))
        .let{MatrixToImageWriter.toBitmap(it, MatrixToImageConfig(0xFF000000.toInt(), 0x00000000))}

    fun stream(intent: Intent) {

        val uri = intent.getParcelableExtra(Intent.EXTRA_STREAM) ?: intent.data
        title = uri.name
        val temp = File.createTempFile(uri.hashCode().toString(), null, cacheDir)
        val input = uri.inputStream(); val output = temp.outputStream()
        try {input.copyTo(output)} finally { input.close(); output.close() }
        show({ipfs.add.file(temp)}, {
            val hash = it.Hash.also{hashtxt.text = it}
            val url = "https://ipfs.io/ipfs/$hash"
            qrimg.setImageBitmap(qr(url, 400, 400))
        })

    }

    fun show(action: () -> NamedHash?, callback: (NamedHash) -> Unit) = Thread {
        val result = try { action() ?: return@Thread } catch (e: Exception) { return@Thread}
        runOnUiThread { callback(result) }
    }.start()
}
