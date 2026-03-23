package ai.mlc.mlcchat

import android.app.Application
import android.util.Log

class MLCChatApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // replication
        val oldHandler = Thread.getDefaultUncaughtExceptionHandler()

        // to re-define
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e("CrashGuard", "App crashed on thread=${thread.name}", throwable)

                // restore brightness
                BrightnessGuard.restoreBrightnessMax()

                // TODO
                // dvfs unset, possible?

                // wait
                Thread.sleep(150)
            } catch ( _: Throwable ) {
            } finally {
                // return the old handler
                oldHandler?.uncaughtException(thread, throwable) ?: run {
                    android.os.Process.killProcess(android.os.Process.myPid())
                    kotlin.system.exitProcess(10)
                }
            }
        }
    }
}