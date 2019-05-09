package fr.rhaz.ipfs.sweet

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_MIN
import android.app.Service
import android.content.Context
import android.content.Context.*
import android.content.Intent
import android.graphics.Color.parseColor
import android.os.Build
import android.os.Build.CPU_ABI
import android.os.Build.SUPPORTED_ABIS
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import java.io.File
import java.io.FileReader
import java.io.InterruptedIOException
import java.lang.Runtime.getRuntime
import androidx.core.content.ContextCompat.getSystemService



class DaemonService : Service() {

    override fun onBind(intent: Intent) = null

    var daemon: Process? = null

    val store get() = getExternalFilesDir(null)!!["ipfs"]
    val bin get() = baseContext.filesDir["goipfs"]

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

        install()
        start()
        startForeground(1, notification.build())
    }

    fun install() {

        val type = CPU_ABI.let {
            when{
                it.startsWith("arm") -> "arm"
                it.startsWith("x86") -> "386"
                else ->  throw Exception("Unsupported ABI")
            }
        }

        bin.apply {
            delete()
            createNewFile()
        }

        val input = assets.open(type)
        val output = bin.outputStream()

        try {
            input.copyTo(output)
        } finally {
            input.close(); output.close()
        }

        bin.setExecutable(true)
        println("Installed binary")
    }

    fun start() {
        exec("init").apply {
            read()
            waitFor()
        }

        config {
            val headers = obj("API").obj("HTTPHeaders")
            val origins = headers.array("Access-Control-Allow-Origin")
            val webui = json("https://webui.ipfs.io")
            if (webui !in origins) origins.add(webui)
        }

        exec("daemon").apply {
            daemon = this
            read()
        }
    }

    fun stop() {
        daemon?.destroy()
        daemon = null
    }

    val notificationBuilder = NotificationCompat.Builder(this, "sweetipfs")

    val notification
        get() = notificationBuilder.apply {
            mActions.clear()
            setOngoing(true)
            setOnlyAlertOnce(true)
            color = parseColor("#69c4cd")
            setSmallIcon(R.drawable.ic_cloud)
            setShowWhen(false)
            setContentTitle("Sweet IPFS")

            val open = pendingActivity<WebActivity>()
            setContentIntent(open)

            if(daemon == null){
                setContentText("IPFS is not running")

                val start = pendingService(intent<DaemonService>().action("start"))
                addAction(R.drawable.ic_cloud, "start", start)
            }

            else {
                setContentText("IPFS is running")

                val restart = pendingService(intent<DaemonService>().action("restart"))
                addAction(R.drawable.ic_cloud, "restart", restart)

                val stop = pendingService(intent<DaemonService>().action("stop"))
                addAction(R.drawable.ic_cloud, "stop", stop)
            }

            val exit = pendingService(intent<DaemonService>().action("exit"))
            addAction(R.drawable.ic_cloud, "exit", exit)
        }

    override fun onStartCommand(i: Intent?, f: Int, id: Int) = START_STICKY.also {
        super.onStartCommand(i, f, id)
        when (i?.action) {
            "start" -> start()
            "stop" -> stop()
            "restart" -> {
                stop(); start()
            }
            "exit" -> System.exit(0)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(1, notification.build())
    }

}
