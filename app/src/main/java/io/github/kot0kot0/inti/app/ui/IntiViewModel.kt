package io.github.kot0kot0.inti.app.ui

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kot0kot0.inti.app.ble.IntiControlService
import io.github.kot0kot0.inti.app.data.IntiSettings
import io.github.kot0kot0.inti.client.IntiClient
import kotlinx.coroutines.*
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class IntiViewModel(
    private val client: IntiClient,
    private val settings: IntiSettings
) : ViewModel() {
    var isConnected by mutableStateOf(false)
    var isScanning by mutableStateOf(false)
    var isRunning by mutableStateOf(false)

    var whiteLevel by mutableFloatStateOf(settings.whiteLevel.toFloat())
    var warmLevel by mutableFloatStateOf(settings.warmLevel.toFloat())
    var durationMinutes by mutableStateOf(settings.durationMinutes)
    var finishTimeText by mutableStateOf("")

    init {
        // ServiceにClientを共有
        IntiControlService.intiClient = client
    }

    fun updateWhite(value: Float) {
        whiteLevel = value
        settings.whiteLevel = value.toInt()
        Timber.d("白色レベル変更: ${value.toInt()}")
    }

    fun updateWarm(value: Float) {
        warmLevel = value
        settings.warmLevel = value.toInt()
        Timber.d("暖色レベル変更: ${value.toInt()}")
    }

    fun sendCurrentLevels() {
        if (!isConnected) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val w = whiteLevel.toInt()
                val r = warmLevel.toInt()
                Timber.d("Bluetooth送信: W=$w, R=$r")
                client.setWhiteLight(w > 0)
                client.setWhiteBrightness(w)
                client.setWarmLight(r > 0)
                client.setWarmBrightness(r)
                client.endControl()
                client.apply()
            } catch (e: Exception) {
                Timber.e(e, "送信失敗")
            }
        }
    }

    fun startControl(context: Context) {
        val minutes = durationMinutes.toIntOrNull() ?: 30
        isRunning = true

        // 消灯時刻計算
        val cal = Calendar.getInstance().apply { add(Calendar.MINUTE, minutes) }
        finishTimeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.time)

        Timber.i("制御開始ボタン押下: ${minutes}分間")

        // サービス起動 (バックグラウンド維持)
        val intent = Intent(context, IntiControlService::class.java).apply {
            putExtra("duration", minutes)
            putExtra("whiteLevel", whiteLevel.toInt())
            putExtra("warmLevel", warmLevel.toInt())
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}