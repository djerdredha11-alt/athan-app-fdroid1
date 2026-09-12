package com.example.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.data.calculation.AstronomicalCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

data class QiblaUiState(
    val hasSensor: Boolean = true,
    val deviceAzimuth: Float = 0f,
    val qiblaBearing: Float = 0f,
    val needleAngle: Float = 0f, // Direction of Kaaba relative to device top
    val distanceKm: Double = 0.0,
    val isFacingQibla: Boolean = false,
    val accuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH
)

class QiblaSensorManager(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _state = MutableStateFlow(QiblaUiState(hasSensor = rotationSensor != null || (accelerometer != null && magnetometer != null)))
    val state: StateFlow<QiblaUiState> = _state.asStateFlow()

    private var targetLatitude: Double = 21.4225
    private var targetLongitude: Double = 39.8262
    private var currentQiblaBearing: Float = 0f
    private var currentDistanceKm: Double = 0.0

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var filteredAzimuth = 0f
    private val alpha = 0.15f // Low pass filter factor

    fun updateLocation(latitude: Double, longitude: Double) {
        targetLatitude = latitude
        targetLongitude = longitude
        val info = AstronomicalCalculator.calculateQibla(latitude, longitude)
        currentQiblaBearing = info.qiblaBearing
        currentDistanceKm = info.distanceKm
        updateNeedle(filteredAzimuth)
    }

    fun startListening() {
        if (rotationSensor != null) {
            sensorManager?.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else if (accelerometer != null && magnetometer != null) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            sensorManager?.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
        } else {
            _state.value = _state.value.copy(
                hasSensor = false,
                qiblaBearing = currentQiblaBearing,
                distanceKm = currentDistanceKm
            )
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        var newAzimuth = filteredAzimuth

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            var azimuthDeg = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
            azimuthDeg = (azimuthDeg + 360f) % 360f
            newAzimuth = azimuthDeg
        } else {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                System.arraycopy(event.values, 0, gravity, 0, 3)
            } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                System.arraycopy(event.values, 0, geomagnetic, 0, 3)
            }
            if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                var azimuthDeg = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                azimuthDeg = (azimuthDeg + 360f) % 360f
                newAzimuth = azimuthDeg
            }
        }

        // Smooth azimuth transition
        val diff = ((newAzimuth - filteredAzimuth + 180f) % 360f) - 180f
        filteredAzimuth = (filteredAzimuth + diff * alpha + 360f) % 360f

        updateNeedle(filteredAzimuth, event.accuracy)
    }

    private fun updateNeedle(azimuth: Float, accuracy: Int = _state.value.accuracy) {
        val needle = (currentQiblaBearing - azimuth + 360f) % 360f
        val isFacing = abs(needle) < 3.5f || abs(needle - 360f) < 3.5f

        _state.value = _state.value.copy(
            hasSensor = true,
            deviceAzimuth = azimuth,
            qiblaBearing = currentQiblaBearing,
            needleAngle = needle,
            distanceKm = currentDistanceKm,
            isFacingQibla = isFacing,
            accuracy = accuracy
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        _state.value = _state.value.copy(accuracy = accuracy)
    }
}
