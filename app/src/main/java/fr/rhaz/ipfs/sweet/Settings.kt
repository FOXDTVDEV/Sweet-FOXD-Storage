package fr.rhaz.ipfs.sweet

import android.content.*
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.*


class Settings : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    val connection = MonitorConnection()
    val settings = SettingsFragment()

    val preferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val service = Intent(this, Monitor::class.java)
        bindService(service, connection, Context.BIND_AUTO_CREATE)
        startService(service)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, settings)
            .commit()

        Timer().schedule(object : TimerTask() {
            override fun run() {
                update()
            }
        }, 0, 1000)

        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(it: SharedPreferences, key: String) {
        GlobalScope.launch(Main) {
            try {
                val monitor = connection.monitor ?: return@launch
                if (key == "lowpower") {
                    val value = preferences.getBoolean(key, false)
                    val arg = if (value) "lowpower" else "default-networking"

                    withContext(IO) {
                        monitor.post("config/profile/apply?arg=$arg")
                    }

                    Toast.makeText(
                        this@Settings,
                        R.string.settings_restart_required,
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@Settings, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
    }

    fun update() = GlobalScope.launch(Main) {
        try {
            val monitor = connection.monitor ?: throw Exception();
            val version = withContext(IO) {
                JSONObject(monitor.post("version")).getString("Version")
            }

            info.text = String.format(getString(R.string.settings_state_connected), version)
            info.setTextColor(getColor(R.color.colorAccent))

            settings.preferenceScreen.apply {
                findPreference<SwitchPreference>("lowpower")?.isEnabled = true
            }
        } catch (e: Exception) {
            info.text = getString(R.string.settings_state_disconnected)
            info.setTextColor(getColor(android.R.color.holo_red_light))

            settings.preferenceScreen.apply {
                findPreference<SwitchPreference>("lowpower")?.isEnabled = false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.appbar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_help -> true.also {
            startActivity(Intent(this, Help::class.java))
        }
        else -> super.onOptionsItemSelected(item)
    }

    inner class MonitorConnection : ServiceConnection {
        var monitor: Monitor? = null

        override fun onServiceDisconnected(name: ComponentName?) {}
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            monitor = (binder as Monitor.MonitorBinder).monitor
        }
    }
}

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun setDivider(divider: Drawable?) {
        super.setDivider(ColorDrawable(Color.TRANSPARENT))
    }
}
