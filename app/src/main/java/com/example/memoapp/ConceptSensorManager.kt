package com.example.memoapp

import android.content.Context
import android.hardware.Sensor
import android.content.Context.SENSOR_SERVICE
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class ConceptSensorManager(context: Context, private val onUpdate: (Float, Float) -> Unit) : SensorEventListener {
    private val sensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager
    private val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    fun start() {
        gyro?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            // event.values[0]: X軸周りの角速度 (上下の傾きに近い)
            // event.values[1]: Y軸周りの角速度 (左右の傾きに近い)
            // 感度調整のための係数 (10.0f)
            val dx = -event.values[1] * 10.0f
            val dy = event.values[0] * 10.0f
            onUpdate(dx, dy)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
