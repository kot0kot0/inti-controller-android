package io.github.kot0kot0.inti.app.ble

import io.github.kot0kot0.inti.client.BleTransport

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class DummyBleTransport : BleTransport {
    override suspend fun write(data: ByteArray) {
        // 実際にはここでBluetoothに書き込むが、まずはログだけ出す
        println("BLE Write: ${data.joinToString(", ") { it.toString() }}")
    }

    override fun observeNotifications(): Flow<ByteArray> {
        // 通知（ライトからの返事）はとりあえず空にする
        return emptyFlow()
    }
}