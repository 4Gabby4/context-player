package com.gabchmel.sensorprocesser;

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager;

class SensorReader(context: Context) {
    private var sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    val deviceSensors: List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_ALL)


}
