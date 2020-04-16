package fr.rhaz.ipfs.sweet

import android.content.*
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity

class Main : AppCompatActivity() {

    val receiver = NodeReceiver()
    val connection = NodeConnection()
    var node: Node.NodeBinder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filter = IntentFilter("fr.rhaz.ipfs.sweet.NODE")
        registerReceiver(receiver, filter)

        val service = Intent(this, Node::class.java)
        startForegroundService(service)
        bindService(service, connection, Context.BIND_AUTO_CREATE)

        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        unbindService(connection)
    }

    inner class NodeConnection : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {}
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            node = service as Node.NodeBinder
        }
    }

    inner class NodeReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {}
    }
}
