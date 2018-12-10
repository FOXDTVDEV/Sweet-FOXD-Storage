package fr.rhaz.ipfs.sweet

import android.app.AlertDialog
import android.app.Application
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.support.v4.app.FragmentActivity
import android.text.InputType
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import com.tbruyelle.rxpermissions2.RxPermissions
import fr.rhaz.ipfs.sweet.R.string.*
import org.ligi.tracedroid.TraceDroid

class App : Application() {
    override fun onCreate() = super.onCreate().also{ TraceDroid.init(this) }
}

fun Context.send(ex: Exception){
    Intent(ACTION_SEND).apply {
        type = "plain/text"
        putExtra(EXTRA_EMAIL, arrayOf("hazae41@gmail.com"))
        putExtra(EXTRA_SUBJECT, "[Error Report] SweetIPFS")
        putExtra(EXTRA_TEXT, "${ex.javaClass.name}: ${ex.message}\n"+ex.stackTrace?.joinToString("\n"))
        startActivity(createChooser(this, "Email..."))
    }
}

fun Context.alert(ex: Exception) {
    ex.printStackTrace()
    alertDialog(error_title, ex.message){
        setNeutralButton(close){ _,_ -> }
        setPositiveButton("Send"){ _,_ -> send(ex) }
    }
}

fun Context.notimpl() {
    alertDialog(not_impl){
        setPositiveButton(close){_,_ -> }
    }
}

fun FragmentActivity.checkPermissions(
    callback: () -> Unit,
    error: () -> Unit = {}
) = RxPermissions(this).request(*permissions)
        .subscribe{ granted -> if(granted) callback() else error() }

fun MenuItem.onClick(action: () -> Unit)
    = setOnMenuItemClickListener { action(); true }

fun View.onClick(action: (View) -> Unit) = setOnClickListener{action(it)}

val EditText.value get() = text.toString()

fun Context.progress(msg: Int)
    = ProgressDialog(ctx).apply {
        setMessage(getString(msg))
        setCancelable(false)
        show()
    }

fun Context.alertDialog(title: Int, builder: AlertDialog.Builder.() -> Unit = {})
    = Dialog(title).apply(builder).show()

fun Context.alertDialog(title: Int, message: String?, builder: AlertDialog.Builder.() -> Unit = {})
    = Dialog(title).apply { setMessage(message) }.apply(builder).show()

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

fun Context.Dialog(title: Int) = AlertDialog.Builder(ctx).apply{ setTitle(title) }

fun Context.inputDialog(title: Int, action: (EditText) -> Unit)
= alertDialog(title){
    val input = editText()
    setView(input)
    setPositiveButton(apply){ d, _ ->
        catch(title){ action(input) }
    }
    setNegativeButton(cancel){ d, _ -> }
}
