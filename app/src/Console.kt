package fr.rhaz.ipfs.sweet

import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.net.Uri
import android.os.Bundle
import fr.rhaz.ipfs.sweet.menu.actionMenu
import fr.rhaz.ipfs.sweet.menu.configMenu
import fr.rhaz.ipfs.sweet.menu.infoMenu
import kotlinx.android.synthetic.main.activity_console.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.anko.alert
import org.jetbrains.anko.okButton
import java.io.FileInputStream
import java.io.FileOutputStream

class ConsoleActivity: ScopedActivity() {

    override fun onBackPressed() {}
    override fun onResume() {
        super.onResume()
        checkAPI({}){startActivityNoAnimation<MainActivity>()}
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_console)
        mkconsole()
        actionMenu()
        infoMenu()
        configMenu()
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?){
        super.onActivityResult(req, res, data)
        if(res != RESULT_OK) return
        when(req){
            1 -> intent<ShareActivity>().also {
                it.data = data?.data ?: return
                it.action = ACTION_SEND
                startActivity(it)
            }
            2 -> UI {
                val uri = data?.data ?: throw Exception("Could not find data")
                val extension = name(uri).takeLast(5)
                val scheme = when(extension){
                    ".ipfs", ".ipns" -> extension.drop(1)
                    else -> throw Exception("File is not a .ipfs/.ipns")
                }
                val text = inputStream(uri).reader().readText()
                Multihash(text) ?: throw Exception("$text is not a valid multihash")
                intent<ShareActivity>().apply {
                    action = ACTION_SEND
                    putExtra("hash", text)
                    putExtra("scheme", scheme)
                    putExtra("name", name(uri))
                    startActivity(this)
                }
            }
            3 -> UIO {
                val uri = data?.data
                    ?: throw Exception("Could not find data")
                if(!name(uri).endsWith(".key"))
                    throw Exception("File is not a .key")
                val input = inputStream(uri)
                val file = Daemon.store["swarm.key"].apply { createNewFile() }
                val output = FileOutputStream(file)
                try{
                    input.copyTo(output)
                } finally {
                    input.close(); output.close()
                }
            }
        }
    }

    fun append(line: String) = console.apply {
        text = "$text\n$line"
        post {
            val y = layout.getLineTop(lineCount) - height
            if(y > 0) scrollTo(0, y)
        }
    }

    fun mkconsole() = input.onWrite {
        UI {
            val cmd = input.value
            input.text.clear()
            append("> $cmd")
            IO {
                val process = Daemon.exec(cmd)
                fun print(line: String) {
                    println(line)
                    UI { append(line) }
                }
                launch { process.inputStream.reader().forEachLine(::print) }
                launch { process.errorStream.reader().forEachLine(::print) }
                process.waitFor()
            }
        }
    }
}
