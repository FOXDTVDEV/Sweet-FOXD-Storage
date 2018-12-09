package fr.rhaz.ipfs.sweet

import android.app.*
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.*
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.tbruyelle.rxpermissions2.RxPermissions
import fr.rhaz.ipfs.sweet.R.string.*
import net.glxn.qrgen.android.MatrixToImageConfig
import net.glxn.qrgen.android.MatrixToImageWriter
import io.ipfs.api.IPFS
import kotlinx.coroutines.*
import org.ligi.tracedroid.TraceDroid
import java.io.File
import kotlin.concurrent.thread
import kotlin.coroutines.*

class App : Application() {
    override fun onCreate() { super.onCreate(); TraceDroid.init(this) }
}

fun Context.alert(ex: Exception) { alertDialog(error_title, ex.message); ex.printStackTrace() }

fun Context.notimpl() { alertDialog(not_impl, getString(not_impl)) }

fun FragmentActivity.checkPermissions(callback: () -> Unit, error: () -> Unit = {})
    = RxPermissions(this)
    .request(*permissions)
    .subscribe{ granted -> if(granted) callback() else error() }

fun MenuItem.onClick(action: () -> Unit)
    = setOnMenuItemClickListener { action(); true }

fun View.onClick(action: (View) -> Unit) = setOnClickListener{action(it)}

val EditText.value get() = text.toString()

fun Context.progress(msg: Int) = ProgressDialog(ctx).apply {
    setMessage(getString(msg))
    setCancelable(false)
    show()
}

fun Context.alertDialog(title: Int, builder: Dialog.() -> Unit = {})
    = Dialog(this, title).apply(builder).show()

fun Context.alertDialog(title: Int, message: String?)
= alertDialog(title){
    setMessage(message)
    setPositiveButton(close){ d, _ -> }
}

fun Context.editText(builder: EditText.() -> Unit = {})
= EditText(ctx).apply{
    inputType = InputType.TYPE_CLASS_TEXT
    apply(builder)
}

class Dialog(val ctx: Context, val title: Int): AlertDialog.Builder(ctx){
    init { setTitle(title) }
    fun getTitle() = ctx.getString(title)
}

fun Context.inputDialog(title: Int, action: (EditText) -> Unit)
= alertDialog(title){
    val input = editText()
    setView(input)
    setPositiveButton(apply){ d, _ ->
        catch(title){ action(input) }
    }
    setNegativeButton(cancel){ d, _ -> }
}
