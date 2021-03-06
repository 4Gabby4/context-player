package com.gabchmel.sensorprocessor.data.model

import java.util.*

data class MeasuredSensorValues(
    var currentTime: Date? = null,
    var longitude: Double = 0.0,
    var latitude: Double = 0.0,
    var currentState: String = "UNKNOWN",
    var lightSensorValue: Float = 0.0f,
    var deviceLying: Float = 0.0f,
    var BTDeviceConnected: Float = 0.0f,
    var headphonesPluggedIn: Float = 0.0f,
    var pressure: Float = 0.0f,
    var temperature: Float = 0.0f,
    var wifi: UInt = 0u,
    var connection: String = "NONE",
    var batteryStatus: String = "NONE",
    var chargingType: String = "NONE",
    var proximity: Float = 0.0f,
    var humidity: Float = 0.0f,
    var heartBeat: Float = 0.0f,
    var heartRate: Float = 0.0f
)