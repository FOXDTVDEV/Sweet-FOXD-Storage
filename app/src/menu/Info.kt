package fr.rhaz.ipfs.sweet.menu

import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import android.widget.EditText
import fr.rhaz.ipfs.sweet.*
import fr.rhaz.ipfs.sweet.R.string.*
import fr.rhaz.ipfs.sweet.UI
import fr.rhaz.ipfs.sweet.utils.obj
import fr.rhaz.ipfs.sweet.utils.string
import io.ipfs.multiaddr.MultiAddress
import kotlinx.android.synthetic.main.activity_console.*
import org.jetbrains.anko.*

fun Menu.sub(id: Int, builder: SubMenu.() -> Unit) = addSubMenu(id).apply(builder)
fun Menu.item(
    id: Int,
    action: () -> Unit = {}
) = add(id).onClick(action)

fun MenuItem.onClick(action: () -> Unit) = setOnMenuItemClickListener{action(); true }

fun ConsoleActivity.infoMenu() = infobtn.onClick {
    popupMenu(it){

        sub(menu_identity){

            item(menu_identity_peerid){
                val id = Daemon.config.obj("Identity").string("PeerID")
                alert(id, getString(title_peerid)){
                    closeButton()
                    positiveButton(copy){ clipboard(id) }
                    show()
                }
            }

            item(menu_identity_privatekey){
                val key = Daemon.config.obj("Identity").string("PrivKey")
                alert(key, getString(title_privatekey)) {
                    positiveButton(copy){ clipboard(key) }
                    closeButton()
                    show()
                }
            }
        }

        item(menu_peers){
            UI {
                val peers = IO { IPFS().swarm.peers() }
                alert {
                    title = getString(title_info_peers, peers.size)
                    customView {
                        scrollView { verticalLayout {
                            padding = dip(16)
                            peers.forEach{ peer ->
                                val hash = peer.id.toBase58()
                                textView(hash).onClick {
                                    alert{
                                        title = hash
                                        message = peer.address.toString()
                                        neutralPressed("Disconnect"){
                                            UI { IO { Daemon.exec("swarm disconnect ${peer.address}").waitFor() } }
                                        }
                                        negativeButton(copy){ clipboard(peer.toString()) }
                                        okButton {  }
                                        show()
                                    }
                                }
                            }
                        } }
                    }
                    negativeButton("Connect"){
                        alert{
                            title = getString(menu_swarm_connect)
                            lateinit var text: EditText
                            customView { verticalLayout {
                                padding = dip(16)
                                text = editText{ hint = "Qm..." }
                            } }
                            cancelButton {  }
                            okButton {
                                UI {
                                    val hash = Multihash(text.value)
                                    val address = MultiAddress(hash)
                                    IO(connecting) { IPFS().swarm.connect(address) }
                                }
                            }
                            show()
                        }
                    }
                    neutralPressed(close){}
                    show()
                }
            }
        }

        item(menu_others){
            UI {
                val version = IO { IPFS().version() }
                val addresses = Daemon.config.obj("Addresses")
                alert{
                    title = getString(title_others)
                    val msgs = mapOf(
                        others_goipfs_version to version,
                        others_api_address to addresses.string("API"),
                        others_gateway_address to addresses.string("Gateway")
                    ).map { "${getString(it.key)}: ${it.value}" }
                    message = msgs.joinToString("\n")
                    okButton { }
                    show()
                }
            }
        }
    }
}