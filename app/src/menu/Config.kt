package fr.rhaz.ipfs.sweet.menu

import android.app.AlertDialog
import android.content.Intent
import android.widget.EditText
import fr.rhaz.ipfs.sweet.*
import fr.rhaz.ipfs.sweet.R.string.*
import kotlinx.android.synthetic.main.activity_console.*

fun ConsoleActivity.configMenu() = configbtn.onClick {
//    popupMenu(it){
//        add(getString(menu_bootstrap)).setOnMenuItemClickListener{notimpl()}
//        addSubMenu(getString(menu_gateway)).apply {
//            add(getString(menu_gateway_writable)).apply {
//                isCheckable = true
//                isChecked = false
//            }.setOnMenuItemClickListener{notimpl()}
//        }
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