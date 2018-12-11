package fr.rhaz.ipfs.sweet

import android.app.Service
import android.content.Context
import android.support.v7.app.AppCompatActivity
import fr.rhaz.ipfs.sweet.R.string.loading
import kotlinx.coroutines.*
import org.jetbrains.anko.custom.async

abstract class ScopedService: Service(), CoroutineScope{
    private val job = SupervisorJob()
    override val coroutineContext get() = Dispatchers.Main + job
    override fun onDestroy() { job.cancel(); super.onDestroy() }
}

open class ScopedActivity: AppCompatActivity(), CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext get() = Dispatchers.Main + job
    override fun onDestroy() { job.cancel(); super.onDestroy() }
}

fun <T> T.UI(
    error: (Exception) -> Any = ::alert,
    block: suspend CoroutineScope.() -> Unit
) where T:Context,T:CoroutineScope = unsafeUI(block).catch(error)

fun CoroutineScope.unsafeUI(block: suspend CoroutineScope.() -> Unit)
    = async(Dispatchers.Main, block = block)

suspend fun <T> Context.silentIO(
    block: suspend CoroutineScope.() -> T
) :T = withContext(Dispatchers.IO, block = block)

suspend fun <T> Context.IO(
    message: Int = loading,
    cancellable: Boolean = true,
    block: suspend CoroutineScope.() -> T
) :T = coroutineScope{
    val progress = progress(message)
    progress.setCancelable(cancellable)
    try{
        val job = async(Dispatchers.IO, block = block)
        progress.setOnCancelListener{ job.cancel() }
        job.await()
    } finally {
        progress.dismiss()
    }
}

fun Deferred<*>.catch(
    error: (Exception) -> Any, success: () -> Unit = {}
) = invokeOnCompletion {
    it?.printStackTrace()
    when (it) {
        is Error -> throw it
        is Exception -> error(it)
        else -> success()
    }
}

fun CoroutineScope.checkAPI(
    success: () -> Unit, error: (Exception) -> Unit
) = async(Dispatchers.IO) { IPFS() }.catch(error, success)