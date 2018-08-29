package fr.rhaz.ipfs.sweet

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.os.Build
import okio.Okio
import java.io.File

class IPFSDaemon(val androidContext: Context) {

    val bin by lazy{File(androidContext.filesDir, "ipfsbin")}
    val store by lazy{File(androidContext.filesDir, ".ipfs_repo")}
    val version by lazy{File(androidContext.filesDir, ".ipfs_version")}

    fun Activity.check(callback: () -> Unit = {}, err: (String) -> Unit = {}){
        if(bin.exists()) callback()
        else install(callback, err)
    }

    fun Activity.install(callback: () -> Unit, err: (String) -> Unit = {}) {

        val type = when {
            Build.CPU_ABI.toLowerCase().startsWith("x86") -> "x86"
            Build.CPU_ABI.toLowerCase().startsWith("arm") -> "arm"
            else -> return err("Unsupported architecture: ${Build.CPU_ABI}")
        }

        val progress = ProgressDialog(androidContext).apply {
            setMessage("Installing...")
            setCancelable(false)
            show()
        }

        Thread {
            val source = Okio.buffer(Okio.source(assets.open(type)))
            val sink = Okio.buffer(Okio.sink(bin))
            while (!source.exhausted()) source.read(sink.buffer(), 1024)
            source.close()
            sink.close()
            bin.setExecutable(true)
            version.writeText(assets.open("version").reader().readText());

            progress.dismiss()
            runOnUiThread(callback)
        }.start()
    }

    fun run(cmd: String): Process {
        val env = arrayOf("IPFS_PATH=${store.absolutePath}")
        val command = "${bin.absolutePath} $cmd"
        return Runtime.getRuntime().exec(command, env)
    }

    fun Activity.init(callback: () -> Unit = {}){

        val progress = ProgressDialog(androidContext).apply {
            setMessage("Initializing...")
            setCancelable(false)
            show()
        }

        Thread{
            val exec = run("init")
            exec.waitFor()
            progress.dismiss()
            runOnUiThread(callback)
        }.start()

    }

}