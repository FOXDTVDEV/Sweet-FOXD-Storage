package fr.rhaz.ipfs.sweet

import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.os.Bundle
import fr.rhaz.ipfs.sweet.menu.actionMenu
import fr.rhaz.ipfs.sweet.menu.configMenu
import fr.rhaz.ipfs.sweet.menu.infoMenu
import kotlinx.android.synthetic.main.activity_console.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream

class ConsoleActivity: ScopedActivity() {

    override fun onBackPressed() {}

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
            111 -> launch(Dispatchers.IO) {
                val input = FileInputStream(data?.data?.path!!.split(":")[1])
                val output = FileOutputStream(Daemon.store["/swarm.key"])
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
        val cmd = input.value
        input.text.clear()
        append("> $cmd")
        launch(Dispatchers.IO) {
            val process = Daemon.exec(cmd)
            fun print(line: String) {
                println(line)
                unsafeUI { append(line) }
            }
            launch { process.inputStream.reader().forEachLine(::print) }
            launch { process.errorStream.reader().forEachLine(::print) }
            process.waitFor()
        }
    }
}
