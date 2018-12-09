package fr.rhaz.ipfs.sweet

import android.app.AlertDialog
import android.content.Intent
import android.content.Intent.EXTRA_PROCESS_TEXT
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Build.VERSION_CODES.M
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebSettings.ZoomDensity.*
import android.webkit.WebView
import android.webkit.WebViewClient
import io.ipfs.multihash.Multihash
import kotlinx.android.synthetic.main.activity_browse.*

class BrowseActivity : ScopedActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse)
    }

    val uri by lazy {
        intent.data ?:
            if(SDK_INT < M) null
            else Uri.parse(intent.getStringExtra(EXTRA_PROCESS_TEXT))
    }

    override fun onResume() = super.onResume().also{
        val uri = uri ?: return
        title = getString(R.string.browser_title)
        IPXSResource(uri).apply {
            if(!valid)
                return AlertDialog.Builder(ctx).apply {
                    setTitle(getString(R.string.browser_not_ipxs))
                    setPositiveButton(getString(R.string.close)){ _, _ -> finish()}
                }.show().let{Unit}

            supportActionBar?.subtitle = toString()

            fun process() = browser.apply {
                loadUrl(toPrivate())
                settings.apply {
                    javaScriptEnabled = true
                    javaScriptCanOpenWindowsAutomatically = true
                    useWideViewPort = true;
                    loadWithOverviewMode = true;
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    setInitialScale(1);
                }
                webViewClient = object: WebViewClient(){
                    override fun shouldOverrideUrlLoading(view: WebView, req: WebResourceRequest) = false.also{
                        if (SDK_INT < LOLLIPOP) return true;
                        IPXSResource(req.url).apply {
                            if(valid) loadUrl(toPrivate())
                            else loadUrl(req.url.toString())
                        }
                    }
                }
            }.let{Unit}

            /*checkAPI(::process) {
                AlertDialog.Builder(ctx).apply {
                    setTitle(getString(R.string.daemon_not_running))
                    setPositiveButton(getString(R.string.start)){ d, _ ->
                        //ipfsd::init, ipfsd::start, {d.dismiss(); process()})
                    }
                    setNeutralButton(getString(R.string.close)){ _, _ -> finish()}
                }.show()
            }*/
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.browse, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.show -> IPXSResource(Uri.parse(browser.url)).apply {
                if(!valid) return@apply
                val hash = hash ?: return@apply
                Intent(ctx, ShareActivity::class.java).apply {
                    putExtra("hash", hash)
                    action = Intent.ACTION_SEND
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

    val valid get() = type != null && path != null

    val path: String? = when(uri.scheme){
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

    val hash = path?.split("/")?.get(0)

    val type: String? = when(uri.scheme){
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
}

