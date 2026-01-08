package com.example.gestorarchivos.model

data class BluetoothDeviceInfo(
    val name: String,
    val address: String,
    val isPaired: Boolean = false,
    val isConnected: Boolean = false,
    val rssi: Int? = null
)