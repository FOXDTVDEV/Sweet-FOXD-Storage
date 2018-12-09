package fr.rhaz.ipfs.sweet

import android.app.Activity
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.Intent.*

inline fun <reified T> Context.intent() = Intent(this, T::class.java)
fun Intent.action(value: String) = apply { action = value }

inline fun <reified T: Activity> Context.startActivity() = startActivity(intent<T>())
inline fun <reified T: Service> Context.startService() = startService(intent<T>())

inline fun <reified T: Activity> Context.startActivityNoAnimation()
    = intent<T>().let{
        it.flags += FLAG_ACTIVITY_NO_ANIMATION
        startActivity(it)
    }

inline fun <reified T: Service> Context.pendingService() = pendingService(intent<T>())
fun Context.pendingService(intent: Intent) = PendingIntent.getService(ctx, 0, intent, 0)

inline fun <reified T: Activity> Context.pendingActivity() = pendingActivity(intent<T>())
fun Context.pendingActivity(intent: Intent) = PendingIntent.getActivity(ctx, 0, intent, 0)