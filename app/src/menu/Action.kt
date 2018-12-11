package fr.rhaz.ipfs.sweet.menu

import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.view.Menu
import android.view.View
import android.widget.EditText
import android.widget.PopupMenu
import fr.rhaz.ipfs.sweet.*
import fr.rhaz.ipfs.sweet.R.string.*
import kotlinx.android.synthetic.main.activity_console.*
import org.jetbrains.anko.*
import fr.rhaz.ipfs.sweet.UI
import java.util.*

fun Context.popupMenu(anchor: View, builder: Menu.() -> Unit)
    = PopupMenu(ctx, anchor).apply { menu.apply(builder) }.show()

fun ConsoleActivity.actionMenu() = actionbtn.onClick{
    if(it != null) popupMenu(it){

        item(menu_add_file){
            Intent(ACTION_GET_CONTENT).apply {
                type = "*/*"
                val chooser = createChooser(this, getString(title_add_file))
                startActivityForResult(chooser, 1)
            }
        }

        item(menu_add_text){
            alert{
                title = getString(title_add_text)
                isCancelable = false
                lateinit var txt: EditText
                customView {
                    verticalLayout {
                        txt = editText()
                        padding = dip(16)
                    }
                }
                cancelButton{}
                okButton {
                    intent<ShareActivity>().apply {
                        action = ACTION_SEND
                        type = "text/plain"
                        putExtra(EXTRA_TEXT, txt.value)
                        startActivity(this)
                    }
                }
                show()
            }
        }

        item(menu_garbage_collect){
            UI {
                IO { IPFS().repo.gc() }
                alert(garbage_collected){ okButton{} }.show()
            }
        }

        item(menu_pins){
            UI{
                val pins = Daemon.pins()
                alert{
                    title = getString(title_action_pins, pins.size)
                    customView {
                        scrollView { verticalLayout {
                            padding = dip(16)
                            pins.forEach { pin ->
                                val hash = pin.toBase58()
                                textView(hash).onClick {
                                    alert(ask_open_hash){
                                        title = hash
                                        val intent = intent<ShareActivity>{
                                            action = ACTION_SEND
                                            putExtra("hash", hash)
                                        }
                                        yes { startActivity(intent) }
                                        no {}
                                        show()
                                    }
                                }
                            }
                        } }
                    }
                    okButton {}
                    show()
                }
            }
        }

        item(menu_keys){
            UI {
                val keys = IO { IPFS().key.list() }
                alert{
                    title = getString(menu_keys)
                    closeButton()
                    positiveButton(btn_add_key){
                        alert{
                            title = getString(btn_add_key)
                            lateinit var text: EditText
                            customView {
                                verticalLayout {
                                    padding = dip(16)
                                    text = editText()
                                }
                            }
                            cancelButton {  }
                            okButton {
                                UI {
                                    IO { Daemon.exec("key gen --type=rsa --size=2048 ${text.value}").waitFor() }
                                }
                            }
                            show()
                        }
                    }
                    customView {
                        scrollView { verticalLayout {
                            padding = dip(16)
                            keys.forEach { key ->
                                val hash = key.id.toBase58()
                                textView(key.name){
                                    textSize = 20f
                                    onClick {
                                        alert{
                                            title = hash
                                            lateinit var text: EditText
                                            customView {
                                                verticalLayout {
                                                    padding = dip(16)
                                                    text = editText(key.name)
                                                    if(key.name == "self")
                                                    text.isEnabled = false
                                                }
                                            }
                                            if(key.name != "self")
                                            neutralPressed(delete){
                                                alert(key_delete_confirm){
                                                    yes {
                                                        UI { IO { IPFS().key.rm(key.name) } }
                                                    }
                                                    no{}
                                                    show()
                                                }
                                            }
                                            negativeButton(copy){ clipboard(hash) }
                                            okButton {
                                                if(key.name != text.value)
                                                UI { IO { IPFS().key.rename(key.name, text.value)} }
                                            }
                                            show()
                                        }
                                    }
                                }
                            }
                        } }
                    }
                    show()
                }
            }
        }

        item(menu_pubsub, ::notimpl)

        sub(menu_dht) {
            item(menu_dht_findpeer, ::notimpl)
            item(menu_dht_findprovs, ::notimpl)
            item(menu_dht_query, ::notimpl)
        }
    }
}