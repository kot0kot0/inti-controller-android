package io.github.kot0kot0.inti.app

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import io.github.kot0kot0.inti.app.ble.AndroidBleTransport
import io.github.kot0kot0.inti.app.data.IntiSettings
import io.github.kot0kot0.inti.app.ui.*
import io.github.kot0kot0.inti.app.ui.theme.IntiGuiAppTheme
import io.github.kot0kot0.inti.client.IntiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 各コンポーネントの初期化
        val transport = AndroidBleTransport(this)
        val client = IntiClient(transport)
        val settings = IntiSettings(this)
        val viewModel = IntiViewModel(client, settings)

        setContent {
            IntiGuiAppTheme {
                PermissionGating {
                    IntiControlScreen(viewModel = viewModel) {
                        // 接続ボタンが押された時の処理
                        performScanAndConnect(this, transport, viewModel)
                    }
                }
            }
        }
    }
}

/**
 * 権限チェックを行うComposable
 */
@Composable
fun PermissionGating(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12 (S) 以上：Bluetooth専用の権限
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        // Android 11以下：「位置情報」の権限が必要（スキャンにGPSが必要だった名残）
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // rememberのため単なる変数ではなくComposeが常に監視している
    // rememberにより画面が再描画されても値を忘れないようにメモリに保持する
    var hasPermission by remember {
        // mutableStateOfの値が書き換わるとComposeが対象のUIを更新する
        mutableStateOf(permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        })
    }

    // OSのシステム画面でAndroidの権限許可ダイアログを表示する
    // リクエストを送ると一度アプリが止まり、ユーザーが許可した後に結果がMap<String, Boolean>で返ってくる
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // すべてが許可してもらえればtrueになる
        hasPermission = results.values.all { it }
    }

    if (hasPermission) {
        // ユーザーが権限を許可した場合はアプリのGUIを続けて表示する
        content()
    } else {
        // ユーザーが権限を拒否した場合はアプリを使用できないことを伝える
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = { launcher.launch(permissions) }) {
                Text("Bluetooth権限を許可してください")
            }
        }
    }
}

/**
 * スキャンと接続を実行するヘルパー関数
 */
private fun performScanAndConnect(
    context: Context,
    transport: AndroidBleTransport,
    viewModel: IntiViewModel
) {
    // Android OSが管理している巨大なサービス群からBluetoothを取得
    val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    // BluetoothのON/OFFを取得。OFFの場合はnullが返る
    val scanner = manager.adapter.bluetoothLeScanner ?: return
    // Dispatchers.MainはViewModelを操作するための専用スレッド
    val scope = CoroutineScope(Dispatchers.Main)

    viewModel.isScanning = true


    // objectクラス
    val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            // intiを探す
            val name = result.device.name ?: result.scanRecord?.deviceName ?: ""
            if (name.contains("inti", ignoreCase = true)) {
                scanner.stopScan(this)
                // Bluetooth接続処理は時間がかかるため非同期で実行
                scope.launch {
                    try {
                        // 接続を試みる
                        transport.connectToDevice(result.device)
                        viewModel.isConnected = true
                    } catch (e: Exception) {
                        viewModel.isConnected = false
                    } finally {
                        viewModel.isScanning = false
                    }
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            viewModel.isScanning = false
        }
    }

    // OSに非同期でscanを実行してもらう
    scanner.startScan(callback)

    // 10秒経っても見つからなければスキャン停止
    scope.launch {
        kotlinx.coroutines.delay(10000)
        if (viewModel.isScanning) {
            scanner.stopScan(callback)
            viewModel.isScanning = false
        }
    }
}