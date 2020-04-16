package fr.rhaz.ipfs.sweet

import android.app.*
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.net.Uri
import android.os.Binder
import android.os.Build.CPU_ABI
import android.os.IBinder
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.FileProvider
import fr.rhaz.ipfs.sweet.BuildConfig.APPLICATION_ID
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.NetworkInterface
import kotlin.reflect.KFunction
import kotlin.system.exitProcess


class Node : Service() {

    private val notification = Notifications()
    private val daemon = Daemon()

    override fun onCreate() {
        super.onCreate()
        notification.makeChannel()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        daemon.destroy()
    }

    inner class NodeBinder : Binder() {
        fun logs() = daemon.logs
        fun execute(command: String) = daemon.execute(command)
        fun open() = this@Node.open()
    }

    override fun onBind(intent: Intent): IBinder {
        return NodeBinder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when(intent?.action) {
            "purge" -> daemon.purge()
            "purge-confirm" -> notification.setPurgeConfirm()
            "logs" -> logs()
            "open" -> open()
            "stop" -> daemon.stop()
            "back" -> notification.back?.invoke()
            "start" -> daemon.start()
            "exit" -> exit()
            else -> {
                if(daemon.state == "started") open()
                else daemon.start(true)
            }
        }

        return START_STICKY
    }

    fun exit(){
        onDestroy()
        exitProcess(0)
    }

    fun open() = GlobalScope.launch(Main) {
        val address = withContext(IO){ getAddress() }
        val uri = Uri.parse("http://${address}:5001/webui")

        val intent = CustomTabsIntent.Builder()
            .setToolbarColor(getColor(R.color.colorPrimaryDark))
            .build()

        intent.intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
        intent.launchUrl(this@Node, uri)
    }

    fun logs(){
        val uri = FileProvider.getUriForFile(this, "$APPLICATION_ID.provider", daemon.logs)

        val intent = CustomTabsIntent.Builder()
            .setToolbarColor(getColor(R.color.colorPrimaryDark))
            .build()

        intent.intent.addFlags(FLAG_GRANT_READ_URI_PERMISSION)
        intent.intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
        intent.launchUrl(this, uri)
    }

    fun getAddress(): String? {
        val nets = NetworkInterface.getNetworkInterfaces()
        val net = nets.nextElement()
        for (address in net.inetAddresses) {
            val host = address.hostAddress
            if(host.contains(":")) continue;
            return host;
        }
        return "127.0.0.1";
    }

    data class Log(val type: String, val msg: String)

    class UnsupportedException() : Exception()

    inner class Daemon {
        var state = "stopped"

        var openRequested = false

        var process: Process? = null

        val bin get() = File(filesDir, "goipfs")
        val store get() = File(getExternalFilesDir(null), "ipfs")
        val logs get() = File(getExternalFilesDir(null), "logs.txt")

        fun execute(command: String) = Runtime.getRuntime().exec(
            "${bin.absolutePath} $command",
            arrayOf("IPFS_PATH=${store.absolutePath}")
        )

        fun broadcast(event: String){
            val intent = Intent("fr.rhaz.ipfs.sweet.NODE")
            intent.action = event
            sendBroadcast(intent)
        }

        fun onInstall(){
            state = "installing"
            broadcast("install")
            notification.setInstalling()
        }

        fun onUnsupported(){
            state = "unsupported"
            broadcast("unsupported")
            notification.setUnsupported()
        }

        fun onStarting(){
            state = "starting"
            broadcast("starting")
            notification.setStarting()
        }

        fun onStart() {
            state = "started"
            broadcast("start")
            notification.setStarted()

            if(openRequested){
                open()
                openRequested = false
            }
        }

        fun onStop(){
            state = "stopped"
            broadcast("stop")
            notification.setStopped()
        }

        fun onError(){
            state = "stopped"
            broadcast("stop")
            notification.setError()
        }

        fun onLog(log: Log) {
            logs.appendText(log.msg + "\n")
            broadcast("log")

            if(state == "stopped") return;
            if(log.type == "error") onError()
            if(log.msg == "Daemon is ready") onStart()
        }

        fun Process.readLogs() = GlobalScope.launch(Main) {
            launch(IO){
                try {
                    inputStream
                        .bufferedReader()
                        .forEachLine { onLog(Log("info", it)) }
                }catch (e: Exception){}
            }

            launch(IO) {
                try{
                    errorStream
                        .bufferedReader()
                        .forEachLine{ onLog(Log("error", it)) }
                } catch (e: Exception){}
            }
        }

        fun install(){
            val cpu = when {
                CPU_ABI.startsWith("arm") -> "arm"
                CPU_ABI.startsWith("x86") -> "x86"
                else -> throw UnsupportedException()
            }

            if(bin.exists()) bin.delete()
            bin.createNewFile()
            val asset = assets.open(cpu);
            val output = bin.outputStream()

            try{
                asset.copyTo(output)
            } finally {
                asset.close()
                output.close()
            }

            bin.setExecutable(true, false)
            execute("init").waitFor()

            execute("config --json API.Gateway.HTTPHeaders.Access-Control-Allow-Headers [\"*\"]").waitFor()
            execute("config --json API.Gateway.HTTPHeaders.Access-Control-Allow-Origin [\"*\"]").waitFor()
            execute("config --json API.HTTPHeaders.Access-Control-Allow-Origin [\"*\"]").waitFor()
            execute("config --json API.HTTPHeaders.Access-Control-Allow-Methods [\"*\"]").waitFor()
            execute("config --json Addresses.API \"/ip4/0.0.0.0/tcp/5001\"").waitFor()
            execute("config --json Addresses.Gateway \"/ip4/${getAddress()}/tcp/8080\"").waitFor()
        }

        fun start(thenOpen: Boolean = false) = GlobalScope.launch(Main) {
            if(state !== "stopped") return@launch

            try {
                destroy()
                onInstall()
                withContext(IO) { install() }
                process = execute("daemon")
                process?.readLogs()
                onStarting()
                if(thenOpen) openRequested = true
            } catch(e: UnsupportedException){
                onUnsupported()
            } catch(e: Exception){
                val name = e::class.java.simpleName
                val msg = e.message ?: "An error occured"
                onLog(Log("error", "$name:$msg"))
            }
        }

        fun destroy(){
            process?.destroyForcibly()
            process = null;
        }

        fun stop(){
            destroy()
            onStop()
        }

        fun purge(){
            store.deleteRecursively()
            notification.setStopped()
        }
    }

    inner class Notifications {
        var back: (() -> Unit)? = null

        val notifier
            get() = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        fun makeChannel() {
            val id = getString(R.string.channel_id)
            val name = getString(R.string.channel_name)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(id, name, importance)
            channel.description = getString(R.string.channel_description)
            notifier.createNotificationChannel(channel)
        }

        fun makeNotification(): Notification.Builder {
            val channel = getString(R.string.channel_id)

            return Notification.Builder(this@Node, channel)
                .setSmallIcon(R.drawable.ic_icon)
                .setColor(getColor(R.color.colorAccent))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setContentTitle(getString(R.string.notification_title))
        }

        fun makePIntent(action: String): PendingIntent {
            val intent = Intent(this@Node, this@Node::class.java)
            intent.action = action;
            return PendingIntent.getService(this@Node, 0, intent, 0);
        }

        fun Notification.Builder.addAction(action: String, title: Int): Notification.Builder {
            val pintent = makePIntent(action)
            val aintent = Notification.Action.Builder(
                R.drawable.ic_icon,
                getString(title),
                pintent
            ).build()

            return this.addAction(aintent)
        }

        fun setInstalling(){
            val notification = makeNotification()
                .setContentText(getString(R.string.notification_installing))

            startForeground(1, notification.build())
        }

        fun setUnsupported(){
            val notification = makeNotification()
                .setContentText(getString(R.string.notification_unsupported))

            startForeground(1, notification.build())
        }

        fun setStarted(){
            val notification = makeNotification()
                .setContentIntent(makePIntent("open"))
                .setContentText(getString(R.string.notification_started))
                .addAction("open", R.string.action_open)
                .addAction("stop", R.string.action_stop)

            startForeground(1, notification.build())
        }

        fun setStarting(){
            val notification = makeNotification()
                .setContentText(getString(R.string.notification_starting))
                .addAction("logs", R.string.action_logs)
                .addAction("stop", R.string.action_stop)

            startForeground(1, notification.build())
        }

        fun setStopped(){
            back = { setStopped() }

            val notification = makeNotification()
                .setContentText(getString(R.string.notification_stopped))
                .addAction("start", R.string.action_start)
                .addAction("purge-confirm", R.string.action_purge)

            startForeground(1, notification.build())
        }

        fun setError(){
            back = { setError() }

            val notification = makeNotification()
                .setContentIntent(makePIntent("logs"))
                .setContentText(getString(R.string.notification_error))
                .addAction("stop", R.string.action_dismiss)
                .addAction("logs", R.string.action_logs)

            startForeground(1, notification.build())
        }

        fun setPurgeConfirm(){
            val notification = makeNotification()
                .setContentText(getString(R.string.notification_purge))
                .addAction("purge", R.string.action_confirm)
                .addAction("back", R.string.action_back)

            startForeground(1, notification.build())
        }
    }
}
