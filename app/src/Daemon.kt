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
import com.google.gson.*
import fr.rhaz.ipfs.sweet.R.drawable.ic_battery
import fr.rhaz.ipfs.sweet.R.drawable.notificon
import fr.rhaz.ipfs.sweet.R.string.*
import fr.rhaz.ipfs.sweet.utils.*
import kotlinx.coroutines.*
import org.jetbrains.anko.toast
import java.io.FileReader
import java.lang.Runtime.*

val Context.Daemon get() = Daemon(this)

class Daemon(val ctx: Context): CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext get() = Dispatchers.Main + job

    val store get() = ctx.getExternalFilesDir(null)!!["ipfs"]
    val initialized get() = store.exists()

    val bin get() = ctx.filesDir["ipfsbin"]
    val installed get() = bin.exists()

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

    suspend fun install() = ctx.apply{

        val act = ctx as? Activity ?: throw Exception("Not an activity")

        val type = when(val abi = SUPPORTED_ABIS[0]) {
            "arm64-v8a" -> "arm64"
            "x86_64" -> "amd64"
            "armeabi", "armeabi-v7a" -> "arm"
            "386" -> "386"
            else -> throw Exception("${ctx.getString(daemon_unsupported_arch)}: $abi")
        }

        silentIO {
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
    }

    suspend fun init(args: String = "") = ctx.apply{

        silentIO {
            exec("init $args").waitFor()

            config{
                obj("Sweet").apply {
                    obj("Gateway").apply {
                        string("Public") ?: set("Public", json("https://ipfs.io/"))
                    }
                }
                // Allow webui
                val headers = obj("API").obj("HTTPHeaders")
                val origins = headers.array("Access-Control-Allow-Origin")
                val webui = json("https://webui.ipfs.io")
                if(webui !in origins) origins.add(webui)
            }
        }
    }

    suspend fun start(args: String = "") = ctx.apply {
        this as? Activity ?: throw Exception("Not an activity")

        silentIO {
            config{ obj("Sweet").set("Args", json(args)) }
        }

        startService<DaemonService>()

        silentIO {
            fun check() =
                try{ IPFS(); true}
                catch (ex: Exception){false}
            while(!check()) delay(1000)
        }
    }

    suspend fun pins() = ctx.IO {
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

        if (VERSION.SDK_INT >= VERSION_CODES.O)
            NotificationChannel("sweetipfs", "Sweet IPFS", IMPORTANCE_MIN).apply {
                description = "Sweet IPFS"
                getSystemService(NotificationManager::class.java)
                .createNotificationChannel(this)
            }

        notif.highPower()
    }

    fun start() {
        val args = Daemon.config.obj("Sweet").string("Args")
        daemon = Daemon.exec("daemon $args")
    }

    val notif get() = NotificationCompat.Builder(this, "sweetipfs").apply {
        setOngoing(true)
        color = parseColor("#4b9fa2")
        setSmallIcon(notificon)
        setShowWhen(false)
        setContentTitle(getString(notif_title))
        setContentText(getString(notif_msg))
        val open = pendingActivity<MainActivity>()
        setContentIntent(open)
        val exit = pendingService(intent<DaemonService>().action("STOP"))
        addAction(ic_menu_close_clear_cancel, getString(stop), exit)
    }

    fun NotificationCompat.Builder.lowPower(){
        val highPower = pendingService(intent<DaemonService>().action("HIGH-POWER"))
        addAction(ic_battery, getString(notif_high_power), highPower)
        startForeground(1, build())
        if(::daemon.isInitialized) daemon.destroy()
        Daemon.config {
            val connmgr = obj("Swarm").obj("ConnMgr")
            // Reduce CPU usage
            connmgr.set("GracePeriod", json("1m0s"))
            // Reduce RAM usage
            connmgr.set("LowWater", json(20))
            connmgr.set("HighWater", json(40))
        }
        start()
    }

    fun NotificationCompat.Builder.highPower() {
        val lowPower = pendingService(intent<DaemonService>().action("LOW-POWER"))
        addAction(ic_battery, getString(notif_low_power), lowPower)
        startForeground(1, build())
        if(::daemon.isInitialized) daemon.destroy()
        Daemon.config {
            val connmgr = obj("Swarm").obj("ConnMgr")
            connmgr.set("GracePeriod", json("20s"))
            connmgr.set("LowWater", json(600))
            connmgr.set("HighWater", json(900))
        }
        start()
    }

    override fun onDestroy() = super.onDestroy().also{
        daemon.destroy()
        NotificationManagerCompat.from(this).cancel(1)
    }

    override fun onStartCommand(i: Intent?, f: Int, id: Int) = START_STICKY.also{
        super.onStartCommand(i, f, id)
        when(i?.action){
            "STOP" -> stopSelf()
            "LOW-POWER" -> notif.lowPower()
            "HIGH-POWER" -> notif.highPower()
        }
    }
}