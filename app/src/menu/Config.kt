package fr.rhaz.ipfs.sweet.menu

import android.widget.EditText
import fr.rhaz.ipfs.sweet.*
import fr.rhaz.ipfs.sweet.R.string.*
import kotlinx.android.synthetic.main.activity_console.*
import org.jetbrains.anko.*
import fr.rhaz.ipfs.sweet.UI
import fr.rhaz.ipfs.sweet.utils.*

fun ConsoleActivity.configMenu() = configbtn.onClick {
    popupMenu(it){

        item(menu_bootstrap){
            UI {
                val lines = silentIO { Daemon.config.array("Bootstrap").map{it.asString} }
                alert {
                    title = getString(menu_bootstrap)
                    lateinit var text: EditText
                    customView {
                        scrollView { verticalLayout {
                            padding = dip(16)
                            text = editText(lines.joinToString("\n\n")){
                                textSize = 14f
                            }
                        } }
                    }
                    cancelButton {  }
                    okButton {
                        UI {
                            val lines = text.value.split("\n").filter { it.isNotBlank() }
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
            item(menu_gateway_writable).apply{
                isCheckable = true
                UI {
                    isChecked = silentIO { Daemon.config.obj("Gateway").boolean("Writable") }
                }
                onClick{
                    UI {
                        isChecked = !isChecked
                        silentIO {
                            Daemon.config{
                                obj("Gateway").set("Writable", json(isChecked))
                            }
                        }
                    }
                }
            }
        }
    }


//    popupMenu(it){
//        addSubMenu(getString(menu_api)).apply {
//            addSubMenu(getString(menu_api_http_headers)).apply {
//                add(getString(menu_api_http_headers_addorigin)).setOnMenuItemClickListener {
//                    AlertDialog.Builder(ctx).apply {
//                        setTitle(getString(title_add_api_origin))
//                        fun action(origin: String){
//                            ipfsd.config{
//                                it.getAsJsonObject("API").getAsJsonObject("HTTPHeaders").apply {
//                                    if(!has("Access-Control-Allow-Origin")) {
//                                        add("Access-Control-Allow-Origin", JsonArray())
//                                        getAsJsonArray("Access-Control-Allow-Origin")
//                                                .add("http://localhost:3000")
//                                    }
//                                    getAsJsonArray("Access-Control-Allow-Origin").add(origin)
//                                }
//                            }
//                            dismiss()
//                        }
//
//                        val input = EditText(ctx).also{setView(it)}.apply {
//                            hint = "http://..."
//                            setOnEditorActionListener{tv, i, keyEvent -> action(text.toString()); true}
//                        }
//                        setPositiveButton(getString(apply)){ d, _ -> action(input.text.toString()) }
//                        setNegativeButton(getString(cancel)){ d, _ -> d.cancel()}
//                    }.show()
//                    true
//                }
//            }
//        }
//        addSubMenu(getString(menu_reprovider)).apply {
//            add(getString(menu_reprovider_interval)).setOnMenuItemClickListener{notimpl()}
//            add(getString(menu_reprovider_strategy)).setOnMenuItemClickListener{notimpl()}
//        }
//        addSubMenu(getString(menu_experimental)).apply {
//            add(getString(menu_experimental_filestore)).apply {
//                isCheckable = true
//                isChecked = false
//            }.setOnMenuItemClickListener{notimpl()}
//            add(getString(menu_experimental_urlstore)).apply {
//                isCheckable = true
//                isChecked = false
//            }.setOnMenuItemClickListener{notimpl()}
//            add(getString(menu_experimental_sharding)).apply {
//                isCheckable = true
//                isChecked = false
//            }.setOnMenuItemClickListener{notimpl()}
//            add(getString(menu_experimental_libp2p_stream_mounting)).apply {
//                isCheckable = true
//                isChecked = false
//            }.setOnMenuItemClickListener{notimpl()}
//        }
//        add(menu_swarm_key).onClick{
//            val intent = Intent().setType("*/*")
//                    .setAction(Intent.ACTION_GET_CONTENT)
//            val chooser = Intent.createChooser(intent, "Select your swarm.key file")
//            startActivityForResult(chooser, 111)
//        }
//    }
}