package fr.rhaz.ipfs.sweet

import android.R.drawable.ic_menu_close_clear_cancel
import android.app.*
import android.app.NotificationManager.IMPORTANCE_MIN
import android.content.Context
import android.content.Intent
import android.graphics.Color.*
import android.os.Build.*
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.util.Log
import com.google.gson.*
import fr.rhaz.ipfs.sweet.R.drawable.notificon
import fr.rhaz.ipfs.sweet.R.string.*
import fr.rhaz.ipfs.sweet.utils.array
import fr.rhaz.ipfs.sweet.utils.json
import fr.rhaz.ipfs.sweet.utils.obj
import fr.rhaz.ipfs.sweet.utils.set
import kotlinx.coroutines.*
import java.io.FileReader
import java.lang.Runtime.*
import kotlin.concurrent.thread

val Context.Daemon get() = Daemon(this)

class Daemon(val ctx: Context): CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext get() = Dispatchers.Main + job

    val store get() = ctx.getExternalFilesDir(null)!!["ipfs"]
    val bin get() = ctx.filesDir["ipfsbin"]
    val config get() = JsonParser().parse(FileReader(store["config"])).asJsonObject

    fun exec(cmd: String) = getRuntime().exec(
        "${bin.absolutePath} $cmd",
        arrayOf("IPFS_PATH=${store.absolutePath}")
    )

    fun config(consumer: JsonObject.() -> Unit){
        val config = config.apply(consumer)
        val data = GsonBuilder().setPrettyPrinting().create().toJson(config)
        store["config"].writeBytes(data.toByteArray())
    }

    suspend fun all(){ install(); init(); start() }

    suspend fun install() {

        val act = ctx as? Activity ?: throw Exception("Not an activity")

        val progress = ctx.progress(daemon_installing)

        val type = when(val abi = SUPPORTED_ABIS[0]) {
            "arm64-v8a" -> "arm64"
            "x86_64" -> "amd64"
            "armeabi", "armeabi-v7a" -> "arm"
            "386" -> "386"
            else -> throw Exception("${ctx.getString(daemon_unsupported_arch)}: $abi")
        }

        IO {
            bin.delete()
            bin.createNewFile()
            val input = act.assets.open(type)
            val output = bin.outputStream()
            try {
                input.copyTo(output)
            } finally {
                input.close(); output.close()
            }
            bin.setExecutable(true)
        }

        progress.dismiss()
    }

    suspend fun init() {

        val progress = ctx.progress(daemon_init)

        IO { exec("init").waitFor() }

        IO {
            config{
                // Allow webui
                val headers = obj("API").obj("HTTPHeaders")
                val origins = headers.array("Access-Control-Allow-Origin")
                val webui = json("https://webui.ipfs.io")
                if(webui !in origins) origins.add(webui)

                // Reduce CPU usage
                val connmgr = obj("Swarm").obj("ConnMgr")
                connmgr.set("GracePeriod", json("40s"))
            }
        }

        progress.dismiss()
    }

    suspend fun start() {
        ctx as? Activity ?: throw Exception("Not an activity")

        ctx.startService<DaemonService>()
        val progress = ctx.progress(daemon_starting)

        IO {
            fun check() =
                try{ IPFS(); true}
                catch (ex: Exception){false}
            while(!check()) delay(1000)
        }

        progress.dismiss()
    }

    suspend fun pins() = IO {
        exec("pin ls").run {
            waitFor()
            val lines = inputStream.reader().readLines()
            val pins = lines.map { it.split(" ") }.filter { it[1] == "recursive" }
            pins.map { Multihash(it[0]) }
        }
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