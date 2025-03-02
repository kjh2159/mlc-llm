package ai.mlc.mlcchat

import ai.mlc.mlcllm.OpenAIProtocol
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import kotlin.math.log

open class Device() {
    var device: String = ""
    var clusterIndices: List<Int> = emptyList()
}

class DVFS() : Device() {
    // scaling CPU frequency
    val cpufreq: Map<String, Map<Int, List<Int>>> = mapOf(
        // path: /sys/devices/system/cpu/cpufreq/policy0/scaling_available_frequencies (policy can be 0, 4, 7)
        "S22_Ultra" to mapOf(
            // default: min - max
            0 to listOf( 307200, 403200, 518400, 614400, 729600, 844800, 960000, 1075200, 1171200, 1267200, 1363200, 1478400, 1574400, 1689600, 1785600 ), // 15
            4 to listOf( 633600, 768000, 883200, 998400, 1113600, 1209600, 1324800, 1440000, 1555200, 1651200, 1766400, 1881600, 1996800, 2112000, 2227200, 2342400, 2419200), // 17
            7 to listOf( 806400, 940800, 1056000, 1171200, 1286400, 1401600, 1497600, 1612800, 1728000, 1843200, 1958400, 2054400, 2169600, 2284800, 2400000, 2515200, 2630400, 2726400, 2822400, 2841600 ) // 20
        ),

        // path: /sys/devices/system/cpu/cpufreq/policy0/scaling_available_frequencies (policy can be 0, 4, 7, 9)
        "S24" to mapOf(
            // default: min - max
            0 to listOf( 400000, 576000, 672000, 768000, 864000, 960000, 1056000, 1152000, 1248000, 1344000, 1440000, 1536000, 1632000, 1728000, 1824000, 1920000, 1959000 ), // 17
            4 to listOf( 672000, 768000, 864000, 960000, 1056000, 1152000, 1248000, 1344000, 1440000, 1536000, 1632000, 1728000, 1824000, 1920000, 2016000, 2112000, 2208000, 2304000, 2400000, 2496000, 2592000), // 21
            7 to listOf( 672000, 768000, 864000, 960000, 1056000, 1152000, 1248000, 1344000, 1440000, 1536000, 1632000, 1728000, 1824000, 1920000, 2016000, 2112000, 2208000, 2304000, 2400000, 2496000, 2592000, 2688000, 2784000, 2880000, 2900000 ), // 25
            9 to listOf( 672000, 768000, 864000, 960000, 1056000, 1152000, 1248000, 1344000, 1440000, 1536000, 1632000, 1728000, 1824000, 1920000, 2016000, 2112000, 2208000, 2304000, 2400000, 2496000, 2592000, 2688000, 2784000, 2880000, 2976000, 3072000, 3207000 ) // 27
        ),

        // path: /sys/devices/system/cpu/cpufreq/policy0/scaling_available_frequencies (policy can be 0, 4, 7)
        "Pixel9" to mapOf(
            0 to listOf( 820000, 955000, 1098000, 1197000, 1328000, 1425000, 1548000, 1696000, 1849000, 1950000 ),
            4 to listOf( 357000, 578000, 648000, 787000, 910000, 1065000, 1221000, 1328000, 1418000, 1549000, 1795000, 1945000, 2130000, 2245000, 2367000, 2450000, 2600000 ),
            7 to listOf( 700000, 1164000, 1396000, 1557000, 1745000, 1885000, 1999000, 2147000, 2294000, 2363000, 2499000, 2687000, 2802000, 2914000, 2943000, 2970000, 3015000, 3105000 )
        )
    )

    val gpufreq : Map<String, List<Int>> = mapOf(
        "Pixel9" to listOf( 150000, 302000, 337000, 376000, 419000, 467000, 521000, 580000, 649000, 723000, 807000, 850000, 890000, 940000 )
    )

    val ddrfreq : Map<String, List<Int>> = mapOf(
        "S22_Ultra" to listOf( 547000, 768000, 1555000, 1708000, 2092000, 2736000, 3196000 ), // 7 levels
        "S24" to listOf( 421000, 676000, 845000, 1014000, 1352000, 1539000, 1716000, 2028000, 2288000, 2730000, 3172000, 3738000, 4206000 ), // 13 levels
        "Pixel9" to listOf( 421000, 546000, 676000, 845000, 1014000, 1352000, 1539000, 1716000, 2028000, 2288000, 2730000, 3172000, 3744000 ) // 13 levels
    )

    val emptyThermal: Map<String, List<String>> = mapOf("S22_Ultra" to
            listOf("sdr0-pa0",
                "sdr1-pa0",
                "pm8350b_tz",
                "pm8350b-ibat-lvl0",
                "pm8350b-ibat-lvl1",
                "pm8350b-bcl-lvl0",
                "pm8350b-bcl-lvl1",
                "pm8350b-bcl-lvl2",
                "socd",
                "pmr735b_tz"))

    constructor(device: String) : this() {
        this.device = device
        when (device) {
            "S22_Ultra" -> clusterIndices = listOf(0, 4, 7)
            "S24" -> clusterIndices = listOf(0, 4, 7, 9)
            "Pixel9" -> clusterIndices = listOf(0, 4, 7)
        }
    }

    fun setGPUFrequency(freqIndex: Int) {
        // freqIndex: index of GPU frequency

        // path to set GPU clock frequency
        // /sys/devices/platform/1f000000.mali/scaling_min_freq
        // /sys/devices/platform/1f000000.mali/scaling_max_freq

        if (freqIndex >= gpufreq[device]!!.size) {
            return
        }

        val freq = gpufreq[device]!![freqIndex] // available frequencies for the device
        var command = "su -c " +    // make a command
                      "echo $freq > /sys/devices/platform/1f000000.mali/scaling_max_freq" +
                      "echo $freq > /sys/devices/platform/1f000000.mali/scaling_min_freq" +
                      "echo $freq > /sys/devices/platform/1f000000.mali/scaling_max_freq"  // to ensure max_freq set

        // run android kernel command
        val process = Runtime.getRuntime().exec(command)
        process.waitFor()
    }

    fun unsetGPUFrequency() {
        // set gpu frequency as min and max

        val freqs = gpufreq[device] // available frequencies for the device
        val min_freq = freqs?.get(0)
        val max_freq = freqs?.get(freqs.size-1)

        var command = "su -c " +      // make a command
                      "echo $max_freq > /sys/devices/platform/1f000000.mali/scaling_max_freq" +
                      "echo $min_freq > /sys/devices/platform/1f000000.mali/scaling_min_freq" +
                      "echo $max_freq > /sys/devices/platform/1f000000.mali/scaling_max_freq"  // to ensure max_freq set

        // run android kernel command
        val process = Runtime.getRuntime().exec(command)
        process.waitFor()
    }

    fun getClusterFrequencies(idx: Int): List<Int>? {
        // idx = 0 .. 3
        return this.cpufreq[this.device]?.get(idx)
    }

    fun setRAMFrequency(freqIndex: Int){
        // freqIndex: index of frequency to set
        // then, freqIndex 0 -> 547000kHz

        // path to set RAM clock frequency
        // /sys/devices/system/cpu/bus_dcvs/DDR/
        val freqs = ddrfreq[device]
        var command = "su -c "
        if (freqIndex > freqs?.size!!) {  return  }

        // S22 Ultra version (need to check S24)
        val freq = freqs[freqIndex]
        Log.d("CHECKER", "$freq")
        when (device) {
            // Snapdragon 8 Gen 1
            "S22_Ultra" -> command += "echo $freq > /sys/devices/system/cpu/bus_dcvs/DDR/soc:qcom,memlat:ddr:prime/max_freq; " +
                    "echo $freq > /sys/devices/system/cpu/bus_dcvs/DDR/soc:qcom,memlat:ddr:prime/min_freq; " +
                    "echo $freq > /sys/devices/system/cpu/bus_dcvs/DDR/soc:qcom,memlat:ddr:prime-latfloor/max_freq; " +
                    "echo $freq > /sys/devices/system/cpu/bus_dcvs/DDR/soc:qcom,memlat:ddr:prime-latfloor/min_freq; " +
                    "echo $freq > /sys/devices/system/cpu/bus_dcvs/DDR/soc:qcom,memlat:ddr:gold/max_freq; " +
                    "echo $freq > /sys/devices/system/cpu/bus_dcvs/DDR/soc:qcom,memlat:ddr:gold/min_freq; " +
                    "echo $freq > /sys/devices/system/cpu/bus_dcvs/DDR/soc:qcom,memlat:ddr:gold-compute/max_freq; " +
                    "echo $freq > /sys/devices/system/cpu/bus_dcvs/DDR/soc:qcom,memlat:ddr:gold-compute/min_freq; " +
                    "echo $freq > /sys/devices/system/cpu/bus_dcvs/DDR/soc:qcom,memlat:ddr:silver/max_freq; " +
                    "echo $freq > /sys/devices/system/cpu/bus_dcvs/DDR/soc:qcom,memlat:ddr:silver/min_freq; " +
                    "echo $freq > /sys/devices/system/cpu/bus_dcvs/DDR/19091000.qcom,bwmon-ddr/max_freq; " +
                    "echo $freq > /sys/devices/system/cpu/bus_dcvs/DDR/19091000.qcom,bwmon-ddr/min_freq; "

            // Exynos 2400
            "S24" -> command += "echo $freq > /sys/devices/platform/17000010.devfreq_mif/devfreq/17000010.devfreq_mif/scaling_devfreq_min; " +
                    "echo $freq > /sys/devices/platform/17000010.devfreq_mif/devfreq/17000010.devfreq_mif/scaling_devfreq_max; "

            // Google Tensor 4
            "Pixel9" -> command += "echo $freq > /sys/devices/platform/17000010.devfreq_mif/devfreq/17000010.devfreq_mif/min_freq; " +
                    "echo $freq > /sys/devices/platform/17000010.devfreq_mif/devfreq/17000010.devfreq_mif/max_freq; "

        }
        Log.d("CHECKER", command)

        // run android kernel command
        val process = Runtime.getRuntime().exec(command)
        process.waitFor()
    }

    fun unsetRAMFrequency(){
        // freqIndex: index of frequency to set
        // then, freqIndex 0 -> 547000kHz

        // path to set RAM clock frequency
        // /sys/devices/system/cpu/bus_dcvs/DDR/
        val freqs = ddrfreq[device]
        val max_freq = freqs?.get(freqs.size-1)
        val min_freq = freqs?.get(0)

        var command = "su -c "
        when(device) {
            "S22_Ultra" -> command += "echo $max_freq > /sys/devices/system/cpu/bus_dcvs/DDR/soc:qcom,memlat:ddr:prime/max_freq; " +
                    "echo $min_freq > /sys/devices/system/cpu/bus_dcvs/DDR/soc:qcom,memlat:ddr:prime/min_freq; " +
                    "echo $max_freq > /sys/devices/system/cpu/bus_dcvs/DDR/soc:qcom,memlat:ddr:prime-latfloor/max_freq; " +
                    "echo $min_freq > /sys/devices/system/cpu/bus_dcvs/DDR/soc:qcom,memlat:ddr:prime-latfloor/min_freq; " +
                    "echo $max_freq > /sys/devices/system/cpu/bus_dcvs/DDR/soc:qcom,memlat:ddr:gold/max_freq; " +
                    "echo $min_freq > /sys/devices/system/cpu/bus_dcvs/DDR/soc:qcom,memlat:ddr:gold/min_freq; " +
                    "echo $max_freq > /sys/devices/system/cpu/bus_dcvs/DDR/soc:qcom,memlat:ddr:gold-compute/max_freq; " +
                    "echo $min_freq > /sys/devices/system/cpu/bus_dcvs/DDR/soc:qcom,memlat:ddr:gold-compute/min_freq; " +
                    "echo $max_freq > /sys/devices/system/cpu/bus_dcvs/DDR/soc:qcom,memlat:ddr:silver/max_freq; " +
                    "echo $min_freq > /sys/devices/system/cpu/bus_dcvs/DDR/soc:qcom,memlat:ddr:silver/min_freq; " +
                    "echo $max_freq > /sys/devices/system/cpu/bus_dcvs/DDR/19091000.qcom,bwmon-ddr/max_freq; " +
                    "echo $min_freq > /sys/devices/system/cpu/bus_dcvs/DDR/19091000.qcom,bwmon-ddr/min_freq; "
            "S24" -> command += "echo $max_freq > /sys/devices/platform/17000010.devfreq_mif/devfreq/17000010.devfreq_mif/scaling_devfreq_max; " +
                    "echo $min_freq > /sys/devices/platform/17000010.devfreq_mif/devfreq/17000010.devfreq_mif/scaling_devfreq_min; "
            // Google Tensor 4
            "Pixel9" -> command += "echo $min_freq > /sys/devices/platform/17000010.devfreq_mif/devfreq/17000010.devfreq_mif/min_freq; " +
                    "echo $max_freq > /sys/devices/platform/17000010.devfreq_mif/devfreq/17000010.devfreq_mif/max_freq; "
        }


        // run android kernel command
        val process = Runtime.getRuntime().exec(command)
        process.waitFor()
    }


}

fun ShareResult(context: Context) {
    // 실험 데이터 공유: hard_info.txt
    var filePath = "/storage/emulated/0/Documents/hard_info.txt"
//                val file = File("/sdcard/Documents/hard_info.txt")
    var file = File(filePath)
    if (file.exists()) {
        Log.d("test", "File exists: $filePath")
    } else {
        Log.d("test", "File does not exist: $filePath")
    }
    var fileUri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
    var shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, fileUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share File"))

    // 실험 데이터 공유: infer_info.txt
    filePath = "/storage/emulated/0/Documents/infer_info.txt"
//                val file = File("/sdcard/Documents/hard_info.txt")
    file = File(filePath)
    if (file.exists()) {
        Log.d("test", "File exists: $filePath")
    } else {
        Log.d("test", "File does not exist: $filePath")
    }
    fileUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
    shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, fileUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share File"))
}


@RequiresApi(Build.VERSION_CODES.R)
@ExperimentalMaterial3Api
@Composable
fun ChatView(
    navController: NavController, chatState: AppViewModel.ChatState
) {
    val localFocusManager = LocalFocusManager.current
    Scaffold(topBar = {
        TopAppBar(
            title = {
                Text(
                    text = "MLCChat: " + chatState.modelName.value.split("-")[0],
                    color = MaterialTheme.colorScheme.onPrimary
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary),
            navigationIcon = {
                IconButton(
                    onClick = { navController.popBackStack() },
                    enabled = chatState.interruptable()
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "back home page",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = { chatState.requestResetChat() },
                    enabled = chatState.interruptable()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Replay,
                        contentDescription = "reset the chat",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            })
    }, modifier = Modifier.pointerInput(Unit) {
        detectTapGestures(onTap = {
            localFocusManager.clearFocus()
        })
    }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 10.dp)
        ) {
            val lazyColumnListState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()
            Text(
                text = chatState.report.value,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(top = 5.dp)
            )
            Divider(thickness = 1.dp, modifier = Modifier.padding(vertical = 5.dp))
            LazyColumn(
                modifier = Modifier.weight(9f),
                verticalArrangement = Arrangement.spacedBy(5.dp, alignment = Alignment.Bottom),
                state = lazyColumnListState
            ) {
                coroutineScope.launch {
                    lazyColumnListState.animateScrollToItem(chatState.messages.size)
                }
                items(
                    items = chatState.messages,
                    key = { message -> message.id },
                ) { message ->
                    MessageView(messageData = message)
                }
                item {
                    // place holder item for scrolling to the bottom
                }
            }
            Divider(thickness = 1.dp, modifier = Modifier.padding(top = 5.dp))
            SendMessageView(chatState = chatState)
        }
    }
}

@Composable
fun MessageView(messageData: MessageData) {
    // default render the Assistant text as MarkdownText
    var useMarkdown by remember { mutableStateOf(true) }

    SelectionContainer {
        if (messageData.role == MessageRole.Assistant) {
            Column {
                if (messageData.text.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Show as Markdown",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .wrapContentWidth()
                                .padding(end = 8.dp)
                                .widthIn(max = 300.dp)
                        )
                        Switch(
                            checked = useMarkdown,
                            onCheckedChange = { useMarkdown = it }
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (useMarkdown) {
                        MarkdownText(
                            isTextSelectable = true,
                            modifier = Modifier
                                .wrapContentWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(5.dp)
                                )
                                .padding(5.dp)
                                .widthIn(max = 300.dp),
                            markdown = messageData.text,
                        )
                    } else {
                        Text(
                            text = messageData.text,
                            textAlign = TextAlign.Left,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .wrapContentWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(5.dp)
                                )
                                .padding(5.dp)
                                .widthIn(max = 300.dp)
                        )
                    }
                }
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = messageData.text,
                    textAlign = TextAlign.Right,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .wrapContentWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(5.dp)
                        )
                        .padding(5.dp)
                        .widthIn(max = 300.dp)
                )

            }
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@ExperimentalMaterial3Api
@Composable
fun SendMessageView(chatState: AppViewModel.ChatState) {
    val localFocusManager = LocalFocusManager.current

    /* custom variables */
    var text by rememberSaveable { mutableStateOf("") }
// query stream
    val coroutineScope = rememberCoroutineScope()
    val query_num = 3
    var qa_idx = 1 // means starting qa index (csv line number)
    val qa_start_idx = qa_idx
    var qa_limit = qa_idx + query_num
    // load csv file for hotpot qa
    val context = LocalContext.current
    var qa_lists by remember { mutableStateOf<List<List<String>>>(emptyList()) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            qa_lists = readCSV(context, "datasets/hotpot_qa.csv")
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "dataset loaded!", Toast.LENGTH_SHORT).show()
            }
        }
    }
    var sigterm = mutableStateOf(false) // if it's true, then recording process is terminated

// data collection
    var queryTimes = ArrayList<ArrayList<String>>()
    // Initialization
    queryTimes.add(arrayListOf("systime", "prefill", "decode", "prefill_tok", "decode_tok", "ttft"))

// a function to send and request text generation
    val onSendButtonClicked: () -> Unit = {
        localFocusManager.clearFocus()
        chatState.requestGenerate(text)
        text = ""
    }


    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(IntrinsicSize.Max)
            .fillMaxWidth()
            .padding(bottom = 5.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(text = "Input") },
            modifier = Modifier
                .weight(9f),
        )
        IconButton(
            onClick = {
                localFocusManager.clearFocus()
                chatState.requestGenerate(text)
                text = ""
            },
            modifier = Modifier
                .aspectRatio(1f)
                .weight(1f),
            enabled = (text != "" && chatState.chatable())
        ) {
            Icon(
                imageVector = Icons.Filled.Send,
                contentDescription = "send message",
            )
        }
        TextButton( onClick = {

            // Pixel9 (Google Tensor G4)
            val dvfs = DVFS("Pixel9")
            val gpu_idx = 0
            val ram_idx = 0


            /* GPU DVFS */
            dvfs.setGPUFrequency(gpu_idx)
            //dvfs.unsetGPUFrequency()

            /* RAM DVFS */
            dvfs.setRAMFrequency(ram_idx)
            //dvfs.unsetRAMFrequency()

            Log.d("State", chatState.chatable().toString())

            /* Record Hard Info */
            val startTime = System.currentTimeMillis()
            CoroutineScope(Dispatchers.IO).launch {
                recordProcessing(
                    "/sdcard/Documents",
                    "hard_info.txt",
                    startTime,
                    dvfs.clusterIndices,
                    dvfs.emptyThermal[dvfs.device],
                    sigterm,
                    dvfs.device
                )

                delay(2000) // for stability
                ShareResult(context)
            }

            /* Query Stream */
            coroutineScope.launch {
                qa_idx = 1
                qa_limit = 20
                while (qa_idx < qa_limit + 1){

                    val temp = arrayListOf(((System.currentTimeMillis() - startTime).toDouble()/1000).toString()) // store system time
                    // set input text
                    text = qa_lists[qa_idx][1]
                    // send message and request text generation
                    onSendButtonClicked()
                    qa_idx++

                    while (!chatState.chatable()){ // while not chatable -> not READY
                        delay(20)
                        continue
                    }
                    delay(5)

                    //collect data
                    queryTimes.add(temp) // system time (to be input)
                    temp.add(chatState.prefill_speed.floatValue.toString())
                    temp.add(chatState.decode_speed.floatValue.toString())
                    temp.add(chatState.prompt_tokens.intValue.toString())
                    temp.add(chatState.completion_tokens.intValue.toString())
                    temp.add(chatState.ttft.floatValue.toString())

                    // for test
//                    Log.d("TOKEN", chatState.report.component1().toString()) // ex: prefill: 1.2 tok/s, decode: 12.0 tok/s
//                    Log.d("TOKEN", chatState.prompt_tokens.intValue.toString())
//                    Log.d("TOKEN", chatState.completion_tokens.intValue.toString())
//                    Log.d("TOKEN", chatState.total_tokens.intValue.toString())

                    // write data
                    writeRecord("/sdcard/Documents", "infer_info.txt", queryTimes)
                    queryTimes.clear()
                    chatState.clearCache() // not allow it to maintain context
                }

                // hard record termination signal
                sigterm.value = true

                // write last line
                writeRecord("/sdcard/Documents", "infer_info.txt", queryTimes)

                Thread.sleep(1000) // for stability
                // Reset DVFS settings
                dvfs.unsetGPUFrequency()
                dvfs.unsetRAMFrequency()
            }
        }) {
            // Text of Text button
            Text(
                text = "Test",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            )
        }
    }
}

@Preview
@Composable
fun MessageViewPreviewWithMarkdown() {
    MessageView(
        messageData = MessageData(
            role = MessageRole.Assistant, text = """
# Sample  Header
* Markdown
* [Link](https://example.com)
<a href="https://www.google.com/">Google</a>
"""
        )
    )
}
fun readCSV(context: Context, filename: String): List<List<String>> {
    val result = mutableListOf<List<String>>()
    val inputStream = context.assets.open(filename)
    val reader = BufferedReader(InputStreamReader(inputStream))

    try {
        // read csv
        reader.useLines { lines ->
            lines.forEach { line ->
                val values = parseCSVLine(line) // parse csv line and split
                result.add(values) // In the case of hotpot_qa.csv
                // [[id, question, answer, type, level, supporting_facts, context], ...]
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return result
}
fun parseCSVLine(line: String): List<String> {
    val values = mutableListOf<String>()
    var current = StringBuilder()
    var insideQuotes = false

    for (char in line) {
        when {
            char == '"' -> {
                // inside quote o -> internal state change
                insideQuotes = !insideQuotes
            }

            char == ',' && !insideQuotes -> {
                // comma o && internal x -> field termination
                values.add(current.toString().trim())
                current = StringBuilder()
            }

            else -> {
                // add internal string
                current.append(char)
            }
        }
    }
    // add last field
    values.add(current.toString().trim())
    return values
}

fun getRecordsName(clusterIndices: List<Int>, emptyThermal: List<String>?, deviceName: String): String {
    var names = "Time,"
    // reference type of pid
    var command = "su -c cat /sys/devices/virtual/thermal/thermal_zone*/type"
    val process = Runtime.getRuntime().exec(command)
    val reader = BufferedReader(InputStreamReader(process.inputStream))
    val tempRecord = reader.use { it.readText() }
    process.waitFor()

    names += tempRecord.replace("\n", ",")
    names += "gpu_min_clock,gpu_max_clock,gpu_cur_freq,"

    // 0, 4, 7) 8 Gen 1 CPU: 1 + 3 + 4 | 7: prime, 4: Gold, 0 Silver
    clusterIndices.forEach { index ->
        names += "cpu$index"+"_max_freq, cpu$index" + "_cur_freq,"
    }
//
    command = "su -c awk '{print \$1}' /proc/meminfo"
    val process2 = Runtime.getRuntime().exec(command)
    val reader2 = BufferedReader(InputStreamReader(process2.inputStream))
    val tempRecord2 = reader2.use { it.readText() }
    process2.waitFor()

    names += tempRecord2.replace("\n", ",")
    if (deviceName == "Pixel9") { names += "current_now,voltage_now,"
    } else { names += "power_now,current_now,voltage_now," }

    Log.d("TEST", "names: $names")
    Log.d("TEST", "emptyThermal: $emptyThermal")

    // RAM DVFS frequency name
    names += "scaling_devfreq_max, scaling_devfreq_min, cur_freq"

    if (emptyThermal != null) {
        for (empty in emptyThermal) {
            names = names.replace("$empty,", "")
        }
    }
    Log.d("TEST", "names: $names")
    return names
}

fun getHardRecords(clusterIndices: List<Int>): String {
    // reference type of pid
    var command =
        "su -c awk '{print \$1/1000}' /sys/devices/virtual/thermal/thermal_zone*/temp; " + // thermal info
                // Galaxy Series
                "awk '{print \$1}' /sys/kernel/gpu/gpu_min_clock; awk '{print \$1}' /sys/kernel/gpu/gpu_max_clock; awk '{print \$1}' /sys/kernel/gpu/gpu_clock; " + //gpu clock
                // Pixel
                "awk '{print \$1}' /sys/devices/platform/1f000000.mali/scaling_min_freq; awk '{print \$1}' /sys/devices/platform/1f000000.mali/scaling_max_freq; awk '{print \$1}' /sys/devices/platform/1f000000.mali/cur_freq; " //gpu clock

    // 0, 4, 7) 8 Gen 1 CPU: 1 + 3 + 4 | 7: prime, 4: Gold, 0 Silver
    clusterIndices.forEach { index ->
        command += "awk '{print \$1/1000}' /sys/devices/system/cpu/cpu$index/cpufreq/scaling_max_freq; awk '{print \$1/1000}' /sys/devices/system/cpu/cpu$index/cpufreq/scaling_cur_freq;"
    }
//
    command = command +
            "awk '{print \$2/1024}' /proc/meminfo; " +                // memory info
            "awk '{print}' /sys/class/power_supply/battery/power_now;" +   // power consumption // pixel does not have power_now
            "awk '{print}' /sys/class/power_supply/battery/current_now;" + // current
            "awk '{print}' /sys/class/power_supply/battery/voltage_now;"   // voltage

    // S24/Pixel9 RAM DVFS
    command += "awk '{print \$1/1000}' /sys/devices/platform/17000010.devfreq_mif/devfreq/17000010.devfreq_mif/max_freq; " + // scaling_devfreq_max
            "awk '{print \$1/1000}' /sys/devices/platform/17000010.devfreq_mif/devfreq/17000010.devfreq_mif/min_freq; " + // scaling_devfreq_min
            "awk '{print \$1/1000}' /sys/devices/platform/17000010.devfreq_mif/devfreq/17000010.devfreq_mif/cur_freq; "

    val process = Runtime.getRuntime().exec(command)
    val reader = BufferedReader(InputStreamReader(process.inputStream))
    val record = reader.use { it.readText() }
    process.waitFor()
    return record
}

fun writeRecord(path: String, filename: String, values: ArrayList<ArrayList<String>>) {
    // write
    // reference type of pid
    val writefile = File(path, filename);

    val writer = FileWriter(writefile, true); // allow appending
    writer.use { writer ->
        values.forEach { row ->
            writer.write(row.joinToString(", ") + "\n");
            writer.flush();
        }
    }
}

fun recordProcessing(
    path: String,
    file: String,
    startTime: Long,
    clusterIndices: List<Int>,
    emptyThermal: List<String>?,
    sigterm: MutableState<Boolean>,
    deviceName: String
) {

    //Tester Code
    //while (!sigterm.value) {
    //    Log.d("CHECKER", "recordProcessing1")
    //    val power = getRecord("/sys/class/power_supply/battery/", "power_now")
    //    Log.d("CHECKER", "recordProcessing2: $power")
    //    Thread.sleep(500)
    //}
    val names = getRecordsName(clusterIndices, emptyThermal, deviceName)
    var writefile = File(path, file);
    var writer = FileWriter(writefile); // allow appending
    writer.use { writer ->
        //values.forEach { row ->
        writer.write(names + "\n");
        writer.flush();
        //}
    }


    while (!sigterm.value) {
        //val power = getRecord("/sys/class/power_supply/battery/", "power_now")
        //val temp = (getRecord("/sys/devices/virtual/thermal/thermal_zone77/", "temp").toDouble()/1000).toString() // convert uC to C
        val curTimeSec = ((System.currentTimeMillis() - startTime).toDouble()/1000).toString() // ms [unit]
        val records = getHardRecords(clusterIndices)
        // packing records into one row
        val record = listOf(curTimeSec, records.replace("\n", ", "))

        Log.d("CHECKER", "records:$record")
        //writeRecord("/sdcard/Documents/", "test.txt", records)

        writefile = File(path, file);
        writer = FileWriter(writefile, true); // allow appending
        writer.use { writer ->
            //values.forEach { row ->
            writer.write(record.joinToString(", ") + "\n");
            writer.flush();
            //}
        }
        //Thread.sleep(170) // 200ms
        Thread.sleep(120) // 150ms
    }

    return;
}