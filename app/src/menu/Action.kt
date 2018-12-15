package fr.rhaz.ipfs.sweet.menu

import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.net.Uri
import android.text.SpannableString
import android.view.Menu
import android.view.View
import android.widget.PopupMenu
import fr.rhaz.ipfs.sweet.*
import fr.rhaz.ipfs.sweet.R.string.*
import kotlinx.android.synthetic.main.activity_console.*
import org.jetbrains.anko.*
import fr.rhaz.ipfs.sweet.UI


fun Context.popupMenu(anchor: View, builder: Menu.() -> Unit)
    = PopupMenu(ctx, anchor).apply { menu.apply(builder) }.show()

fun ConsoleActivity.actionMenu() = actionbtn.onClick{
    popupMenu(it){

        item(menu_add_file){
            val intent = Intent(ACTION_GET_CONTENT).setType("*/*")
            val chooser = createChooser(intent, getString(title_add_file))
            startActivityForResult(chooser, 1)
        }

        item(menu_add_text){
            alert{
                title = getString(title_add_text)
                isCancelable = false
                val input = inputView()
                cancelButton{}
                okButton {
                    intent<ShareActivity>().apply {
                        action = ACTION_SEND
                        type = "text/plain"
                        putExtra(EXTRA_TEXT, input.value)
                        startActivity(this)
                    }
                }
                show()
            }
        }

        item(menu_action_open_clipboard){ UI {
            val input = clipboardManager.primaryClip.getItemAt(0)
            val uri = input.uri ?: Uri.parse(input.text.toString())
            val res = IPXSResource(uri)
            if(!res.valid) throw Exception(browser_not_ipxs)
            intent<ShareActivity>().apply {
                action = ACTION_SEND
                putExtra("hash", res.hash!!.toBase58())
                putExtra("scheme", res.type)
                startActivity(this)
            }
        } }

        item(menu_action_open_file){
            val intent = Intent(ACTION_GET_CONTENT).setType("*/*")
            val chooser = createChooser(intent, getString(menu_action_open_file))
            startActivityForResult(chooser, 2)
        }

        item(menu_garbage_collect){
            UI {
                IO { IPFS().repo.gc() }
                alert(garbage_collected, menu_garbage_collect){ okButton{} }.show()
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
                    positiveButton(_new){
                        alert{
                            title = getString(title_add_key)
                            val input = inputView{hint = getString(name)}
                            cancelButton {  }
                            okButton {
                                UI {
                                    IO { Daemon.exec("key gen --type=rsa --size=2048 ${input.value}").waitFor() }
                                }
                            }
                            show()
                        }
                    }
                    customView {
                        scrollView { verticalLayout {
                            padding = dip(24)
                            keys.forEach { key ->
                                val hash = key.id.toBase58()
                                textView(key.name){
                                    textSize = 18f
                                    onClick {
                                        alert{
                                            title = hash
                                            val input = inputView{
                                                setText(key.name)
                                                if(key.name == "self") isEnabled = false
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
                                            negativeButton(open){
                                                intent<ShareActivity>().apply {
                                                    action = ACTION_VIEW
                                                    putExtra("hash", key.id.toBase58())
                                                    putExtra("scheme", "ipns")
                                                    putExtra("name", key.name)
                                                    startActivity(this)
                                                }
                                            }
                                            okButton {
                                                if(key.name != input.value)
                                                UIO { IPFS().key.rename(key.name, input.value) }
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
    }
}