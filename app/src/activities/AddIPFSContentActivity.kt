package fr.rhaz.ipfs.sweet.activities

import android.content.Intent
import android.os.Bundle

class AddIPFSContentActivity : ShareActivity() {

    override fun onCreate(state: Bundle?) = super.onCreate(state).also{

        if (Intent.ACTION_SEND == intent.action)
            if (intent.type != null && "text/plain" == intent.type) {
                handleSendText(intent) // Handle text being sent
            } else {
                //sendStream(intent)
            }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //onRequestPermissionsResult(requestCode, grantResults)
    }

    private fun handleSendText(intent: Intent) {
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (sharedText != null) {
            //addWithUI { ipfs.add.string(sharedText) }
        }
    }

    /*fun sendStream(intent: Intent) {
        Manifest.permission.READ_EXTERNAL_STORAGE
        val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) ?: intent.data

        val inputStreamWithSource = InputStreamProvider.fromURI(this, uri)

        var createTempFile = File.createTempFile("import", null, cacheDir)

        if (inputStreamWithSource != null) {
            val sink = Okio.buffer(Okio.sink(createTempFile))

            val buffer = Okio.source(inputStreamWithSource.inputStream)
            sink.writeAll(buffer)
            sink.close()
        }

        if (inputStreamWithSource == null || !createTempFile.exists()) {
            createTempFile = uri.loadImage(this)
        }

        addWithUI {
            ipfs.add.file(createTempFile)
        }
    }*/
}
