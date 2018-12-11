package fr.rhaz.ipfs.sweet

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.*
import android.net.Uri
import android.os.Environment
import android.preference.PreferenceManager
import android.provider.OpenableColumns
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.EditText
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import fr.rhaz.ipfs.sweet.R.string.error_title
import io.ipfs.api.IPFS
import io.ipfs.multihash.Multihash
import net.glxn.qrgen.android.MatrixToImageConfig
import net.glxn.qrgen.android.MatrixToImageWriter
import org.jetbrains.anko.alert
import java.io.File
import java.lang.Exception

fun Context.Exception(id: Int) = Exception(getString(id))
fun Multihash(base58: String) = Multihash.fromBase58(base58)

fun IPFS(): IPFS = IPFS("127.0.0.1", 5001)

val Context.prefs get() = PreferenceManager.getDefaultSharedPreferences(this)
val storage get() = Environment.getExternalStorageDirectory()

val Context.permissions
    get() = packageManager.getPackageInfo(packageName, GET_PERMISSIONS).requestedPermissions

operator fun File.get(path: String) = File(this, path)

fun View.visible(){ visibility = View.VISIBLE }

fun EditText.onWrite(action: () -> Unit) = setOnEditorActionListener { v, actionId, event -> action(); true }

fun rethrow(ex: Throwable): Nothing = throw ex

fun Context.catch(title: Int = error_title, callback: () -> Unit){
    try{ callback() }
    catch (ex: Throwable) {
        alert(ex.message?:"", getString(title)).show()
    }
}

fun Activity.clipboard(text: String){
    val clipboard = getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.primaryClip = ClipData.newPlainText("text", text)
}

fun qr(text: String, width: Int, height: Int) = QRCodeWriter()
    .encode(text, BarcodeFormat.QR_CODE, width, height, mapOf(EncodeHintType.MARGIN to 0))
    .let{ MatrixToImageWriter.toBitmap(it, MatrixToImageConfig(0xFF000000.toInt(), 0x00000000))}

fun Context.inputStream(uri: Uri) = contentResolver.openInputStream(uri)

fun Context.name(uri: Uri) =
    if(uri.scheme == "file") uri.lastPathSegment
    else contentResolver.query(uri, null, null, null, null).run {
        val index = getColumnIndex(OpenableColumns.DISPLAY_NAME); moveToFirst()
        getString(index).also{close()}
    }