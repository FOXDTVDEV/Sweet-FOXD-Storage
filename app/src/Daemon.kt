package fr.rhaz.ipfs.sweet

import android.R.drawable.*
import android.app.*
import android.app.NotificationManager.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Binder
import android.os.Build
import android.os.Environment
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import fr.rhaz.ipfs.sweet.R.drawable.*
import java.io.File
import java.io.FileOutputStream

val Context.ipfsd
    get() = Daemon(this)

class Daemon(val ctx: Context) {

    val store by lazy{storage[".ipfs"]}
    val bin by lazy{ctx.filesDir["ipfsbin"]}
    val version by lazy{ctx.filesDir["version"]}

    fun check(callback: () -> Unit = {}, err: (String) -> Unit = {}){
        if(ctx !is Activity) return;

        if(bin.exists()) callback()
        else install(callback, err)
    }

    fun install(callback: () -> Unit, err: (String) -> Unit = {}) {
        val act = ctx as? Activity ?: return

        val type = when {
            Build.CPU_ABI.toLowerCase().startsWith("x86") -> "x86"
            Build.CPU_ABI.toLowerCase().startsWith("arm") -> "arm"
            else -> return err("Unsupported architecture: ${Build.CPU_ABI}")
        }

        val progress = ProgressDialog(ctx).apply {
            setMessage("Installing...")
            setCancelable(false)
            show()
        }

        Thread {
            act.assets.open(type).copyTo(FileOutputStream(bin))
            bin.setExecutable(true)
            version.writeText(act.assets.open("version").reader().readText());

            progress.dismiss()
            act.runOnUiThread(callback)
        }.start()
    }

    fun run(cmd: String): Process {
        val env = arrayOf("IPFS_PATH=${store.absolutePath}")
        val command = "${bin.absolutePath} $cmd"
        return Runtime.getRuntime().exec(command, env)
    }

    fun init(callback: () -> Unit = {}){
        val act = ctx as? Activity ?: return

        val progress = ProgressDialog(ctx).apply {
            setMessage("Initializing...")
            setCancelable(false)
            show()
        }

        Thread{
            val exec = run("init")
            exec.waitFor()
            progress.dismiss()
            act.runOnUiThread(callback)
        }.start()

    }

}

class DaemonService: Service() {

    override fun onBind(i: Intent) = null

    lateinit var daemon: Process

    override fun onCreate() = super.onCreate().also{

        val exit = Intent(this, DaemonService::class.java).apply {
            action = "STOP"
        }.let { PendingIntent.getService(this, 0, it, 0) }

        val open = Intent(this, MainActivity::class.java)
                .let { PendingIntent.getActivity(this, 0, it, 0) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            NotificationChannel("sweetipfs", "Sweet IPFS", IMPORTANCE_LOW).apply {
                description = "Sweet IPFS"
                getSystemService(NotificationManager::class.java)
                    .createNotificationChannel(this)
            }

        NotificationCompat.Builder(this, "sweetipfs").run {
            setOngoing(true)
            color = Color.parseColor("#4b9fa2")
            setSmallIcon(notificon)
            setShowWhen(false)
            setContentTitle("IPFS Daemon")
            setContentText("Your IPFS node is running in foreground")
            setContentIntent(open)
            addAction(ic_menu_close_clear_cancel, "exit", exit)
            build()
        }.also { startForeground(1, it) }


                Thread{
            try {
                daemon = ipfsd.run("daemon")
                daemon.waitFor()
            } catch (e: InterruptedException) { }
        }.start()
    }

    override fun onDestroy() = super.onDestroy().also{
        daemon.destroy()
        NotificationManagerCompat.from(this).cancel(1)
    }

    override fun onStartCommand(i: Intent?, f: Int, id: Int) = START_STICKY.also{
        super.onStartCommand(i, f, id)
        i?.action?.takeIf{it == "STOP"}?.also{stopSelf()}
    }

}