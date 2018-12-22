package fr.rhaz.ipfs.sweet.menu

import android.content.Intent
import android.content.Intent.ACTION_GET_CONTENT
import android.content.Intent.createChooser
import android.view.Menu
import com.google.gson.JsonObject
import fr.rhaz.ipfs.sweet.*
import fr.rhaz.ipfs.sweet.R.string.*
import fr.rhaz.ipfs.sweet.utils.*
import kotlinx.android.synthetic.main.activity_console.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.cancelButton
import org.jetbrains.anko.okButton

fun ConsoleActivity.configMenu() = configbtn.onClick {

    fun Menu.switch(res: Int, key: String, accessor: JsonObject.() -> JsonObject){
        item(res).apply{
            isCheckable = true
            UI { isChecked = silentIO { Daemon.config.accessor().boolean(key) ?: false } }
            onClick{ UI {
                isChecked = !isChecked
                silentIO {
                    Daemon.config{ accessor().set(key, json(isChecked)) }
                }
            } }
        }
    }

    popupMenu(it){

        item(menu_config_args){
            UI{
                val args = silentIO{ Daemon.config.obj("Sweet").string("Args") ?: "" }
                alert{
                    title = getString(menu_config_args)
                    val input = inputView{setText(args.replace(" ", "\n"))}
                    cancelButton {  }
                    okButton { UIO{
                        Daemon.config {
                            obj("Sweet").set("Args", json(input.value.replace("\n", " ")))
                        }
                    } }
                    show()
                }
            }
        }

        item(menu_bootstrap){
            UI {
                val lines = silentIO { Daemon.config.array("Bootstrap").map{it.asString} }
                alert {
                    title = getString(menu_bootstrap)
                    val input = inputView(menu_bootstrap_hint){
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
            item(menu_gateway_public){ UI {
                val value = silentIO{Daemon.config.obj("Sweet").obj("Gateway").string("Public")}
                alert{
                    title = getString(menu_gateway_public)
                    val input = inputView(menu_gateway_public_hint) { setText(value) }
                    okButton { UI { silentIO {
                        Daemon.config {
                            obj("Sweet").obj("Gateway").set("Public", json(input.value))
                        }
                    } } }
                    show()
                }
            } }
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
            startActivityForResult(chooser, 3)
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