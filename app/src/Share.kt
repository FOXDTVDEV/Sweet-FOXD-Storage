package fr.rhaz.ipfs.sweet

import android.content.Intent
import android.content.Intent.*
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns.*
import fr.rhaz.ipfs.sweet.R.string.*
import io.ipfs.api.MerkleNode
import io.ipfs.api.NamedStreamable.FileWrapper
import io.ipfs.multihash.Multihash
import kotlinx.android.synthetic.main.activity_share.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.io.File
import java.io.File.*
import java.io.InputStream

class ShareActivity : ScopedActivity() {

    override fun onCreate(state: Bundle?){
        super.onCreate(state)
        if(!intent.hasExtra("hash")) check()
        else Multihash.fromBase58(intent.getStringExtra("hash")).show()
    }

    // Check that ipfs is running
    // if yes: continue
    // if no: ask to start it
    //      if yes: start it then continue
    //      if no: close the activity
    fun check() = checkAPI(::process) {
        catchUI {
            alertDialog(daemon_not_running) {
                setNeutralButton(close) { _, _ -> finish() }
                setPositiveButton(start) { d, _ ->
                    catchUI {
                        Daemon.all()
                        d.dismiss()
                        process()
                    }
                }
            }
        }

    }

    // Try to make a file then ask for wrapping it
    // if it could not: say that it could not with a button to close the activity
    fun process() {
        catchUI {
            throw Exception("Test")
            intent.tempFile?.askWrap()
            ?: alertDialog(share_cannot_open)
                { setNeutralButton(close){ _, _ -> finish()} }
        }
    }

    // Create file from resource type
    val Intent.tempFile get() = when(type){
    "text/plain" -> text?.tempFile
    else -> (getParcelableExtra(EXTRA_STREAM) ?: data)?.apply{title = name}?.tempFile
}

    // Get text resource
    val Intent.text get() = getStringExtra(EXTRA_TEXT)?.also{title = it}

    // Retrieve uri data
    val Uri.inputStream get() = contentResolver.openInputStream(this)

    // Retrieve uri name
    val Uri.name: String get() = contentResolver.query(this, null, null, null, null).run {
    val index = getColumnIndex(DISPLAY_NAME)
    moveToFirst()
    getString(index).also{close()}
}

    fun InputStream.copyTo(file: File) = file.also{
        val out = it.outputStream()
        try { copyTo(out) }
        finally {close(); out.close()}
    }

    // Create temp file from uri
    val Uri.tempFile: File? get() =
    inputStream.copyTo(createTempFile("temp", name, cacheDir))

    // Create temp file from text
    val String.tempFile: File? get() =
    byteInputStream().copyTo(createTempFile("temp", ".txt", cacheDir))

    // Ask if we wrap the file in a dir or not
    // try to show the file
    // then retrieve its hash
    // then show it
    fun File.askWrap() {
        alertDialog(share_wrap) {
            setPositiveButton(yes){ _, _ -> add(true) }
            setNegativeButton(no){ _, _ -> add(false) }
        }
    }

    fun File.add(wrap: Boolean) {
        val wrapper = FileWrapper(this)
        catchUI(::alert) {
            val progress = ctx.progress(share_on_ipfs)
            val hash = async(Dispatchers.IO){
                var i: List<MerkleNode>? = null
                while(i == null)
                    try {i = IPFS().add(wrapper, wrap)}
                    catch(ex: NullPointerException){}
                i.last().hash
            }.await()
            progress.dismiss()
            hash.show()
        }
    }

    fun Multihash.show() = catchUI {
        setContentView(R.layout.activity_share)
        val hash = this@show
        val url = "https://ipfs.io/ipfs/$hash"
        qrimg.setImageBitmap(qr(url, 400, 400))
        layout.onClick { it.requestFocus() }
        hashtxt.apply {
            text = "$hash"
            var index = 0
            val switch = {
                text = when(++index%4){
                    1 -> url
                    2 -> "ipfs://$hash"
                    3 -> "/ipfs/$hash"
                    else -> "$hash"
                }
            }
            onClick { switch() }
        }
        pinbtn.onClick{notimpl()}
        publishbtn.onClick{notimpl()}
        exportbtn.onClick{notimpl()}
    }
}
