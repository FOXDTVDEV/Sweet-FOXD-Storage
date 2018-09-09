package fr.rhaz.ipfs.sweet

import android.R.drawable.ic_menu_close_clear_cancel
import android.app.*
import android.app.NotificationManager.IMPORTANCE_MIN
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import fr.rhaz.ipfs.sweet.R.drawable.notificon

val Context.ipfsd get() = Daemon(this)

class Daemon(val ctx: Context) {

    val store by lazy{ctx.getExternalFilesDir(null)["ipfs"]}
    val bin by lazy{ctx.filesDir["ipfsbin"]}
    val version by lazy{ctx.getExternalFilesDir(null)["version"]}

    fun check(callback: () -> Unit = {}, err: (String) -> Unit = {}){
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

        act.assets.open(type).apply {
            bin.outputStream().also {
                try {copyTo(it)}
                finally {
                    it.close()
                    close()
                }
            }
        }

        bin.setExecutable(true)
        version.writeText(act.assets.open("version").reader().readText());

        progress.dismiss()
        callback()
    }

    fun run(cmd: String) = Runtime.getRuntime().exec(
        "${bin.absolutePath} $cmd",
        arrayOf("IPFS_PATH=${store.absolutePath}")
    )

    fun init(callback: () -> Unit = {}){
        val act = ctx as? Activity ?: return

        val progress = ProgressDialog(ctx).apply {
            setMessage("Initializing...")
            setCancelable(false)
            show()
        }

        Thread{
            val exec = run("init")
            Thread {
                exec.inputStream.bufferedReader().forEachLine { println(it) }
            }.start()
            Thread {
                exec.errorStream.bufferedReader().forEachLine { println(it) }
            }.start()
            exec.waitFor()
            progress.dismiss()
            act.runOnUiThread(callback)
        }.start()

    }

    fun start(callback: () -> Unit) {
        val act = ctx as? Activity ?: return

        act.startService(Intent(act, DaemonService::class.java))

        val progress = ProgressDialog(act).apply {
            setMessage("Starting...")
            setCancelable(false)
            show()
        }

        Thread{

            while(true.also { Thread.sleep(1000) }) try {
                version.writeText(
                    ipfs.version() ?: continue
                ); break
            } catch(ex: Exception){}

            act.runOnUiThread {
                progress.dismiss()
                callback()
            }
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
            NotificationChannel("sweetipfs", "Sweet IPFS", IMPORTANCE_MIN).apply {
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
            addAction(ic_menu_close_clear_cancel, "Stop", exit)
            build()
        }.also { startForeground(1, it) }

        daemon = ipfsd.run("daemon")

        Thread{
            daemon.inputStream.bufferedReader().forEachLine { println(it) }
        }.start()
        Thread{
            daemon.errorStream.bufferedReader().forEachLine { println(it) }
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