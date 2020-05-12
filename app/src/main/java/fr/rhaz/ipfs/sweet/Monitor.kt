package fr.rhaz.ipfs.sweet

import android.app.*
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import androidx.browser.customtabs.CustomTabsIntent
import androidx.preference.PreferenceManager
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Parameters
import com.github.kittinunf.fuel.coroutines.awaitString
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.*

fun String?.or(other: String) = if (isNullOrBlank()) other else this

class Monitor : Service() {

    val preferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    val API get() = Uri.parse(preferences.getString("api", null).or("http://127.0.0.1:5001"))
    val interval get() = preferences.getInt("interval", 1)

    var paused = false

    val notifier
        get() = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    inner class MonitorBinder : Binder() {
        val monitor get() = this@Monitor
    }

    override fun onBind(intent: Intent): IBinder {
        return MonitorBinder()
    }

    override fun onCreate() {
        super.onCreate()
        start()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent.action) {
            "open" -> open()
            "settings" -> settings()
            "pause" -> pause()
            "start" -> start()
        }

        return START_STICKY
    }

    fun open(path: String = "") {
        val uri = Uri.withAppendedPath(API, "webui/$path")

        val intent = CustomTabsIntent.Builder()
            .setToolbarColor(getColor(R.color.colorPrimaryDark))
            .build()

        intent.intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
        intent.launchUrl(this, uri)
    }

    fun settings() {
        val intent = Intent(this, Settings::class.java)
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    fun intent(action: String): PendingIntent {
        val intent = Intent(this, this::class.java)
        intent.action = action
        return PendingIntent.getService(this, 0, intent, 0)
    }

    fun channel(): String {
        val id = "monitoring"
        val name = "Monitoring"
        val importance = NotificationManager.IMPORTANCE_MIN
        val channel = NotificationChannel(id, name, importance)
        channel.description = "Monitoring your IPFS node"
        notifier.createNotificationChannel(channel)
        return id
    }

    fun pathOf(path: String) = Uri.withAppendedPath(API, "api/v0/$path").toString()

    suspend fun post(path: String, parameters: Parameters? = null) =
        Fuel.post(pathOf(path), parameters).awaitString()

    fun pause() {
        paused = true

        val notification = Notification.Builder(this@Monitor, channel())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(getColor(R.color.colorAccent))
            .setSubText("Monitoring")
            .setContentTitle("Paused")
            .setContentText("Monitoring is paused")
            .addAction(R.drawable.ic_notification, "Resume", intent("start"))
            .addAction(R.drawable.ic_notification, "Settings", intent("settings"))

        startForeground(1, notification.build())
    }

    fun start() {
        paused = false
        update()
    }

    fun schedule() {
        Timer().schedule(object : TimerTask() {
            override fun run() {
                update()
            }
        }, (interval * 1000).toLong())
    }

    fun update() = GlobalScope.launch(Main) {
        if (paused) return@launch

        val notification = Notification.Builder(this@Monitor, channel())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setSubText("Monitoring")
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(getColor(R.color.colorAccent))
            .addAction(R.drawable.ic_notification, "Pause", intent("pause"))
            .addAction(R.drawable.ic_notification, "Settings", intent("settings"))

        try {
            val (version, peers, ratein, rateout) = withContext(IO) {
                val version = JSONObject(post("version")).getString("Version")
                val peers = JSONObject(post("swarm/peers")).getJSONArray("Peers").length()
                val bandwidth = JSONObject(post("stats/bw"))
                val ratein = "%.2f".format(bandwidth.getDouble("RateIn") / 1000000)
                val rateout = "%.2f".format(bandwidth.getDouble("RateOut") / 1000000)
                listOf(version, peers, ratein, rateout)
            }

            notification
                .setContentTitle("Connected")
                .setContentText("v$version • $peers peers • $ratein / $rateout Mbps")
                .setContentIntent(intent("open"))

        } catch (e: FuelError) {
            println(e.message)
            notification
                .setContentTitle("Not connected")
                .setContentText("Tap to open settings")
                .setContentIntent(intent("settings"))
        } catch (e: Exception) {
            println(e.message)
            notification
                .setContentTitle("An error occured")
                .setContentText(e.message)
        }

        startForeground(1, notification.build())
        if (!paused) schedule()
    }
}
