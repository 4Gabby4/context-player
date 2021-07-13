package com.gabchmel.sensorprocessor

import android.Manifest
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.content.*
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import com.gabchmel.common.LocalBinder
import com.gabchmel.predicitonmodule.PredictionModelBuiltIn
import com.gabchmel.sensorprocessor.utility.InputProcessHelper.inputProcessHelper
import com.gabchmel.sensorprocessor.utility.InputProcessHelper.processInputCSV
import com.gabchmel.sensorprocessor.activityDetection.TransitionList
import com.gabchmel.sensorprocessor.utility.SensorManagerUtility
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs


class SensorProcessService : Service() {

    // Structure to store sensor values
    var _sensorData = MutableStateFlow(
        SensorData(
            null, 0.0, 0.0, "NONE", 0.0f,
            0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
            0, 0.0f
        )
    )
    val sensorData: StateFlow<SensorData> = _sensorData

    // List of orientation coordinates
    var coordList = mutableListOf<Float>()

    // Input CSV file to save sensor values
    private lateinit var csvFile: File

    // Random Forest model
    private val predictionModel = PredictionModelBuiltIn(this)

    // Song IDs
    private var classNames = arrayListOf<String>()

    // Saved prediction result as StateFlow to show notification
    private val _prediction = MutableStateFlow<String?>(null)
    val prediction: StateFlow<String?> = _prediction

    // binder given to clients
    private val binder = object : LocalBinder<SensorProcessService>() {
        override fun getService() = this@SensorProcessService
    }

    override fun onCreate() {
        super.onCreate()

        // CSV file with sensor measurements and context data
        csvFile = File(this.filesDir, "data.csv")

        registerLocationListener()

        // Set ut callbacks for activity detection
        activityDetection()

        // Check if the BT device is connected
        bluetoothDevicesConnection()

//        val receiverFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
//        registerReceiver(SensorReceiver(), receiverFilter)
    }

    override fun onBind(intent: Intent): IBinder {

        val sensorManager = this.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Register listeners to sensor value changes
        SensorManagerUtility.sensorReader(
            sensorManager, Sensor.TYPE_ORIENTATION,
            "Orientation",
            this
        )

        SensorManagerUtility.sensorReader(
            sensorManager, Sensor.TYPE_LIGHT,
            "Ambient light",
            this
        )

        SensorManagerUtility.sensorReader(
            sensorManager, Sensor.TYPE_PRESSURE,
            "Barometer",
            this
        )

        SensorManagerUtility.sensorReader(
            sensorManager, Sensor.TYPE_AMBIENT_TEMPERATURE,
            "Temperature",
            this
        )

        SensorManagerUtility.sensorReader(
            sensorManager, Sensor.TYPE_PROXIMITY,
            "Proximity",
            this
        )

        SensorManagerUtility.sensorReader(
            sensorManager, Sensor.TYPE_RELATIVE_HUMIDITY,
            "Humidity",
            this
        )

        SensorManagerUtility.sensorReader(
            sensorManager, Sensor.TYPE_HEART_BEAT,
            "Heart beat",
            this
        )

        SensorManagerUtility.sensorReader(
            sensorManager, Sensor.TYPE_HEART_RATE,
            "Heart rate",
            this
        )

        SensorManagerUtility.sensorReader(
            sensorManager, Sensor.TYPE_TEMPERATURE,
            "Heart rate",
            this
        )

        headphonesPluggedInDetection()
        wifiConnection()
        internetConnectivity(this)

        return binder
    }

    fun writeToFile(songID: String) {
        // Read current time
        _sensorData.value.currentTime = Calendar.getInstance().time

//        Log.d("Sensor", "write")

        processOrientation()

        // TODO Make check that we have a value - maybe we don't have to have value idk - let
        try {
            // TODO redo do for each to optimize
            // Write to csv file
            csvFile.appendText(
                songID + ","
                    + sensorData.value.currentTime + ","
                    + sensorData.value.longitude + ","
                    + sensorData.value.latitude + ","
                    + sensorData.value.currentState + ","
                    + sensorData.value.lightSensorValue + ","
                    + sensorData.value.deviceLying + ","
                    + sensorData.value.BTdeviceConnected + ","
                    + sensorData.value.headphonesPluggedIn + ","
                    + sensorData.value.pressure + ","
                    + sensorData.value.temperature + "\n"
            )
        } catch (e: IOException) {
            Log.e("Err", "Couldn't write to file", e)
        }
    }

    fun createModel(): Boolean {
        // Process input CSV file and save class names into ArrayList<String>
        classNames = processInputCSV(this)

        if(!predictionModel.createModel(classNames)) {
            return false
        }

        Log.d("model", "model created")
        return true
    }

    fun triggerPrediction() {
        Log.d("prediction", "trigger prediction")

        // Get the processed input values
        val input = inputProcessHelper(sensorData.value)

        _prediction.value = predictionModel.predict(input, classNames)
    }

    fun detectContextChange(): Boolean {
        val prefs = getSharedPreferences("MyPrefsFile", MODE_PRIVATE)
        val time = prefs.getString("time", "No name defined")
        val headphones = prefs.getFloat("headphones", -1.0f)
        val bluetooth = prefs.getFloat("bluetooth", -1.0f)
        val light = prefs.getFloat("light", -1.0f)
        if (sensorData.value.BTdeviceConnected != bluetooth) {
            return true
        }
        if (sensorData.value.lightSensorValue != light) {
            return true
        }
        return false
    }

    fun saveSensorData() {
        val editor = getSharedPreferences("MyPrefsFile", MODE_PRIVATE).edit()
        editor.putString("time", sensorData.value.currentTime.toString())
        sensorData.value.longitude?.let { editor.putFloat("longitude", it.toFloat()) }
        sensorData.value.latitude?.let { editor.putFloat("latitude", it.toFloat()) }
        editor.putString("state", sensorData.value.currentState)
        sensorData.value.lightSensorValue?.let { editor.putFloat("light", it) }
        sensorData.value.deviceLying?.let { editor.putFloat("lying", it) }
        sensorData.value.BTdeviceConnected?.let { editor.putFloat("bluetooth", it) }
        sensorData.value.headphonesPluggedIn?.let { editor.putFloat("headphones", it) }
        editor.apply()
    }

    private fun activityDetection() {
        val request = ActivityTransitionRequest(TransitionList.getTransitions())

        val intent = Intent(this, ActivityTransitionReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0)

        // myPendingIntent is the instance of PendingIntent where the app receives callbacks.
        val task = ActivityRecognition.getClient(this)
            .requestActivityTransitionUpdates(request, pendingIntent)

        // used: https://heartbeat.fritz.ai/detect-users-activity-in-android-using-activity-transition-api-f718c844efb2
        task.addOnSuccessListener {
            // Handle success
            Log.d("ActivityRecognition", "Transitions Api registered with success")
        }

        task.addOnFailureListener { e: Exception ->
            // Handle error
            Log.d(
                "ActivityRecognition",
                "Transitions Api could NOT be registered ${e.localizedMessage}"
            )
        }
    }

    private fun processOrientation() {

//    private var orientSensorAzimuthZAxis: Float = 0.0f
//    private var orientSensorPitchXAxis: Float = 0.0f
//    private var orientSensorRollYAxis: Float = 0.0f

        // Get the range of acceptable values for lying device - -1.58 is exactly lying
        _sensorData.value.deviceLying = if (abs(coordList[0]) in 1.4f..1.7f) {
            Log.d("Orientation", "Device is lying")
            1.0f
        } else {
//            Log.d("Orientation", "Device is staying")
            0.0f
        }
    }

    private fun bluetoothDevicesConnection() {
        // Check if the device supports bluetooth
        val pm: PackageManager = this.packageManager
        val hasBluetooth = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)

        if (hasBluetooth) {

            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val pairedDevices = bluetoothAdapter.bondedDevices

            val s: MutableList<String> = ArrayList()
            for (bt in pairedDevices) s.add(bt.name)

            _sensorData.value.BTdeviceConnected =
                if (bluetoothAdapter != null && BluetoothProfile.STATE_CONNECTED == bluetoothAdapter.getProfileConnectionState(
                        BluetoothProfile.HEADSET
                    )
                ) {
                    Log.d("BT", "mame headset")
                    1.0f
                } else {
                    0.0f
                }
        }
    }

    private fun headphonesPluggedInDetection() {
        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                val action = intent.action
                if (Intent.ACTION_HEADSET_PLUG == action) {
                    _sensorData.value.headphonesPluggedIn =
                        intent.getIntExtra("state", -1).toFloat()
                    if (_sensorData.value.headphonesPluggedIn == 0.0f) {
//                        Toast.makeText(
//                            applicationContext,
//                            "Headphones not plugged in",
//                            Toast.LENGTH_LONG
//                        ).show()
                    } else if (_sensorData.value.headphonesPluggedIn == 1.0f) {
//                        Toast.makeText(
//                            applicationContext,
//                            "Headphones plugged in",
//                            Toast.LENGTH_LONG
//                        ).show()
                    }
                }
            }
        }

        val receiverFilter = IntentFilter(Intent.ACTION_HEADSET_PLUG)
        registerReceiver(broadcastReceiver, receiverFilter)
    }

    private fun wifiConnection() {
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo

        Log.d("ssid", "SSID:${wifiInfo.ssid}, hashCode:${wifiInfo.ssid.hashCode()}")

        _sensorData.value.wifi = wifiInfo.ssid.hashCode()

//        return wifiInfo.ssid.hashCode().toString()
    }

    private fun internetConnectivity(context: Context): String {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities: NetworkCapabilities?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        } else {
            return "NONE"
        }
        if (capabilities != null) {
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                    return "TRANSPORT_CELLULAR"
                }
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                    return "TRANSPORT_WIFI"
                }
//                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
//                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
//                    return "TRANSPORT_ETHERNET"
//                }
            }
        }
        return "NONE"
    }

    private fun registerLocationListener() {

        // Location change listener
        val locationListener = LocationListener { location ->
            _sensorData.value.longitude = location.longitude
            _sensorData.value.latitude = location.latitude
        }

        // Persistent LocationManager reference
        val locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager?

        // TODO if not granted
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("permission", "permission not granted")
            return
        }

        locationManager?.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            0L,
            0f,
            locationListener
        )

        //        if (location.value == null) {
//            locationManager.requestLocationUpdates(getProviderName(), 0, 0, this)
//        }

    }
}