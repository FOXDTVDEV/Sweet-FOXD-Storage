package fr.rhaz.ipfs.sweet

import android.app.Activity
import android.app.Application
import android.app.ProgressDialog.*
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.Intent.*
import android.support.v4.app.FragmentActivity
import android.view.View
import android.widget.EditText
import fr.rhaz.ipfs.sweet.R.string.*
import org.jetbrains.anko.*
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

fun Context.alert(ex: Exception) = alert(ex){}
fun Activity.alertFinish(ex: Exception) = alert(ex){finish()}
fun Context.alert(ex: Exception, then: () -> Unit = {}) {
    ex.printStackTrace()
    alert("${ex.message} (${ex.javaClass.simpleName})"){
        titleResource = error_title
        closeButton{then()}
        positiveButton(report){ send(ex) }
        show()
    }
}

fun Context.notimpl() {
    alert(not_impl){ okButton{} }.show()
}

fun View.onClick(action: (View) -> Unit) = setOnClickListener{action(it)}

val EditText.value get() = text.toString()

fun Context.progress(msg: Int)
    = indeterminateProgressDialog(msg) { show() }

inline fun AlertBuilder<*>.yes(noinline handler: (dialog: DialogInterface) -> Unit) =
        positiveButton(R.string.yes, handler)

inline fun AlertBuilder<*>.no(noinline handler: (dialog: DialogInterface) -> Unit) =
        negativeButton(R.string.no, handler)

fun AlertBuilder<*>.closeButton(action: (DialogInterface) -> Unit = {}) = neutralPressed(R.string.close, action)

fun AlertBuilder<*>.inputView(builder: EditText.() -> Unit = {}): EditText {
    lateinit var input: EditText
    customView {
        verticalLayout {
            padding = dip(16)
            input = editText(builder)
        }
    }
    return input
}
