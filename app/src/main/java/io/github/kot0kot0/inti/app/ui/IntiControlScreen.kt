package io.github.kot0kot0.inti.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun IntiControlScreen(viewModel: IntiViewModel, onConnectRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // タイトル
        Text("Inti Controller", style = MaterialTheme.typography.h5)

        Spacer(Modifier.height(16.dp))

        // 接続開始ボタン
        Button(
            onClick = onConnectRequest,
            enabled = !viewModel.isConnected && !viewModel.isScanning,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (viewModel.isScanning) "探索中..." else if (viewModel.isConnected) "接続済み" else "接続する")
        }

        Spacer(Modifier.height(16.dp))

        // --- 白色レベル設定 ---
        Text("白色レベル: ${viewModel.whiteLevel.toInt()}")
        Slider(
            value = viewModel.whiteLevel,
            onValueChange = { viewModel.updateWhite(it) },
            onValueChangeFinished = { viewModel.sendCurrentLevels() },
            valueRange = 0f..10f
        )

        // --- 暖色レベル設定 ---
        Text("暖色レベル: ${viewModel.warmLevel.toInt()}")
        Slider(
            value = viewModel.warmLevel,
            onValueChange = { viewModel.updateWarm(it) },
            onValueChangeFinished = { viewModel.sendCurrentLevels() },
            valueRange = 0f..10f
        )

        Spacer(Modifier.height(16.dp))

        // --- 点灯時間設定 ---
        TextField(
            value = viewModel.durationMinutes,
            onValueChange = { viewModel.durationMinutes = it },
            label = {
                // 入力前でもはっきり見えるよう、濃いグレー（Black）を指定
                Text("点灯時間（分）", color = Color.Black)
            },
            placeholder = {
                Text("例: 60", color = Color.Gray)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            // ここが重要：MaterialThemeに頼らず、直接色を指定して背景を「塗りつぶし」ます
            colors = TextFieldDefaults.textFieldColors(
                // 1. 未入力・未選択状態でも見える「濃いめのグレー」
                backgroundColor = Color(0xFFE0E0E0),

                // 2. テキストの色を強制
                textColor = Color.Black,

                // 3. 下線をあえて消す（箱としての存在感を強調）
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            shape = MaterialTheme.shapes.small // 少し角を丸めて「箱」らしくする
        )

        // 点灯状態表示
        if (viewModel.isRunning) {
            Spacer(Modifier.height(16.dp))
            Text(
                "消灯予定時刻: ${viewModel.finishTimeText}",
                color = MaterialTheme.colors.primary,
                style = MaterialTheme.typography.subtitle1
            )
        }

        Spacer(Modifier.weight(1f)) // 残りのスペースを埋めてボタンを下に配置

        // 実行ボタン
        val context = LocalContext.current
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            onClick = { viewModel.startControl(context) },
            enabled = viewModel.isConnected
        ) {
            Text(if (viewModel.isRunning) "延長・更新" else "点灯開始")
        }
    }
}