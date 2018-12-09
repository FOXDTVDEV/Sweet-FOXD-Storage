package fr.rhaz.ipfs.sweet.menu

import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.text.InputType.TYPE_CLASS_TEXT
import android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
import android.view.Menu
import android.view.View
import android.widget.EditText
import android.widget.PopupMenu
import fr.rhaz.ipfs.sweet.*
import fr.rhaz.ipfs.sweet.R.string.*
import kotlinx.android.synthetic.main.activity_console.*
import kotlinx.coroutines.async

fun Context.popupMenu(anchor: View, builder: Menu.() -> Unit)
    = PopupMenu(ctx, anchor).apply { menu.apply(builder) }.show()

fun ConsoleActivity.actionMenu() = actionbtn.onClick {
    popupMenu(it){

        add(menu_add_file).onClick {
            Intent(ACTION_GET_CONTENT).also {
                it.type = "*/*"
                val chooser = createChooser(it, getString(title_add_file))
                startActivityForResult(chooser, 1)
            }
        }

        add(menu_add_folder).onClick{
            if (VERSION.SDK_INT < VERSION_CODES.LOLLIPOP)
                alertDialog(title_add_folder, "Android version not supported")
            else Intent(ACTION_OPEN_DOCUMENT_TREE).also {
                it.type = "*/*"
                val chooser = createChooser(it, getString(title_add_folder))
                startActivityForResult(chooser, 2)
            }
        }

        add(menu_add_text).onClick{
            alertDialog(title_add_text){
                val txt = EditText(ctx).apply{
                    inputType = TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_PASSWORD
                    setView(this)
                }
                setNegativeButton(getString(cancel)){ d, _ -> }
                setPositiveButton(apply){ d, _ ->
                    intent<ShareActivity>().apply {
                        type = "text/plain"
                        putExtra(EXTRA_TEXT, txt.value)
                    }
                }
            }
        }

        add(menu_garbage_collect).onClick{
            catchUI {
                async { IPFS().repo.gc() }.await()
                alertDialog(garbage_collected)
            }
        }

        add(menu_pins).onClick(::notimpl)
        add(menu_keys).onClick(::notimpl)
        add(menu_pubsub).onClick(::notimpl)

        addSubMenu(menu_swarm).apply {
            add(menu_swarm_connect).onClick(::notimpl)
            add(menu_swarm_disconnect).onClick(::notimpl)
        }

        addSubMenu(menu_dht).apply {
            add(menu_dht_findpeer).onClick(::notimpl)
            add(menu_dht_findprovs).onClick(::notimpl)
            add(menu_dht_query).onClick(::notimpl)
        }
    }
}