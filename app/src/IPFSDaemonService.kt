package fr.rhaz.ipfs.sweet

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.support.v4.app.NotificationCompat
import fr.rhaz.ipfs.sweet.State.running

class IPFSDaemonService: Service() {

    override fun onBind(i: Intent) =
        object: Binder() {
            val service = this@IPFSDaemonService
        }

    val notifs: NotificationManager
        get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    lateinit var daemon: Process

    override fun onCreate() = super.onCreate().also{
        val exit = Intent(this, IPFSDaemonService::class.java).apply {
            action = "STOP"
        }.let { PendingIntent.getService(this, 0, it, 0) }

        val open = Intent(this, ConsoleActivity::class.java)
            .let { PendingIntent.getActivity(this, 0, it, 0) }

        val builder = NotificationCompat.Builder(this).setOngoing(true)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle("IPFS Daemon")
                .setContentText("The daemon is running")
                .setContentIntent(open)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "exit", exit)

        notifs.notify(1, builder.build())

        Thread{
            try {
                daemon = ipfsd.run("daemon")
                running = true
                daemon.waitFor()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }.start()
    }

    override fun onDestroy() = super.onDestroy().also{
        daemon.destroy()
        running = false
        notifs.cancel(1)
    }

    override fun onStartCommand(i: Intent?, f: Int, id: Int) = super.onStartCommand(i, f, id)
        .also{ i?.action?.takeIf{it == "STOP"}?.also{stopSelf()} }

}
