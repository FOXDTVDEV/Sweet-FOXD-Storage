package fr.rhaz.ipfs.sweet

import android.app.Activity
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.InterruptedIOException

operator fun File.get(path: String) = File(this, path)

inline fun <reified T> Context.intent(builder: Intent.() -> Unit = {}) = Intent(this, T::class.java).apply(builder)
fun Intent.action(value: String) = apply { action = value }

inline fun <reified T : Activity> Context.startActivity() = startActivity(intent<T>())
inline fun <reified T : Service> Context.startService() = startService(intent<T>())

inline fun <reified T : Service> Context.pendingService() = pendingService(intent<T>())
fun Context.pendingService(intent: Intent) = PendingIntent.getService(this, 0, intent, 0)

inline fun <reified T : Activity> Context.pendingActivity() = pendingActivity(intent<T>())
fun Context.pendingActivity(intent: Intent) = PendingIntent.getActivity(this, 0, intent, 0)

fun <T> Context.catcher(action: () -> T) =
    try {
        action()
    } catch (ex: Exception) {
        Toast.makeText(this, ex.localizedMessage, Toast.LENGTH_LONG).show()
    }

fun json(value: Boolean) = JsonPrimitive(value)
fun json(value: Int) = JsonPrimitive(value)
fun json(value: String) = JsonPrimitive(value)
fun json(value: List<String>) = JsonArray().apply { value.forEach(::add) }

fun JsonObject.set(key: String, value: JsonElement) = add(key, value)
fun JsonObject.boolean(key: String) = getAsJsonPrimitive(key)?.asBoolean
fun JsonObject.string(key: String) = getAsJsonPrimitive(key)?.asString
fun JsonObject.int(key: String) = getAsJsonPrimitive(key)?.asInt

fun JsonObject.array(key: String): JsonArray {
    if (key !in keySet()) set(key, JsonArray())
    return getAsJsonArray(key)
}

fun JsonObject.obj(key: String): JsonObject {
    if (key !in keySet()) set(key, JsonObject())
    return getAsJsonObject(key)
}

fun Process.read() {
    listOf(inputStream, errorStream).forEach {
        stream -> GlobalScope.launch {
            try{
                stream.bufferedReader().forEachLine { println(it) }
            } catch(ex: InterruptedIOException){}
        }
    }
}