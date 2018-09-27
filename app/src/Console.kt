package fr.rhaz.ipfs.sweet

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.text.format.Formatter
import android.widget.EditText
import android.widget.PopupMenu
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.android.synthetic.main.activity_console.*
import java.io.FileReader

class ConsoleActivity: AppCompatActivity() {

    val ctx = this as Context

    override fun onBackPressed() {}

    val config by lazy{ JsonParser().parse(FileReader(ipfsd.store["config"])).asJsonObject}

    fun config(consumer: (JsonObject) -> Unit){
        consumer(config)
        ipfsd.store["config"].writeBytes(GsonBuilder().setPrettyPrinting().create().toJson(config).toByteArray())
    }

    override fun onCreate(state: Bundle?) = super.onCreate(state).also{

        setContentView(R.layout.activity_console)

        input.setOnEditorActionListener { textview, i, ev ->  true.also {
            val cmd = input.text.toString()
            console.apply {
                text = "${console.text}\n> $cmd"
                post {
                    val y = layout.getLineTop(lineCount) - height
                    if(y > 0) scrollTo(0, y)
                }
            }
            Thread {
                ipfsd.run(cmd).apply {
                    inputStream.bufferedReader().readLines().forEach {
                        runOnUiThread {
                            console.apply {
                                text = "${console.text}\n$it"
                                post {
                                    val y = layout.getLineTop(lineCount) - height
                                    if(y > 0) scrollTo(0, y)
                                }
                            }
                        }
                    }
                    errorStream.bufferedReader().readLines().forEach {
                        runOnUiThread {
                            console.apply {
                                text = "${console.text}\n$it"
                                post {
                                    val y = layout.getLineTop(lineCount) - height
                                    if(y > 0) scrollTo(0, y)
                                }
                            }
                        }
                    }
                }
            }.start()
            input.text.clear()
        }}

        val notimpl = {AlertDialog.Builder(ctx).setMessage("This feature is not yet implemented. Sorry").show(); true}

        actionbtn.setOnClickListener { btn ->
            PopupMenu(ctx, btn).apply {
                menu.apply {
                    add(str(R.string.menu_add_file)).setOnMenuItemClickListener {
                        true.also{
                            try {
                                Intent(ACTION_GET_CONTENT).apply {
                                    type = "*/*"
                                    startActivityForResult(createChooser(this, str(R.string.title_add_file)), 1)
                                }
                            } catch (e: ActivityNotFoundException) {
                                Snackbar.make(btn, "Unavailable", Snackbar.LENGTH_LONG).show()
                            }
                        }
                    }

                    add(str(R.string.menu_add_folder)).setOnMenuItemClickListener{notimpl()}

                    add(str(R.string.menu_add_text)).setOnMenuItemClickListener{notimpl()}

                    add(str(R.string.menu_garbage_collect)).setOnMenuItemClickListener { true.also{
                        Thread {
                            val gc = ipfs.repo.gc()
                            runOnUiThread {
                                AlertDialog.Builder(ctx)
                                    .setMessage(str(R.string.garbage_collected)).show()
                            }
                        }.start()
                    }}

                    add(str(R.string.menu_pins)).setOnMenuItemClickListener{notimpl()}
                    add(str(R.string.menu_keys)).setOnMenuItemClickListener{notimpl()}
                    add(str(R.string.menu_pubsub)).setOnMenuItemClickListener{notimpl()}
                    addSubMenu(str(R.string.menu_swarm)).apply {
                        add(str(R.string.menu_swarm_connect)).setOnMenuItemClickListener{notimpl()}
                        add(str(R.string.menu_swarm_disconnect)).setOnMenuItemClickListener{notimpl()}
                    }
                    addSubMenu(str(R.string.menu_dht)).apply {
                        add(str(R.string.menu_dht_findpeer)).setOnMenuItemClickListener{notimpl()}
                        add(str(R.string.menu_dht_findprovs)).setOnMenuItemClickListener{notimpl()}
                        add(str(R.string.menu_dht_query)).setOnMenuItemClickListener{notimpl()}
                    }
                }
            }.show()
        }

        infobtn.setOnClickListener {
            PopupMenu(ctx, it).apply {
                menu.apply {
                    addSubMenu(str(R.string.menu_identity)).apply {
                        add(str(R.string.menu_identity_peerid)).setOnMenuItemClickListener{
                            val id = config.getAsJsonObject("Identity").getAsJsonPrimitive("PeerID").asString
                            AlertDialog.Builder(ctx).apply {
                                setTitle(str(R.string.title_peerid))
                                setMessage(id)
                                setPositiveButton(str(R.string.copy)){ d, _ -> }
                                setNeutralButton(str(R.string.close)){ d, _ -> }
                            }.show().apply {
                                getButton(AlertDialog.BUTTON_POSITIVE)
                                    .setOnClickListener { clipboard(id) }
                            }; true
                        }
                        add(str(R.string.menu_identity_privatekey)).setOnMenuItemClickListener{
                            val key = config.getAsJsonObject("Identity").getAsJsonPrimitive("PrivKey").asString
                            AlertDialog.Builder(ctx).apply {
                                setTitle(str(R.string.title_privatekey))
                                setMessage(key)
                                setPositiveButton(str(R.string.copy)){ d, _ -> }
                                setNeutralButton(str(R.string.close)){ d, _ -> }
                            }.show().apply {
                                getButton(AlertDialog.BUTTON_POSITIVE)
                                    .setOnClickListener { clipboard(key) }
                            }; true
                        }
                    }
                    add(getString(R.string.menu_peers)).setOnMenuItemClickListener{notimpl()}
                    add(getString(R.string.menu_others)).setOnMenuItemClickListener{
                        async(60, {ipfs.version()},
                            {
                                val addresses = config.getAsJsonObject("Addresses")
                                AlertDialog.Builder(ctx).apply {
                                    setTitle(getString(R.string.title_others))
                                    setMessage("""
                                        ${str(R.string.others_goipfs_version)}: $it
                                        ${str(R.string.others_api_address)}: ${addresses.getAsJsonPrimitive("API").asString}
                                        ${str(R.string.others_gateway_address)}: ${addresses.getAsJsonPrimitive("Gateway").asString}
                                    """.trimIndent())
                                    setNeutralButton(getString(R.string.close)){ d, _ -> }
                                }.show();
                            },
                            {
                                val addresses = config.getAsJsonObject("Addresses")
                                AlertDialog.Builder(ctx).apply {
                                    setTitle(getString(R.string.title_others))
                                    setMessage("""
                                        ${str(R.string.others_goipfs_version)}: ${str(R.string.others_goipfs_version_unknown)}
                                        ${str(R.string.others_api_address)}: ${addresses.getAsJsonPrimitive("API").asString}
                                        ${str(R.string.others_gateway_address)}: ${addresses.getAsJsonPrimitive("Gateway").asString}
                                    """.trimIndent())
                                }.show();
                            }
                        ); true
                    }
                }
            }.show()
        }

        configbtn.setOnClickListener {
            PopupMenu(ctx, it).apply {
                menu.apply {
                    add(getString(R.string.menu_bootstrap)).setOnMenuItemClickListener{notimpl()}
                    addSubMenu(getString(R.string.menu_gateway)).apply {
                        add(getString(R.string.menu_gateway_writable)).apply {
                            isCheckable = true
                            isChecked = false
                        }.setOnMenuItemClickListener{notimpl()}
                    }
                    addSubMenu(getString(R.string.menu_api)).apply {
                        addSubMenu(getString(R.string.menu_api_http_headers)).apply {
                            add(getString(R.string.menu_api_http_headers_addorigin)).setOnMenuItemClickListener {
                                AlertDialog.Builder(ctx).apply {
                                    setTitle(getString(R.string.title_add_api_origin))
                                    fun action(origin: String){
                                        config{
                                            it.getAsJsonObject("API").getAsJsonObject("HTTPHeaders").apply {
                                                if(!has("Access-Control-Allow-Origin")) {
                                                    add("Access-Control-Allow-Origin", JsonArray())
                                                    getAsJsonArray("Access-Control-Allow-Origin")
                                                        .add("http://localhost:3000")
                                                }
                                                getAsJsonArray("Access-Control-Allow-Origin").add(origin)
                                            }
                                        }
                                        dismiss()
                                    }

                                    val input = EditText(ctx).also{setView(it)}.apply {
                                        hint = "http://..."
                                        setOnEditorActionListener{tv, i, keyEvent -> action(text.toString()); true}
                                    }
                                    setPositiveButton(getString(R.string.apply)){ d, _ -> action(input.text.toString()) }
                                    setNegativeButton(getString(R.string.cancel)){ d, _ -> d.cancel()}
                                }.show()
                                true
                            }
                        }
                    }
                    addSubMenu(getString(R.string.menu_reprovider)).apply {
                        add(getString(R.string.menu_reprovider_interval)).setOnMenuItemClickListener{notimpl()}
                        add(getString(R.string.menu_reprovider_strategy)).setOnMenuItemClickListener{notimpl()}
                    }
                    addSubMenu(getString(R.string.menu_experimental)).apply {
                        add(getString(R.string.menu_experimental_filestore)).apply {
                            isCheckable = true
                            isChecked = false
                        }.setOnMenuItemClickListener{notimpl()}
                        add(getString(R.string.menu_experimental_urlstore)).apply {
                            isCheckable = true
                            isChecked = false
                        }.setOnMenuItemClickListener{notimpl()}
                        add(getString(R.string.menu_experimental_sharding)).apply {
                            isCheckable = true
                            isChecked = false
                        }.setOnMenuItemClickListener{notimpl()}
                        add(getString(R.string.menu_experimental_libp2p_stream_mounting)).apply {
                            isCheckable = true
                            isChecked = false
                        }.setOnMenuItemClickListener{notimpl()}
                    }
                }
            }.show()
        }

    }

    override fun onActivityResult(req: Int, res: Int, rdata: Intent?){
        super.onActivityResult(req, res, rdata)
        if(res != RESULT_OK) return;
        when(req){
            1 -> Intent(ctx, ShareActivity::class.java).apply {
                data = rdata?.data ?: return
                action = ACTION_SEND
                startActivity(this)
            }
        }
    }

    fun Long.format() = Formatter.formatFileSize(ctx, this)

}
