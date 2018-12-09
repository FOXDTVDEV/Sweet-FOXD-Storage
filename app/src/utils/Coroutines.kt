package fr.rhaz.ipfs.sweet

import android.app.Service
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
    fun catchUI(
        error: (Exception) -> Any = ::alert,
        block: suspend CoroutineScope.() -> Unit
    ) = UI(block).catch(error)
}

fun CoroutineScope.UI(block: suspend CoroutineScope.() -> Unit)
    = async(Dispatchers.Main, block = block)

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