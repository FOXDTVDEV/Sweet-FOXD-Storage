package fr.rhaz.ipfs.sweet

import android.content.Intent.ACTION_SEND
import android.content.Intent.EXTRA_PROCESS_TEXT
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import fr.rhaz.ipfs.sweet.R.id.show
import fr.rhaz.ipfs.sweet.R.layout.activity_browse
import fr.rhaz.ipfs.sweet.R.string.*
import kotlinx.android.synthetic.main.activity_browse.*
import org.jetbrains.anko.alert

class BrowseActivity : ScopedActivity() {

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(activity_browse)
    }

    val uri get() = intent.data ?: Uri.parse(intent.getStringExtra(EXTRA_PROCESS_TEXT))

    override fun onResume() = super.onResume().also{
        val uri = uri ?: return
        title = getString(browser_title)

        UI(::alertFinish) {
            val res = IPXSResource(uri)
            if(!res.valid) throw Exception(browser_not_ipxs)
            supportActionBar?.subtitle = res.toString()

            val process = { browser.process(res) }
            checkAPI(process) { alert{
                title = getString(daemon_not_running)
                closeButton()
                positiveButton(start){ UI {
                    IO { Daemon.all() }
                    process()
                } }
                show()
            } }
        }
    }

    fun WebView.process(res: IPXSResource){ UI {
        loadUrl(res.toPrivate())
        settings.apply {
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            setInitialScale(1)
        }
        webViewClient = object: WebViewClient(){
            override fun shouldOverrideUrlLoading(view: WebView, req: WebResourceRequest)
            = IPXSResource(req.url).run {
                if(valid) loadUrl(toPrivate())
                else loadUrl(req.url.toString())
                false
            }
        }
    } }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.browse, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            show -> IPXSResource(Uri.parse(browser.url)).apply {
                if(!valid) return@apply
                val hash = hash ?: return@apply
                intent<ShareActivity>().apply {
                    putExtra("hash", hash.toBase58())
                    action = ACTION_SEND
                    startActivity(this)
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

}

class IPXSResource(uri: Uri) {

    override fun toString() = "$type://$path"
    fun toPublic() = "https://ipfs.io/$type/$path/"
    fun toPrivate() = "http://127.0.0.1:8080/$type/$path"

    val valid get() = hash != null && type != null && path != null

    val path: String? = try{
        when(uri.scheme){
            "ipfs", "ipns" -> uri.schemeSpecificPart.substring(2)
            "http", "https" -> uri.pathSegments.run{subList(1, size)}.joinToString("/")
            else -> when{
                uri.path.startsWith("/ipfs/") -> uri.path.substring(6)
                uri.path.startsWith("/ipns/") -> uri.path.substring(6)
                uri.path.startsWith("ipfs/") -> uri.path.substring(5)
                uri.path.startsWith("ipns/") -> uri.path.substring(5)
                uri.path.startsWith("Qm") -> uri.path
                else -> null
            }
        }
    } catch (ex: Exception){null}

    val hash =
        try { path?.split("/")?.get(0)?.let(::Multihash) }
        catch (ex: Exception){null}

    val type: String? = try{
        when(uri.scheme){
            "ipfs", "ipns" -> uri.scheme
            "http", "https" -> uri.pathSegments[0].let{
                if(it in listOf("ipfs", "ipns")) it else null
            }
            else -> when{
                uri.path.startsWith("/ipfs/") -> "ipfs"
                uri.path.startsWith("/ipns/") -> "ipns"
                uri.path.startsWith("ipfs/") -> "ipfs"
                uri.path.startsWith("ipns/") -> "ipns"
                uri.path.startsWith("Qm") -> "ipfs"
                else -> null
            }
        }
    } catch (ex: Exception){null}
}

