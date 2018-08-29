package fr.rhaz.ipfs.sweet

import android.net.Uri
import android.text.TextUtils

class IPXSResource(uri: Uri) {

    override fun toString() = "$type:$address"
    fun toPublic() = "https://ipfs.io/$type/$address/"

    val err: (String) -> Nothing = {throw IllegalArgumentException(it)}

    val address: String by lazy {
        when (uri.scheme) {
            "fs" -> {
                if(uri.host != null) uri.path.substring(1)
                else uri.pathSegments.run{subList(1, size)}.joinToString("/")
            }
            "ipfs", "ipns" -> uri.schemeSpecificPart.substring(2)
            "http", "https" -> uri.pathSegments.run{subList(1, size)}.joinToString("/")
            else -> err("Could not resolve address for $uri")
        }
    }

    val type: String by lazy {
        when(uri.scheme) {
            "fs" -> (uri.host?.toLowerCase() ?: uri.pathSegments[0].replace("/", "")).also{
                if(it !in listOf("ipfs", "ipns"))
                    err("When scheme is fs:// then it must follow with ipfs or ipns but was $it")
            }
            "ipfs", "ipns" -> uri.scheme
            "http", "https" -> uri.pathSegments[0].also{
                if(uri.host != "ipfs.io")
                    err("when scheme is http(s) then host has to be ipfs.io")
                if(it !in listOf("ipfs", "ipns"))
                    err("cannot handle this ipfs.io url $it")
            }
            else -> err("scheme not supported")
        }
    }
}
