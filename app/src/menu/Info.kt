package fr.rhaz.ipfs.sweet.menu

import android.app.AlertDialog.BUTTON_POSITIVE
import android.app.AlertDialog.Builder
import android.view.Menu
import android.view.SubMenu
import fr.rhaz.ipfs.sweet.*
import fr.rhaz.ipfs.sweet.R.string.*
import kotlinx.android.synthetic.main.activity_console.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

fun Menu.sub(id: Int, builder: SubMenu.() -> Unit) = addSubMenu(id).apply(builder)

operator fun Menu.plus(id: Int) = add(id)


fun ConsoleActivity.infoMenu() = infobtn.onClick {
    popupMenu(it){

        sub(menu_identity){

            add(getString(menu_identity_peerid)).onClick{
                val id = Daemon.config.getAsJsonObject("Identity").getAsJsonPrimitive("PeerID").asString
                alertDialog(title_peerid){
                    setMessage(id)
                    setPositiveButton(getString(copy)){ d, _ -> }
                    setNeutralButton(getString(close)){ d, _ -> }
                }.apply {
                    getButton(BUTTON_POSITIVE).onClick { clipboard(id) }
                }
            }

            add(menu_identity_privatekey).onClick{
                val key = Daemon.config.obj("Identity").primitive("PrivKey").asString
                alertDialog(title_privatekey) {
                    setMessage(key)
                    setPositiveButton(getString(copy)){ d, _ -> }
                    setNeutralButton(getString(close)){ d, _ -> }
                }.apply {
                    getButton(BUTTON_POSITIVE).onClick { clipboard(key) }
                }
            }
        }

        add(menu_peers).onClick(::notimpl)
        add(menu_others).onClick{
            launch {
                val version = async(Dispatchers.IO){ IPFS().version() }.await()

            }
            async(60, {ipfs.version()},
                    {
                        val addresses = ipfsd.config.getAsJsonObject("Addresses")
                        Builder(ctx).apply {
                            setTitle(getString(title_others))
                            setMessage("""
                                    ${getString(others_goipfs_version)}: $it
                                    ${getString(others_api_address)}: ${addresses.getAsJsonPrimitive("API").asString}
                                    ${getString(others_gateway_address)}: ${addresses.getAsJsonPrimitive("Gateway").asString}
                                """.trimIndent())
                            setNeutralButton(getString(close)){ d, _ -> }
                        }.show();
                    },
                    {
                        val addresses = ipfsd.config.getAsJsonObject("Addresses")
                        Builder(ctx).apply {
                            setTitle(getString(title_others))
                            setMessage("""
                                    ${getString(others_goipfs_version)}: ${getString(others_goipfs_version_unknown)}
                                    ${getString(others_api_address)}: ${addresses.getAsJsonPrimitive("API").asString}
                                    ${getString(others_gateway_address)}: ${addresses.getAsJsonPrimitive("Gateway").asString}
                                """.trimIndent())
                        }.show();
                    }
            )
        }
    }
}