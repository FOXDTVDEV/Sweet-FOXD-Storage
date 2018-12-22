package fr.rhaz.ipfs.sweet.menu

import android.graphics.Color
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import android.widget.EditText
import android.widget.TextView
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

val style = { it: TextView ->
    it.textColor = Color.BLACK
    it.isSelectable = true
}

fun ConsoleActivity.infoMenu() = infobtn.onClick {
    popupMenu(it){

        sub(menu_identity){

            item(menu_identity_peerid){
                val id = Daemon.config.obj("Identity").string("PeerID")!!
                alert(id, getString(title_peerid)){
                    closeButton()
                    positiveButton(copy){ clipboard(id) }
                    show()
                }
            }

            item(menu_identity_privatekey){
                val key = Daemon.config.obj("Identity").string("PrivKey")!!
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
                                    UI {
                                        val addresses = IO {
                                            val result = IPFS().dht.findpeer(peer.id)
                                            val responses = result["Responses"] as? List<*>
                                                ?: return@IO emptyList<String>()
                                            val response = responses[0] as Map<*, *>
                                            response["Addrs"] as? List<String> ?: emptyList()
                                        }.filter { it.isNotBlank() }
                                        alert{
                                            title = hash
                                            customView {
                                                verticalLayout {
                                                    padding = dip(16)
                                                    textView("Peer ID")
                                                    textView("${peer.id}", style)
                                                    textView("\nLocal address")
                                                    textView("${peer.address}", style)
                                                    textView("\nDHT addresses")
                                                    textView(addresses.joinToString("\n"), style)
                                                }
                                            }
                                            positiveButton(peer_disconnect){
                                                UIO { Daemon.exec("swarm disconnect ${peer.address}").waitFor() }
                                            }
                                            closeButton()
                                            show()
                                        }
                                    }
                                }
                            }
                        } }
                    }
                    negativeButton(peer_connect){
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
                val addresses = silentIO { Daemon.config.obj("Addresses") }
                alert{
                    title = getString(title_others)
                    customView {
                        verticalLayout {
                            padding = dip(16)
                            mapOf(
                                others_goipfs_version to version,
                                others_api_address to addresses.string("API"),
                                others_gateway_address to addresses.string("Gateway")
                            ).map {
                                linearLayout {
                                    textView(it.key); textView(" "); textView(it.value, style)
                                }
                            }
                        }
                    }
                    okButton { }
                    show()
                }
            }
        }
    }
}