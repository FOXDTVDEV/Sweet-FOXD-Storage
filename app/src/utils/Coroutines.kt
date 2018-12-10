package fr.rhaz.ipfs.sweet

import android.app.Service
import android.content.Context
import android.support.v7.app.AppCompatActivity
import kotlinx.coroutines.*
import java.lang.Error

abstract class ScopedService: Service(), CoroutineScope{
    private val job = SupervisorJob()
    override val coroutineContext get() = Dispatchers.Main + job
}

open class ScopedActivity: AppCompatActivity(), CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext get() = Dispatchers.Main + job
}

fun <T> T.UI(
    error: (Exception) -> Any = ::alert,
    block: suspend CoroutineScope.() -> Unit
) where T:Context,T:CoroutineScope = unsafeUI(block).catch(error)

fun CoroutineScope.unsafeUI(block: suspend CoroutineScope.() -> Unit)
    = async(Dispatchers.Main, block = block)

suspend fun <T> IO(block: suspend CoroutineScope.() -> T)
    :T = withContext(Dispatchers.IO, block = block)

fun Deferred<*>.catch(
    error: (Exception) -> Any, success: () -> Unit = {}
) = invokeOnCompletion {
    when (it) {
        is Error -> throw it
        is Exception -> error(it)
        else -> success()
    }
}

fun CoroutineScope.checkAPI(
    success: () -> Unit, error: (Exception) -> Unit
) = async(Dispatchers.IO) { IPFS() }.catch(error, success)