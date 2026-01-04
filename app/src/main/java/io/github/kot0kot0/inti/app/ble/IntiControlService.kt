package io.github.kot0kot0.inti.app.ble

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import io.github.kot0kot0.inti.app.MainActivity
import io.github.kot0kot0.inti.client.IntiClient
import kotlinx.coroutines.*
import timber.log.Timber

class IntiControlService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var controlJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    // 本来はDIで渡すべきですが、簡略化のためシングルトン的に扱うか、
    // ここでClientを再生成する必要があります。
    // 今回はMainActivityで初期化したclientを外部から注入できるように想定
    companion object {
        var intiClient: IntiClient? = null
    }

    override fun onCreate() {
        super.onCreate()
        // WakeLockの初期化
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "IntiApp::ControlWakeLock")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val duration = intent?.getIntExtra("duration", 30) ?: 30
        val white = intent?.getIntExtra("whiteLevel", 0) ?: 0
        val warm = intent?.getIntExtra("warmLevel", 0) ?: 0

        // CPUを寝かせないようにロックを取得
        wakeLock?.acquire(duration * 60 * 1000L + 60000L) // 予定時間 + 1分のバッファ

        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "inti_control")
            .setContentTitle("Inti 制御中")
            .setContentText("${duration}分後に消灯します")
            .setSmallIcon(android.R.drawable.ic_menu_day)
            .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
            .build()

        startForeground(1, notification)

        controlJob?.cancel()
        controlJob = serviceScope.launch {
            val endTime = System.currentTimeMillis() + (duration * 60 * 1000)
            val client = intiClient ?: return@launch

            try {
                while (isActive && System.currentTimeMillis() < endTime) {
                    Timber.d("Service: 定期送信を実行中 (残り: ${(endTime - System.currentTimeMillis())/1000}秒)")
                    if (white == 0) {
                        client.setWhiteLight(false)
                    }
                    else {
                        client.setWhiteBrightness(white)
                        client.setWhiteLight(true)
                    }
                    if (warm == 0) {
                        client.setWarmLight(false)
                    }
                    else {
                        client.setWarmBrightness(warm)
                        client.setWarmLight(true)
                    }
                    client.endControl()
                    client.apply()

                    // 次の再送まで待機（25分または終了まで）
                    delay(minOf(endTime - System.currentTimeMillis(), 25 * 60 * 1000L))
                }
            } finally {
                Timber.i("Service: 時間終了につき消灯")
                client.setWhiteLight(false)
                client.setWarmLight(false)
                client.endControl()
                client.apply()
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("inti_control", "Inti Control Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        controlJob?.cancel()
        serviceScope.cancel()
        // 解放を忘れない（重要！）
        if (wakeLock?.isHeld == true) wakeLock?.release()
        super.onDestroy()
    }
}