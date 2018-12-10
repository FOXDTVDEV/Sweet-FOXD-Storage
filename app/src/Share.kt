package fr.rhaz.ipfs.sweet

import android.app.Activity
import android.app.Activity.*
import android.app.DialogFragment
import android.app.DialogFragment.*
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns.*
import com.rustamg.filedialogs.FileDialog
import com.rustamg.filedialogs.FileDialog.*
import com.rustamg.filedialogs.SaveFileDialog
import fr.rhaz.ipfs.sweet.R.layout.activity_share
import fr.rhaz.ipfs.sweet.R.string.*
import fr.rhaz.ipfs.sweet.R.style.*
import io.ipfs.api.MerkleNode
import io.ipfs.api.NamedStreamable
import io.ipfs.api.NamedStreamable.*
import io.ipfs.multihash.Multihash
import kotlinx.android.synthetic.main.activity_share.*
import java.io.File

class ShareActivity : ScopedActivity(), FileDialog.OnFileSelectedListener {

    override fun onCreate(state: Bundle?){
        super.onCreate(state)

        if(intent.hasExtra("hash")){
            val hash = intent.getStringExtra("hash")
            Multihash(hash).show()
        }

        else UI { checkAPI() }
    }

    fun process() { intent.parse() }

    fun checkAPI() = checkAPI(::process) {
        fun start() = UI {
            Daemon.all()
            process()
        }
        alertDialog(daemon_not_running) {
            setNeutralButton(close) { _, _ -> finish() }
            setPositiveButton(start) { _, _ -> start() }
        }
    }

    fun Intent.parse() = when(action) {
        ACTION_SEND -> when(type) {
            "text/plain" -> handleText()
            else -> handleStream()
        }
        ACTION_SEND_MULTIPLE -> handleMultiple()
        else -> throw Exception(share_action_not_supported)
    }

    fun askWrap(then: (Boolean) -> Unit){
        alertDialog(share_wrap) {
            setCancelable(false)
            setPositiveButton(yes){ _, _ -> then(true) }
            setNegativeButton(no){ _, _ -> then(false) }
        }
    }

    fun Intent.handleText() = UI {
        val text = getStringExtra(EXTRA_TEXT)
        val name = text.take(30).also { title = it }
        val bytes = text.toByteArray()
        val wrapper = ByteArrayWrapper(name, bytes)
        askWrap{ wrap -> add(wrapper, wrap) }
    }

    fun Intent.handleStream() = UI {
        val uri = getParcelableExtra(EXTRA_STREAM) ?: data
        title = uri.name
        val wrapper = wrapperOf(uri)
        askWrap{ wrap -> add(wrapper, wrap) }
    }

    fun Intent.handleMultiple() = UI {
        val list = getParcelableArrayListExtra<Uri>(EXTRA_STREAM)
        val wrappers = list.map(::wrapperOf)
        val wrapper = DirWrapper("files", wrappers)
        askWrap{ wrap -> add(wrapper, wrap) }
    }

    val Uri.inputStream get() = contentResolver.openInputStream(this)

    val Uri.name: String get() =
        if(scheme == "file") lastPathSegment
        else contentResolver.query(this, null, null, null, null).run {
            val index = getColumnIndex(DISPLAY_NAME); moveToFirst()
            getString(index).also{close()}
        }

    fun wrapperOf(uri: Uri): ByteArrayWrapper {
        val bytes = uri.inputStream.readBytes()
        return ByteArrayWrapper(uri.name, bytes)
    }

    fun add(wrapper: NamedStreamable, wrap: kotlin.Boolean) = UI {
        val progress = ctx.progress(share_on_ipfs)
        val hash = IO {
            var i: List<MerkleNode>? = null
            while(i == null)
                try {i = IPFS().add(wrapper, wrap)}
                catch(ex: NullPointerException){}
            i.last().hash
        }
        progress.dismiss()
        hash.show()
    }

    fun Multihash.show() = UI {
        setContentView(activity_share)
        layout.onClick { it.requestFocus() }

        val hash = this@show
        val url = "https://ipfs.io/ipfs/$hash"
        qrimg.setImageBitmap(qr(url, 400, 400))

        hashtxt.apply {
            text = "$hash"
            var index = 0
            val switch = {
                text = when(++index % 5){
                    1 -> url
                    2 -> "ipfs://$hash"
                    3 -> "/ipfs/$hash"
                    4 -> "https://webui.ipfs.io/#/explore/$hash"
                    else -> "$hash"
                }
            }
            onClick { switch() }
        }

        if(hash !in Daemon.pins())
            pinbtn.text = getString(share_btn_pin)

        pinbtn.onClick{ pin(hash) }
        publishbtn.onClick{notimpl()}
        exportbtn.onClick{ export(hash) }
    }

    fun publish(hash: Multihash) = UI {

    }

    fun pin(hash: Multihash) = UI {
        if(hash in Daemon.pins()){
            IO { Daemon.exec("pin rm $hash").waitFor() }
            pinbtn.text = getString(share_btn_pin)
        }
        else {
            IO { Daemon.exec("pin add $hash").waitFor() }
            pinbtn.text = getString(share_btn_unpin)
        }
    }

    fun export(hash: Multihash) = UI {
        SaveFileDialog().apply {
            arguments = Bundle().apply {
                putString("hash", hash.toString())
                putString("extension", ".ipfs")
            }
            setStyle(DialogFragment.STYLE_NO_TITLE, AppTheme)
            show(supportFragmentManager, "SaveFileDialog")
        }
    }

    override fun onFileSelected(dialog: FileDialog, file: File) {
        val hash = dialog.arguments?.getString("hash") ?: return
        file.apply {
            createNewFile()
            writeText(hash)
        }
    }
}
