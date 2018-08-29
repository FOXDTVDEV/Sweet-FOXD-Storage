package fr.rhaz.ipfs.sweet

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.os.Build
import io.ipfs.api.IPFS
import java.io.File
import java.io.FileOutputStream

val Context.ipfsd
    get() = Daemon(this)

val Context.ipfs by lazy{IPFS("/ip4/127.0.0.1/tcp/5001")}

class Daemon(val ctx: Context) {

    val bin by lazy{File(ctx.filesDir, "ipfsbin")}
    val store by lazy{File(ctx.filesDir, ".ipfs_repo")}
    val version by lazy{File(ctx.filesDir, ".ipfs_version")}

    fun check(callback: () -> Unit = {}, err: (String) -> Unit = {}){
        if(ctx !is Activity) return;

        if(bin.exists()) callback()
        else install(callback, err)
    }

    fun install(callback: () -> Unit, err: (String) -> Unit = {}) {
        val act = ctx as? Activity ?: return

        val type = when {
            Build.CPU_ABI.toLowerCase().startsWith("x86") -> "x86"
            Build.CPU_ABI.toLowerCase().startsWith("arm") -> "arm"
            else -> return err("Unsupported architecture: ${Build.CPU_ABI}")
        }

        val progress = ProgressDialog(ctx).apply {
            setMessage("Installing...")
            setCancelable(false)
            show()
        }

        Thread {
            act.assets.open(type).copyTo(FileOutputStream(bin))
            bin.setExecutable(true)
            version.writeText(act.assets.open("version").reader().readText());

            progress.dismiss()
            act.runOnUiThread(callback)
        }.start()
    }

    fun run(cmd: String): Process {
        val env = arrayOf("IPFS_PATH=${store.absolutePath}")
        val command = "${bin.absolutePath} $cmd"
        return Runtime.getRuntime().exec(command, env)
    }

    fun init(callback: () -> Unit = {}){
        val act = ctx as? Activity ?: return

        val progress = ProgressDialog(ctx).apply {
            setMessage("Initializing...")
            setCancelable(false)
            show()
        }

        Thread{
            val exec = run("init")
            exec.waitFor()
            progress.dismiss()
            act.runOnUiThread(callback)
        }.start()

    }

}