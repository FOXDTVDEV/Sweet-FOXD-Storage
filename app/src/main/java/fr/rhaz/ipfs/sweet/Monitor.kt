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

operator fun Uri.plus(it: String): Uri = Uri.withAppendedPath(this, it)

class Monitor : Service() {

    val preferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    val interval get() = preferences.getInt("interval", 1)

    val API: Uri
        get() {
            var api = preferences.getString("api", null)
            if (api.isNullOrBlank()) api = "http://127.0.0.1:5001"
            return Uri.parse(api)
        }

    val WebUI: Uri
        get() {
            val webui = preferences.getString("webui", null)
            if (webui.isNullOrBlank()) return API + "webui"
            return Uri.parse(webui)
        }

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
        val uri = WebUI + path

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

    fun pathOf(path: String) = (API + "api/v0/$path").toString()

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
            .setSubText(getString(R.string.notification_sub))
            .setContentTitle(getString(R.string.notification_title_paused))
            .setContentText(getString(R.string.notification_content_paused))
            .addAction(
                R.drawable.ic_notification,
                getString(R.string.notification_action_resume),
                intent("start")
            )
            .addAction(
                R.drawable.ic_notification,
                getString(R.string.notification_action_settings),
                intent("settings")
            )

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
            .addAction(
                R.drawable.ic_notification,
                getString(R.string.notification_action_pause),
                intent("pause")
            )
            .addAction(
                R.drawable.ic_notification,
                getString(R.string.notification_action_settings),
                intent("settings")
            )

        try {
            val (version, peers, ratein, rateout) = withContext(IO) {
                val version = JSONObject(post("version")).getString("Version")
                val peers = JSONObject(post("swarm/peers")).getJSONArray("Peers").length()
                val bandwidth = JSONObject(post("stats/bw"))
                val ratein = "%.2f".format(bandwidth.getDouble("RateIn") / 1000000)
                val rateout = "%.2f".format(bandwidth.getDouble("RateOut") / 1000000)
                listOf(version, peers, ratein, rateout)
            }

            val content = getString(R.string.notification_content_connected)

            notification
                .setContentTitle(getString(R.string.notification_title_connected))
                .setContentText(String.format(content, version, peers, ratein, rateout))
                .setContentIntent(intent("open"))

        } catch (e: FuelError) {
            println(e.message)
            notification
                .setContentTitle(getString(R.string.notification_title_disconnected))
                .setContentText(getString(R.string.notification_content_disconnected))
                .setContentIntent(intent("settings"))
        } catch (e: Exception) {
            println(e.message)
            notification
                .setContentTitle(getString(R.string.notification_title_error))
                .setContentText(e.message)
        }

        startForeground(1, notification.build())
        if (!paused) schedule()
    }
}
