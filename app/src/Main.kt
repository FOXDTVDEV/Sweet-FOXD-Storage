package fr.rhaz.ipfs.sweet

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.support.v4.app.ActivityCompat.requestPermissions
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.animation.AlphaAnimation
import android.view.animation.DecelerateInterpolator
import fr.rhaz.ipfs.sweet.R.layout.activity_main
import fr.rhaz.ipfs.sweet.R.string.*
import fr.rhaz.ipfs.sweet.utils.obj
import fr.rhaz.ipfs.sweet.utils.string
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.*
import org.ligi.tracedroid.sending.TraceDroidEmailSender.sendStackTraces

class MainActivity: ScopedActivity() {

    override fun onBackPressed() {}
    override fun onResume() {
        super.onResume()
        checkAPI(::redirect){}
    }

    fun ready() { checkAPI(::redirect){show()} }
    fun redirect() = startActivityNoAnimation<ConsoleActivity>()

    override fun onCreate(state: Bundle?){
        super.onCreate(state)
        sendStackTraces("hazae41@gmail.com", this)
        requestPermissions(this, permissions, 1)
    }

    override fun onRequestPermissionsResult(req: Int, perms: Array<out String>, grants: IntArray) {
        if(grants.all { it == PERMISSION_GRANTED })
            ready()
    }

    var initArgs = ""
    var startArgs = "--enable-pubsub-experiment --enable-namesys-pubsub"

    fun show() = UI {
        Daemon.install()

        if(Daemon.initialized){
            val conf = Daemon.config.obj("Sweet").string("Args")
            if(conf != null) startArgs = conf
        }

        setContentView(activity_main)

        satellite.onClick {settings()}
        rocket.onClick {start()}
    }

    fun start() = UI {
        message.fadeOut { duration = 500 }
        satellite.fadeOut { duration = 500 }
        rocket.shake{
            duration = 500
            then{
                message.text = getString(main_progress)
                message.fadeIn { duration = 1500 }
                progress.fadeIn { duration = 2000 }
                rocket.moveY(-300f){duration = 500 }
                rocket.moveX(600f){duration = 500 }
            }
        }

        Daemon.init(initArgs)
        Daemon.start(startArgs)
        startActivity<ConsoleActivity>()
    }

    fun settings() = alert{
        title = getString(settings)
        customView {
            verticalLayout {
                padding = dip(16)
                textView(main_settings_init_args)
                val initInput = editText(""){
                    if(Daemon.initialized) isEnabled = false
                }
                textView(main_settings_start_args)
                val startInput = editText(startArgs)
                okButton {
                    if(initInput.value.isNotBlank())
                        initArgs = initInput.value.replace("\n", " ")
                    if(startInput.value.isNotBlank())
                        startArgs = startInput.value.replace("\n", " ")
                }
            }
        }
        neutralPressed(main_settings_delete_storage){
            alert{
                title = getString(main_settings_delete_storage)
                message = getString(main_settings_delete_storage_confirmation)
                no {  }
                yes { Daemon.store.deleteRecursively() }
                show()
            }
        }
        cancelButton {  }
        show()
    }

    fun View.shake(builder: ObjectAnimator.() -> Unit){
        ObjectAnimator
            .ofFloat(this, "translationX", 0f, 25f, -25f, 25f, -25f,15f, -15f, 6f, -6f, 0f)
            .apply(builder).start()
    }

    fun View.moveY(value: Float, builder: ObjectAnimator.() -> Unit){
        ObjectAnimator.ofFloat(this, "y", value).apply(builder).start()
    }

    fun View.moveX(value: Float, builder: ObjectAnimator.() -> Unit){
        ObjectAnimator.ofFloat(this, "translationX", value).apply(builder).start()
    }

    fun View.fadeOut(builder: ObjectAnimator.() -> Unit){
        ObjectAnimator.ofFloat(this, "alpha", 1f, 0f).apply(builder).start()
    }

    fun View.fadeIn(builder: ObjectAnimator.() -> Unit){
        ObjectAnimator.ofFloat(this, "alpha", 0f, 1f).apply(builder).start()
    }

    fun Animator.then(action: () -> Unit) = this.also{
        addListener(object: Animator.AnimatorListener{
            override fun onAnimationStart(p0: Animator?){}
            override fun onAnimationCancel(p0: Animator?){}
            override fun onAnimationRepeat(p0: Animator?){}
            override fun onAnimationEnd(p0: Animator?) {action()}
        })
    }
}