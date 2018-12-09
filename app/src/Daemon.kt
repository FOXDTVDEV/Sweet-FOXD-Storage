package fr.rhaz.ipfs.sweet

import android.R.drawable.ic_menu_close_clear_cancel
import android.app.*
import android.app.NotificationManager.IMPORTANCE_MIN
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Color.*
import android.os.Build.*
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import fr.rhaz.ipfs.sweet.R.drawable.notificon
import fr.rhaz.ipfs.sweet.R.string.*
import kotlinx.coroutines.*
import java.io.FileReader
import java.io.IOException
import java.lang.Runtime.*
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext

val Context.Daemon get() = Daemon(this)

class Daemon(val ctx: Context): CoroutineScope {
    override val coroutineContext = Job()

    val store get() = ctx.getExternalFilesDir(null)["ipfs"]
    val bin get() = ctx.filesDir["ipfsbin"]
    val config get() = JsonParser().parse(FileReader(store["config"])).asJsonObject

    fun exec(cmd: String) = getRuntime().exec(
        "${bin.absolutePath} $cmd",
        arrayOf("IPFS_PATH=${store.absolutePath}")
    )

    fun config(consumer: JsonObject.() -> Unit){
        consumer(config)
        val data = GsonBuilder().setPrettyPrinting().create().toJson(config).toByteArray()
        store["config"].writeBytes(data)
    }

    suspend fun all(){ install(); init(); start() }

    suspend fun install() {

        val act = ctx as? Activity ?: throw Exception("Not an activity")

        val progress = ctx.progress(daemon_installing)

        val type = when {
            CPU_ABI.toLowerCase().startsWith("x86") -> "x86"
            CPU_ABI.toLowerCase().startsWith("arm") -> "arm"
            else -> throw Exception("${ctx.getString(daemon_unsupported_arch)}: $CPU_ABI")
        }

        async {
            val input = act.assets.open(type)
            val output = bin.outputStream()
            try {
                input.copyTo(output)
            } finally {
                input.close(); output.close()
            }
            bin.setExecutable(true)
        }.await()

        progress.dismiss()
    }

    suspend fun init() {

        val progress = ctx.progress(daemon_init)

        async { exec("init").waitFor() }.await()

        async{
            config{
                getAsJsonObject("Swarm").getAsJsonObject("ConnMgr").apply {
                    remove("LowWater")
                    addProperty("LowWater", 20)
                    remove("HighWater")
                    addProperty("HighWater", 40)
                    remove("GracePeriod")
                    addProperty("GracePeriod", "120s")
                }
            }
        }.await()

        progress.dismiss()
    }

    suspend fun start() {
        ctx as? Activity ?: throw Exception("Not an activity")

        ctx.startService<DaemonService>()
        val progress = ctx.progress(daemon_starting)

        async{
            fun check() =
                try{ IPFS(); true}
                catch (ex: Exception){false}
            while(!check()) delay(1000)
        }.await()

        progress.dismiss()
    }

}

class DaemonService: ScopedService() {
    override fun onBind(i: Intent) = null

    lateinit var daemon: Process

    override fun onCreate() {
        super.onCreate()

        val exit = pendingService(intent<DaemonService>().action("STOP"))
        val open = pendingActivity<MainActivity>()

        if (VERSION.SDK_INT >= VERSION_CODES.O)
            NotificationChannel("sweetipfs", "Sweet IPFS", IMPORTANCE_MIN).apply {
                description = "Sweet IPFS"
                getSystemService(NotificationManager::class.java)
                .createNotificationChannel(this)
            }

        NotificationCompat.Builder(this, "sweetipfs").run {
            setOngoing(true)
            color = parseColor("#4b9fa2")
            setSmallIcon(notificon)
            setShowWhen(false)
            setContentTitle(getString(notif_title))
            setContentText(getString(notif_msg))
            setContentIntent(open)
            addAction(ic_menu_close_clear_cancel, getString(stop), exit)
            startForeground(1, build())
        }

        thread { daemon = Daemon.exec("daemon") }
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