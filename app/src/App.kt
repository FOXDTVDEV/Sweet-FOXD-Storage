package fr.rhaz.ipfs.sweet

import android.app.Activity
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import io.ipfs.kotlin.IPFS
import org.ligi.tracedroid.TraceDroid
import java.io.File

class App : Application() {
    override fun onCreate() = super.onCreate().also{TraceDroid.init(this)}
}

val storage get() = Environment.getExternalStorageDirectory()

operator fun File.get(path: String) = File(this, path)

val ipfs by lazy{IPFS()}

fun chain(vararg cbs: (() -> Unit) -> Unit){
    var last: () -> Unit = {(cbs.last()){}}
    for(cb in cbs.dropLast(1).reversed()){
        val last2 = last
        last = {cb(last2)}
    }
    last()
}

fun Activity.check(callback: () -> Unit, error: () -> Unit) = Thread{
    try {
        ipfs.info.version()
        runOnUiThread(callback)
    } catch(ex: Exception){
        runOnUiThread(error)
    }
}.start()

fun Activity.clipboard(text: String){
    val clipboard = getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager;
    clipboard.primaryClip = ClipData.newPlainText("text", text)
}
fun Activity.async(timeout: Int, runnable: () -> Any?, success: (Any) -> Unit, error: () -> Unit) = Thread{
    try {
        val result = runnable()
        if(result != null)
            runOnUiThread{success(result)}
        else runOnUiThread(error)
    }catch(ex: Exception) {runOnUiThread(error)}
}.let{tasker(it, timeout, error)}

fun Activity.tasker(thread: Thread, timeout: Int, error: () -> Unit) = {
    thread.start()
    val start = System.currentTimeMillis()
    while (thread.isAlive) {
        Thread.sleep(1000)
        if (System.currentTimeMillis() - start > (timeout * 1000)){
            thread.interrupt(); runOnUiThread(error); break
        }
    }
}.let {Thread(it).start()}

fun Activity.wait(timeout: Long, callback: () -> Unit) = {
    Thread.sleep(timeout)
    runOnUiThread{ callback() }
}.let { Thread(it) }.apply { start() }