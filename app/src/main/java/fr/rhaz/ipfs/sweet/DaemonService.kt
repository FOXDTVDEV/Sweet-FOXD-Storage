package fr.rhaz.ipfs.sweet

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_MIN
import android.app.Service
import android.content.Intent
import android.graphics.Color.parseColor
import android.os.Build
import android.os.Build.SUPPORTED_ABIS
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.FileReader
import java.lang.Runtime.getRuntime

class DaemonService : Service(), CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext get() = Dispatchers.IO + job

    override fun onBind(intent: Intent) = null

    var daemon: Process? = null

    val store get() = getExternalFilesDir(null)!!["ipfs"]
    val bin get() = filesDir["ipfsbin"]

    val config get() = JsonParser().parse(FileReader(store["config"])).asJsonObject

    fun config(consumer: JsonObject.() -> Unit) {
        val config = config.apply(consumer)
        val data = GsonBuilder().setPrettyPrinting().create().toJson(config)
        store["config"].writeBytes(data.toByteArray())
    }


    fun exec(cmd: String) = getRuntime().exec(
        "${bin.absolutePath} $cmd",
        arrayOf("IPFS_PATH=${store.absolutePath}")
    )

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            NotificationChannel("sweetipfs", "Sweet IPFS", IMPORTANCE_MIN).apply {
                description = "Sweet IPFS"
                getSystemService(NotificationManager::class.java)
                    .createNotificationChannel(this)
            }

        launch {
            if (!bin.exists()) install().join()
            start().join()
            startForeground(1, notification.build())
        }
    }

    fun install() = launch {
        var type: String? = null
        for (abi in SUPPORTED_ABIS) {
            if (type != null) break
            type = when (abi) {
                "arm64-v8a" -> "arm64"
                "x86_64" -> "amd64"
                "armeabi", "armeabi-v7a" -> "arm"
                "x86", "386" -> "386"
                else -> null
            }
        }

        if (type == null) throw Exception("Unsupported ABI")

        bin.delete()
        bin.createNewFile()

        val input = assets.open(type)
        val output = bin.outputStream()

        try {
            input.copyTo(output)
        } finally {
            input.close(); output.close()
        }

        bin.setExecutable(true)
    }

    fun start() = launch {
        val init = exec("init")
        init.read()
        init.waitFor()

        config {
            val headers = obj("API").obj("HTTPHeaders")
            val origins = headers.array("Access-Control-Allow-Origin")
            val webui = json("https://webui.ipfs.io")
            if (webui !in origins) origins.add(webui)
        }

        val daemon = exec("daemon")
        this@DaemonService.daemon = daemon
        daemon.read()
    }

    fun Process.read() {
        launch {
            inputStream.bufferedReader().forEachLine { println(it) }
        }
        launch {
            errorStream.bufferedReader().forEachLine { println(it) }
        }
    }

    val notification
        get() = NotificationCompat.Builder(this, "sweetipfs").apply {
            setOngoing(true)
            color = parseColor("#69c4cd")
            setSmallIcon(R.drawable.ic_cloud)
            setShowWhen(false)
            setContentTitle("Sweet IPFS")
            setContentText("IPFS is running")

            val open = pendingActivity<WebActivity>()
            setContentIntent(open)

            val exit = pendingService(intent<DaemonService>().action("exit"))
            addAction(R.drawable.ic_cloud, "exit", exit)
        }

    override fun onStartCommand(i: Intent?, f: Int, id: Int) = START_STICKY.also {
        super.onStartCommand(i, f, id)
        when (i?.action) {
            "start" -> {
            }
            "exit" -> stopSelf()
        }
    }

    override fun onDestroy() = super.onDestroy().also {
        daemon?.destroy()
        NotificationManagerCompat.from(this).cancel(1)
    }

}
