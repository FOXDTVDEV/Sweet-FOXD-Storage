package fr.rhaz.ipfs.sweet

import android.content.Intent
import android.content.Intent.*
import android.net.Uri
import android.os.Bundle
import android.os.Environment.getExternalStorageDirectory
import fr.rhaz.ipfs.sweet.R.layout.activity_share
import fr.rhaz.ipfs.sweet.R.string.*
import fr.rhaz.ipfs.sweet.utils.obj
import fr.rhaz.ipfs.sweet.utils.string
import io.ipfs.api.MerkleNode
import io.ipfs.api.NamedStreamable
import io.ipfs.api.NamedStreamable.ByteArrayWrapper
import io.ipfs.api.NamedStreamable.DirWrapper
import io.ipfs.multihash.Multihash
import kotlinx.android.synthetic.main.activity_share.*
import org.jetbrains.anko.*

class ShareActivity : ScopedActivity() {

    override fun onCreate(state: Bundle?){
        super.onCreate(state)
        if(intent.hasExtra("hash")){
            val hash = intent.getStringExtra("hash")
            val scheme = intent.getStringExtra("scheme") ?: "ipfs"
            val name = intent.getStringExtra("name")
            if(name != null) title = name
            Multihash(hash).show(scheme)
        }
        else UI { checkAPI() }
    }

    fun process() { intent.parse() }

    fun checkAPI() = checkAPI(::process) {
        fun start() = UI {
            IO { Daemon.all()}
            process()
        }
        UI { alert(daemon_not_running) {
            neutralPressed(close){ finish() }
            positiveButton(start) { start() }
            show()
        } }
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
        alert {
            title = getString(share_wrap)
            isCancelable = false
            yes { then(true) }
            no { then(false) }
            show()
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
        title = name(uri)
        val wrapper = wrapperOf(uri)
        askWrap{ wrap -> add(wrapper, wrap) }
    }

    fun Intent.handleMultiple() = UI {
        val list = getParcelableArrayListExtra<Uri>(EXTRA_STREAM)
        val wrappers = list.map(::wrapperOf)
        val wrapper = DirWrapper("files", wrappers)
        askWrap{ wrap -> add(wrapper, wrap) }
    }

    fun wrapperOf(uri: Uri): ByteArrayWrapper {
        val bytes = inputStream(uri).readBytes()
        return ByteArrayWrapper(name(uri), bytes)
    }

    fun add(wrapper: NamedStreamable, wrap: kotlin.Boolean) = UI {
        val progress = progress(share_on_ipfs)
        val hash = IO {
            var i: List<MerkleNode>? = null
            while(i == null)
                try {i = IPFS().add(wrapper, wrap)}
                catch(ex: NullPointerException){}
            i.last().hash
        }
        progress.dismiss()
        hash.show("ipfs")
    }

    fun Multihash.show(scheme: String) = UI {
        setContentView(activity_share)
        layout.onClick { it.requestFocus() }

        val hash = this@show
        val gateway = silentIO{Daemon.config.obj("Sweet").obj("Gateway").string("Public")}
        val url = gateway + "$scheme/$hash"
        qrimg.setImageBitmap(qr(url, 400, 400))

        hashtxt.apply {
            text = "/$scheme/$hash"
            var index = 0
            val switch = {
                text = when(++index % 5){
                    1 -> url
                    2 -> "$scheme://$hash"
                    3 -> "http://localhost:8080/$scheme/$hash"
                    4 -> "https://webui.ipfs.io/#/explore/$hash"
                    else -> "/$scheme/$hash"
                }
            }
            onClick { switch() }
        }

        if(hash !in Daemon.pins())
            pinbtn.text = getString(share_btn_pin)

        pinbtn.onClick{ pin(hash) }
        publishbtn.onClick{ publish(hash) }
        exportbtn.onClick{ export(hash, scheme) }
    }

    fun publish(hash: Multihash) = UI {
        val keys = IO { IPFS().key.list() }
        alert{
            title = publishbtn.text
            customView {
                scrollView { verticalLayout {
                    padding = dip(24)
                    keys.forEach { key ->
                        textView(key.name){
                            textSize = 18f
                            onClick {
                                alert{
                                    message = getString(publish_confirm, key.name)
                                    title = publishbtn.text
                                    no{}
                                    yes {
                                        UI {
                                            IO(publishing) { Daemon.exec("name publish --key=${key.name} $hash").waitFor() }
                                            alert("Published!"){closeButton()}.show()
                                        }
                                    }
                                    show()
                                }
                            }
                        }
                    }
                } }
            }
            neutralPressed(close){}
            show()
        }
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

    fun export(hash: Multihash, scheme: String) = UI {
        val sdcard = getExternalStorageDirectory()
        val folder = sdcard["IPFS"].apply { mkdir() }
        alert{
            title = getString(share_btn_export)
            val input = inputView{ hint = getString(filename) }
            okButton {
                UI{
                    val file = folder["${input.value}.$scheme"]
                    IO{
                        file.createNewFile()
                        file.writeText(hash.toBase58())
                    }
                    val path = file.relativeTo(sdcard).path
                    alert{
                        title = getString(share_btn_export)
                        message = getString(exported, "sdcard/$path")
                        okButton{}
                        negativeButton(copy){ clipboard(file.path)}
                    }.show()
                }
            }
            cancelButton {}
            show()
        }
    }
}
