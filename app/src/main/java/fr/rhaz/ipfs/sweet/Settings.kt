package fr.rhaz.ipfs.sweet

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import androidx.preference.PreferenceFragmentCompat

class Settings : AppCompatActivity() {
    val connection = MonitorConnection()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val service = Intent(this, Monitor::class.java)
        bindService(service, connection, Context.BIND_AUTO_CREATE)
        startService(service)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
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
}
