package fr.rhaz.ipfs.sweet.activities

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.format.Formatter
import android.widget.PopupMenu
import fr.rhaz.ipfs.sweet.App
import fr.rhaz.ipfs.sweet.IPFSDaemon
import fr.rhaz.ipfs.sweet.R
import io.ipfs.kotlin.IPFS
import kotlinx.android.synthetic.main.activity_details.*
import javax.inject.Inject

class DetailsActivity: AppCompatActivity() {

    val ctx = this as Context

    @Inject
    lateinit var ipfs: IPFS

    val ipfsd = IPFSDaemon(this)

    override fun onBackPressed() {}
    override fun onResume() = super.onResume()

    override fun onCreate(state: Bundle?) = super.onCreate(state).also{

        App.component()?.inject(this)
        setContentView(R.layout.activity_details)

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

        actionbtn.setOnClickListener { btn ->
            PopupMenu(ctx, btn).apply {
                menu.apply {
                    add("Add file...").setOnMenuItemClickListener { true.also{
                        try {
                            Intent(ACTION_OPEN_DOCUMENT).apply {
                                addCategory(CATEGORY_OPENABLE)
                                type = "*/*"
                                startActivityForResult(this, 1)
                            }
                        } catch (e: ActivityNotFoundException) {
                            Snackbar.make(btn, "Unavailable", Snackbar.LENGTH_LONG).show()
                        }
                    }}
                    add("Add text...").setOnMenuItemClickListener { true.also{
                        Intent(ctx, AddIPFSContentActivity::class.java).apply {
                            action = ACTION_SEND
                            type = "text/plain"
                            putExtra(EXTRA_TEXT, input.text.toString())
                            startActivity(this)
                        }
                    }}

                    add("Garbage collect").setOnMenuItemClickListener { true.also{
                        Thread {
                            val gc = ipfs.repo.gc()
                            runOnUiThread {
                                AlertDialog.Builder(ctx)
                                    .setMessage("Collected ${gc.size} objects").show()
                            }
                        }.start()
                    }}

                    add("Pins management")
                    add("Keys management")
                    add("Pub/Sub")
                    addSubMenu("Swarm").apply {
                        add("Connect to...")
                        add("Disconnect from...")
                    }
                    addSubMenu("DHT").apply {
                        add("Find peer...")
                        add("Find provs...")
                        add("Query...")
                    }
                }
            }.show()
        }

        infobtn.setOnClickListener {
            PopupMenu(ctx, it).apply {
                menu.apply {
                    addSubMenu("Identity").apply {
                        add("Peer ID")
                        add("Private Key")
                    }
                    add("Peers")
                    add("Others")
                }
            }.show()
        }

        configbtn.setOnClickListener {
            PopupMenu(ctx, it).apply {
                menu.apply {
                    add("Bootstrap")
                    addSubMenu("Gateway").apply {
                        add("Writable").apply {
                            isCheckable = true
                            isChecked = false
                        }
                    }
                    addSubMenu("API").apply {
                        addSubMenu("HTTP Headers").apply {
                            add("Add origin...")
                        }
                    }
                    addSubMenu("Reprovider").apply {
                        add("Interval")
                        add("Strategy")
                    }
                    addSubMenu("Experimental").apply {
                        add("Filestore").apply {
                            isCheckable = true
                            isChecked = false
                        }
                        add("URL Store").apply {
                            isCheckable = true
                            isChecked = false
                        }
                        add("Sharding").apply {
                            isCheckable = true
                            isChecked = false
                        }
                        add("LibP2P Stream Mounting").apply {
                            isCheckable = true
                            isChecked = false
                        }
                    }
                }
            }.show()
        }

    }

    override fun onActivityResult(req: Int, res: Int, resdata: Intent?) =
        super.onActivityResult(req, res, resdata).also {
            if(req == 1)
            if(res == RESULT_OK)
            if(resdata != null)
            Intent(ctx, AddIPFSContentActivity::class.java).apply {
                action = ACTION_SEND
                data = resdata.data
                startActivity(this)
            }
        }

    fun Long.format() = Formatter.formatFileSize(ctx, this)

}
