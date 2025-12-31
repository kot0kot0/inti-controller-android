package io.github.kot0kot0.inti.app.ble

import io.github.kot0kot0.inti.client.BleTransport

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.callback.profile.ProfileReadResponse
import no.nordicsemi.android.ble.ktx.asValidResponseFlow
import no.nordicsemi.android.ble.ktx.suspend
import java.util.*

class AndroidBleTransport(context: Context) : BleManager(context), BleTransport {
    private val TARGET_CHAR_UUID = UUID.fromString("6ff4d0b4-f687-11e6-bc64-92361f002671")
    private var characteristic: BluetoothGattCharacteristic? = null

    override fun getGattCallback(): BleManagerGattCallback = object : BleManagerGattCallback() {
        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            for (service in gatt.services) {
                val target = service.getCharacteristic(TARGET_CHAR_UUID)
                if (target != null) {
                    characteristic = target
                    characteristic?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    return true
                }
            }
            return false
        }
        override fun onServicesInvalidated() { characteristic = null }
    }

    override suspend fun write(data: ByteArray) {
        val char = characteristic ?: return
        writeCharacteristic(char, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT).suspend()
    }

    override fun observeNotifications(): Flow<ByteArray> {
        return setNotificationCallback(characteristic)
            .asValidResponseFlow<ProfileReadResponse>()
            .map { it.rawData?.value ?: byteArrayOf() }
    }

    suspend fun connectToDevice(device: BluetoothDevice) {
        connect(device).retry(3, 100).useAutoConnect(false).suspend()
    }
}
