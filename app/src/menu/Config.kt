package fr.rhaz.ipfs.sweet.menu

import android.content.Intent
import android.content.Intent.*
import android.view.Menu
import android.widget.EditText
import com.google.gson.JsonObject
import fr.rhaz.ipfs.sweet.*
import fr.rhaz.ipfs.sweet.R.string.*
import kotlinx.android.synthetic.main.activity_console.*
import org.jetbrains.anko.*
import fr.rhaz.ipfs.sweet.UI
import fr.rhaz.ipfs.sweet.utils.*

fun ConsoleActivity.configMenu() = configbtn.onClick {

    fun Menu.switch(res: Int, key: String, accessor: JsonObject.() -> JsonObject){
        item(res).apply{
            isCheckable = true
            UI { isChecked = silentIO { Daemon.config.accessor().boolean(key) } }
            onClick{ UI {
                isChecked = !isChecked
                silentIO {
                    Daemon.config{ accessor().set(key, json(isChecked)) }
                }
            } }
        }
    }

    popupMenu(it){

        item(menu_bootstrap){
            UI {
                val lines = silentIO { Daemon.config.array("Bootstrap").map{it.asString} }
                alert {
                    title = getString(menu_bootstrap)
                    val input = inputView{
                        setText(lines.joinToString("\n\n"))
                        textSize = 14f
                    }
                    cancelButton {  }
                    okButton {
                        UI {
                            val lines = input.value.split("\n").filter { it.isNotBlank() }
                            silentIO {
                                Daemon.config {
                                    val bootstrap = array("Bootstrap")
                                    bootstrap.removeAll { true }
                                    lines.forEach(bootstrap::add)
                                }
                            }
                        }
                    }
                    show()
                }
            }
        }

        sub(menu_gateway){
            switch(menu_gateway_writable, "Writable"){obj("Gateway")}
        }

        sub(menu_api){
            sub(menu_api_headers){
                item(menu_api_headers_origins){
                    UI{
                        val lines = silentIO {
                            val headers = Daemon.config.obj("API").obj("HTTPHeaders")
                            headers.array("Access-Control-Allow-Origin").map{it.asString}
                        }
                        alert{
                            title = getString(title_api_headers_origin)
                            val input = inputView{
                                setText(lines.joinToString("\n"))
                                textSize = 14f
                            }
                            okButton {
                                UI {
                                    val lines = input.value.split("\n").filter { it.isNotBlank() }
                                    silentIO {
                                        Daemon.config {
                                            val bootstrap = array("Bootstrap")
                                            bootstrap.removeAll { true }
                                            lines.forEach(bootstrap::add)
                                        }
                                    }
                                }
                            }
                            cancelButton {  }
                            show()
                        }
                    }
                }
            }
        }

        item(menu_swarm_key){
            val intent = Intent(ACTION_GET_CONTENT).setType("*/*")
            val chooser = createChooser(intent, "Select your swarm.key file")
            startActivityForResult(chooser, 111)
        }

        sub(menu_experimental){
            val accessor: JsonObject.() -> JsonObject = {obj("Experimental")}
            switch(menu_experimental_filestore, "FilestoreEnabled", accessor)
            switch(menu_experimental_urlstore, "UrlstoreEnabled", accessor)
            switch(menu_experimental_sharding, "ShardingEnabled", accessor)
            switch(menu_experimental_libp2p_stream_mounting, "Libp2pStreamMounting", accessor)
            switch(menu_experimental_p2phttpproxy, "P2pHttpProxy", accessor)
            switch(menu_experimental_quic, "QUIC", accessor)
        }
    }
}