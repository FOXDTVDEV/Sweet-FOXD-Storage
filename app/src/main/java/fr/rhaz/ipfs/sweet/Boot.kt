package fr.rhaz.ipfs.sweet

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class Boot : BroadcastReceiver(){
    override fun onReceive(context: Context, intent: Intent) {
        val service = Intent(context, Node::class.java)
        context.startForegroundService(service)
    }
}