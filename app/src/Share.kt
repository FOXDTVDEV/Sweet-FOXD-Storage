package fr.rhaz.ipfs.sweet

import android.app.AlertDialog
import android.content.Intent
import android.content.Intent.*
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.support.v7.app.AppCompatActivity
import io.ipfs.api.MerkleNode
import io.ipfs.api.NamedStreamable.FileWrapper
import io.ipfs.multihash.Multihash
import kotlinx.android.synthetic.main.activity_share.*
import java.io.File
import java.io.InputStream

class ShareActivity : ScopedActivity() {

    override fun onCreate(state: Bundle?) = super.onCreate(state).also {
        if(!intent.hasExtra("hash")) check()
        else Multihash.fromBase58(intent.getStringExtra("hash")).show()
        /*else AlertDialog.Builder(this).apply {
            setTitle(getString(R.string.share_action_not_supported))
            setNeutralButton(getString(R.string.close)){ _, _ -> finish()}
        }.show()*/
    }

    // Check that ipfs is running
    // if yes: continue
    // if no: ask to start it
    //      if yes: start it then continue
    //      if no: close the activity
    fun check() = {}/*checkAPI(::process) {
        AlertDialog.Builder(this).apply {
            setTitle(getString(R.string.daemon_not_running))
            setPositiveButton(getString(R.string.start)){ d, _ ->
                //chain(ipfsd::init, ipfsd::start, {d.dismiss(); process()})
            }
            setNeutralButton(getString(R.string.close)){ _, _ -> finish()}
        }.show()
    }*/

    // Try to make a file then ask for wrapping it
    // if it could not: say that it could not with a button to close the activity
    fun process() = intent.tempFile?.askWrap() ?: AlertDialog.Builder(this).apply {
        setTitle(getString(R.string.share_cannot_open))
        setNeutralButton(getString(R.string.close)){ _, _ -> finish()}
    }.show().let{Unit}

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
        val index = getColumnIndex(OpenableColumns.DISPLAY_NAME)
        moveToFirst()
        getString(index).also{close()}
    }

    fun InputStream.copy(file: File) = file.outputStream().let {
        try {copyTo(it); file}
        catch(ex: Exception){null}
        finally {close(); it.close()}
    }

    // Create temp file from uri
    val Uri.tempFile: File? get() =
        inputStream.copy(File.createTempFile("temp", name, cacheDir))

    // Create temp file from text
    val String.tempFile: File? get() =
        byteInputStream().copy(File.createTempFile("temp", ".txt", cacheDir))

    // Ask if we wrap the file in a dir or not
    // try to add the file
    // then retrieve its hash
    // then show it
    fun File.askWrap() = AlertDialog.Builder(ctx).apply {
        setTitle(getString(R.string.share_wrap))
        val wrapper = FileWrapper(this@askWrap)
        setPositiveButton(getString(R.string.yes)){ _, _ ->
            add {
                var i: List<MerkleNode>? = null
                while(i == null) try {i = IPFS().add(wrapper, true)}
                catch(ex: NullPointerException){}
                i.last().hash
            }
        }
        setNegativeButton(getString(R.string.no)){ _, _ ->
            add{
                var i: List<MerkleNode>? = null
                while(i == null) try {i = IPFS().add(wrapper, false)}
                catch(ex: NullPointerException){}
                i!!.last().hash
            }
        }
    }.show().let{Unit}

    fun error(ex: Exception? = null) = AlertDialog.Builder(ctx).apply {
        setTitle(getString(R.string.share_error))
        setPositiveButton(getString(R.string.report)){ d, _ -> finish()}
        setNegativeButton(getString(R.string.close)){ _, _ -> finish()}
    }.show().let{Unit}

    // Show hash after adding it
    fun add(action: () -> Multihash?) = Thread {
        try { action()?.show() ?: error()}
        catch(ex: Exception) {error(ex)}
    }.start()

    fun Multihash.show() = runOnUiThread {
        setContentView(R.layout.activity_share)
        val hash = this@show
        val url = "https://ipfs.io/ipfs/$hash"
        qrimg.setImageBitmap(qr(url, 400, 400))
        layout.apply { setOnClickListener { requestFocus() } }
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
            setOnClickListener { switch() }
        }
        pinbtn.setOnClickListener{notimpl()}
        publishbtn.setOnClickListener { notimpl() }
        exportbtn.setOnClickListener { notimpl() }
    }
}
