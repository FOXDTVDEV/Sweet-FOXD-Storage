package fr.rhaz.ipfs.sweet

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_PROCESS_TEXT
import android.content.Intent.ACTION_SEND
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.InlineDataPart
import com.github.kittinunf.fuel.core.Method.POST
import com.github.kittinunf.fuel.coroutines.awaitString
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class Share : AppCompatActivity() {
    val connection = MonitorConnection()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share)

        val service = Intent(this, Monitor::class.java)
        bindService(service, connection, Context.BIND_AUTO_CREATE)
        startService(service)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
    }

    suspend fun Monitor.add(text: String): String {
        val data = InlineDataPart(text, "path", "text")
        val request = Fuel.upload(pathOf("add"), POST).add(data)
        val response = JSONObject(request.awaitString())
        return response.getString("Hash")
    }

    suspend fun Monitor.handle() {
        when (intent?.action) {
            ACTION_SEND -> when (intent?.type) {
                "text/plain" -> {
                    val text = intent.getStringExtra(Intent.EXTRA_TEXT)!!
                    val hash = withContext(IO) { add(text) }
                    open("#/ipfs/$hash")
                }
            }
            ACTION_PROCESS_TEXT -> when (intent?.type) {
                "text/plain" -> {
                    val text = intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT)!!
                    open("#/ipfs/$text")
                }
            }
        }
    }

    fun onMonitorConnected(monitor: Monitor) = GlobalScope.launch(Main) {
        try {
            monitor.handle()
        } catch (e: Exception) {
            println(e)
            Toast.makeText(this@Share, e.message, Toast.LENGTH_LONG).show()
        } finally {
            finish()
        }
    }

    inner class MonitorConnection : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {}
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val monitor = (binder as Monitor.MonitorBinder).monitor
            onMonitorConnected(monitor)
        }
    }
}
