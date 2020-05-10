package fr.rhaz.ipfs.sweet

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class Boot : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return;
 
        val service = Intent(context, Monitor::class.java)
        context.startService(service)
    }
}
