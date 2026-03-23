package ai.mlc.mlcchat

import android.os.Build
import android.util.Log
import java.util.concurrent.TimeUnit
import kotlin.math.log

object BrightnessGuard {
    private const val TAG = "BrightnessGuard"
    data class BrightnessPath(
        val brightnessNode: String,
        val maxBrightnessNode: String,
        val maxPassiveBrightness: Int
    )

    private val S24Path = BrightnessPath(
        brightnessNode = "/sys/class/backlight/panel/brightness",
        maxBrightnessNode = "/sys/class/backlight/panel/max_brightness", // unused
        maxPassiveBrightness = 2550
    )

    private val brightnessPathMap = mapOf(
        "Pixel9" to BrightnessPath(
            brightnessNode = "/sys/class/backlight/panel0-backlight/brightness",
            maxBrightnessNode = "/sys/class/backlight/panel0-backlight/max_brightness",
            maxPassiveBrightness = -1
        ),
        "S24" to S24Path,
        "S25" to S24Path
    )

    @Volatile
    private var restored = false

    private fun resolveDeviceKey(): String? {
        val model = Build.MODEL ?: ""
        val device = Build.DEVICE ?: ""
        val product = Build.PRODUCT ?: ""

        Log.i(TAG, "model=$model")
        Log.i(TAG, "device=$device")
        Log.i(TAG, "product=$product")

        return when {
            model.contains("Pixel 9", ignoreCase = true) ||
            device.contains("tokay", ignoreCase = true)  ||
            product.contains("tokay", ignoreCase = true) -> "Pixel9"

            model.contains("SM-S921", ignoreCase = true) || // it allows SM-S921N, etc.
            model.contains("S24", ignoreCase = true) -> "S24"

            model.contains("SM-S931", ignoreCase = true) ||
            model.contains("S25", ignoreCase = true) -> "S25"

            else -> null
        }
    }

    private fun getPath(): BrightnessPath? {
        val key = resolveDeviceKey()
        if (key == null) {
            Log.e(TAG, "Unsupported device")
            return null
        }
        return brightnessPathMap[key]
    }

    fun restoreBrightnessMax() : Boolean{
        val path = getPath() ?: return false
        val Max = path.maxPassiveBrightness

        if (Max == -1) {
            val cmd = """
                        max=$(cat ${path.maxBrightnessNode} 2>/dev/null)
                        if [ -z "${'$'}max" ]; then exit 1; fi
                        echo ${'$'}max > ${path.brightnessNode}
                      """.trimIndent()

            return runRootCommand(cmd)
        } else {
            val cmd = "echo $Max > ${path.brightnessNode}"
            return runRootCommand(cmd)
        }
    }

    fun setBrightnessMin(): Boolean {
        val Min = 0
        val path = getPath() ?: return false

        val cmd = "echo $Min > ${path.brightnessNode}"
        return runRootCommand(cmd)
    }

    // require root permission
    private fun runRootCommand(shellCommand: String): Boolean {
        return try {
            val proc = ProcessBuilder("su", "-c", shellCommand).redirectErrorStream(true).start()
            val finished = proc.waitFor(200, TimeUnit.MILLISECONDS)

            if (!finished) {
                proc.destroy()
                Log.e(TAG, "[runRootCommand] Timeout")
                false
            } else {
                val exitCode = proc.exitValue()
                if (exitCode != 0) {Log.e(TAG, "[runRootCommand] failed: exit code = $exitCode")}
                exitCode == 0
            }
        } catch (t: Throwable) {
            Log.e(TAG, "[runRootCommand] Error")
            false
        }
    }
}