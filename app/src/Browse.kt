package fr.rhaz.ipfs.sweet

import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebViewClient
import fr.rhaz.ipfs.sweet.IPXSResource
import fr.rhaz.ipfs.sweet.R
import kotlinx.android.synthetic.main.activity_browse.*

class BrowseActivity : AppCompatActivity() {

    private var ipxsResource: IPXSResource? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse)
    }

    override fun onResume() {
        super.onResume()

        /*val loadToast = LoadToast(this).show()
        ipxs = IPXSResource(intent.data)*/
        webView.loadUrl(ipxsResource!!.toPublic())
        webView.settings.javaScriptEnabled = false

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)

                //loadToast.success()
            }
        }

        supportActionBar?.subtitle = ipxsResource!!.toString()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.browse, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.publish -> {
                /*val intent = Intent(this@IPFSBrowseActivity, PublishIPFSContentActivity::class.java)
                intent.putExtra("HASH", ipxsResource!!.address)
                startActivity(intent)*/
            }
        }
        return super.onOptionsItemSelected(item)

    }
}

class IPXSResource(uri: Uri) {

    override fun toString() = "$type:$address"
    fun toPublic() = "https://ipfs.io/$type/$address/"

    val err: (String) -> Nothing = {throw IllegalArgumentException(it)}

    val address: String by lazy {
        when (uri.scheme) {
            "fs" -> {
                if(uri.host != null) uri.path.substring(1)
                else uri.pathSegments.run{subList(1, size)}.joinToString("/")
            }
            "ipfs", "ipns" -> uri.schemeSpecificPart.substring(2)
            "http", "https" -> uri.pathSegments.run{subList(1, size)}.joinToString("/")
            else -> err("Could not resolve address for $uri")
        }
    }

    val type: String by lazy {
        when(uri.scheme) {
            "fs" -> (uri.host?.toLowerCase() ?: uri.pathSegments[0].replace("/", "")).also{
                if(it !in listOf("ipfs", "ipns"))
                    err("When scheme is fs:// then it must follow with ipfs or ipns but was $it")
            }
            "ipfs", "ipns" -> uri.scheme
            "http", "https" -> uri.pathSegments[0].also{
                if(uri.host != "ipfs.io")
                    err("when scheme is http(s) then host has to be ipfs.io")
                if(it !in listOf("ipfs", "ipns"))
                    err("cannot handle this ipfs.io url $it")
            }
            else -> err("scheme not supported")
        }
    }
}

