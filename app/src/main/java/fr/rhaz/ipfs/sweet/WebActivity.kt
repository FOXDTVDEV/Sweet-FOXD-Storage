package fr.rhaz.ipfs.sweet

import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_web.*


class WebActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)
        startService<DaemonService>()
        webview!!.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true;
            settings.loadWithOverviewMode = true;
            settings.useWideViewPort = true;
            settings.builtInZoomControls = true;
            settings.displayZoomControls = false;
            settings.setSupportZoom(true);
            settings.defaultTextEncodingName = "utf-8";
            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
            loadUrl("https://webui.ipfs.io/")
        }
    }
}
