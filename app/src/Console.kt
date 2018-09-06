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
                    add("Add file...").setOnMenuItemClickListener {
                        true.also{
                            try {
                                Intent(ACTION_GET_CONTENT).apply {
                                    //addCategory(CATEGORY_OPENABLE)
                                    type = "*/*"
                                    startActivityForResult(createChooser(this, "Add file to IPFS"), 1)
                                }
                            } catch (e: ActivityNotFoundException) {
                                Snackbar.make(btn, "Unavailable", Snackbar.LENGTH_LONG).show()
                            }
                        }
                    }

                    add("Add folder...").setOnMenuItemClickListener{notimpl()}

                    add("Add text...").setOnMenuItemClickListener{notimpl()}

                    add("Garbage collect").setOnMenuItemClickListener { true.also{
                        Thread {
                            val gc = ipfs.repo.gc()
                            runOnUiThread {
                                AlertDialog.Builder(ctx)
                                    .setMessage("Collected ${gc.size} objects").show()
                            }
                        }.start()
                    }}

                    add("Pins management").setOnMenuItemClickListener{notimpl()}
                    add("Keys management").setOnMenuItemClickListener{notimpl()}
                    add("Pub/Sub").setOnMenuItemClickListener{notimpl()}
                    addSubMenu("Swarm").apply {
                        add("Connect to...").setOnMenuItemClickListener{notimpl()}
                        add("Disconnect from...").setOnMenuItemClickListener{notimpl()}
                    }
                    addSubMenu("DHT").apply {
                        add("Find peer...").setOnMenuItemClickListener{notimpl()}
                        add("Find provs...").setOnMenuItemClickListener{notimpl()}
                        add("Query...").setOnMenuItemClickListener{notimpl()}
                    }
                }
            }.show()
        }

        infobtn.setOnClickListener {
            PopupMenu(ctx, it).apply {
                menu.apply {
                    addSubMenu("Identity").apply {
                        add("Peer ID").setOnMenuItemClickListener{notimpl()}
                        add("Private Key").setOnMenuItemClickListener{notimpl()}
                    }
                    add("Peers").setOnMenuItemClickListener{notimpl()}
                    add("Others").setOnMenuItemClickListener{notimpl()}
                }
            }.show()
        }

        configbtn.setOnClickListener {
            PopupMenu(ctx, it).apply {
                menu.apply {
                    add("Bootstrap").setOnMenuItemClickListener{notimpl()}
                    addSubMenu("Gateway").apply {
                        add("Writable").apply {
                            isCheckable = true
                            isChecked = false
                        }.setOnMenuItemClickListener{notimpl()}
                    }
                    addSubMenu("API").apply {
                        addSubMenu("HTTP Headers").apply {
                            add("Add origin...").setOnMenuItemClickListener {
                                AlertDialog.Builder(ctx).apply {
                                    setTitle("Add API origin")
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
                                    setPositiveButton("Apply"){ d, _ -> action(input.text.toString()) }
                                    setNegativeButton("Cancel"){ d, _ -> d.cancel()}
                                }.show()
                                true
                            }
                        }
                    }
                    addSubMenu("Reprovider").apply {
                        add("Interval").setOnMenuItemClickListener{notimpl()}
                        add("Strategy").setOnMenuItemClickListener{notimpl()}
                    }
                    addSubMenu("Experimental").apply {
                        add("Filestore").apply {
                            isCheckable = true
                            isChecked = false
                        }.setOnMenuItemClickListener{notimpl()}
                        add("URL Store").apply {
                            isCheckable = true
                            isChecked = false
                        }.setOnMenuItemClickListener{notimpl()}
                        add("Sharding").apply {
                            isCheckable = true
                            isChecked = false
                        }.setOnMenuItemClickListener{notimpl()}
                        add("LibP2P Stream Mounting").apply {
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
