package com.example.androidSensorsTest

import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var mSensorManager: SensorManager
    private var mLight: Sensor? = null
    private var mGyroscope: Sensor? = null
    private var resume = true
    private lateinit var mp: MediaPlayer
    lateinit var mainHandler: Handler
//    private val ip = "10.0.2.2"
    private val ip = "192.168.0.99"
    private val checkSoundStatusTask = object : Runnable {
        override fun run() {
            checkSoundStatus()
            mainHandler.postDelayed(this, 1000)
        }
    }
    private val sendDataTask = object : Runnable {
        override fun run() {
            sendData()
            mainHandler.postDelayed(this, 1000)
        }
    }

    fun sendData() {
        val textView = findViewById<TextView>(R.id.sendEventValue)

        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(this)

        val light = findViewById<TextView>(R.id.light).getText().toString().toDouble().toInt()
        val gyro = findViewById<TextView>(R.id.gyro_y).getText().toString().toDouble().toInt()
        val url = "http://${ip}:8080/sendData?light=${light}&gyro=${gyro}"

        // Request a string response from the provided URL.
        val stringRequest = StringRequest(
            Request.Method.POST, url,
            { response ->
                textView.text = getString(R.string.sensor_save_response, response)
            },
            { textView.text = getString(R.string.Error_sending_sensor_value) })

        // Add the request to the RequestQueue.
        queue.add(stringRequest)
    }

    private fun checkSoundStatus() {
        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(this)
        val url = "http://${ip}:8080/getSoundStatus"

        // Request a string response from the provided URL.
        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                findViewById<TextView>(R.id.soundStatusValue).text =  response
                if (response.toBoolean()) {
                    mp.start()
                }
            },
            { Toast.makeText(this, "Error reading sound status", Toast.LENGTH_SHORT).show() })

        // Add the request to the RequestQueue.
        queue.add(stringRequest)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        mp = MediaPlayer.create(this, R.raw.school_bell)
        mainHandler = Handler(Looper.getMainLooper())
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        print("accuracy changed")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && resume) {
            if (event.sensor.type == Sensor.TYPE_LIGHT) {
                findViewById<TextView>(R.id.light).text = event.values[0].toString()
            }
            if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
                findViewById<TextView>(R.id.gyro_y).text = event.values[1].toString()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL)
        mainHandler.post(checkSoundStatusTask)
        mainHandler.post(sendDataTask)
    }

    override fun onPause() {
        super.onPause()
        mSensorManager.unregisterListener(this)
        mainHandler.removeCallbacks(checkSoundStatusTask)
        mainHandler.removeCallbacks(sendDataTask)
    }

    override fun onDestroy() {
        super.onDestroy()
        mp.release()
    }

    fun resumeReading(view: View) {
        this.resume = true
        mainHandler.post(checkSoundStatusTask)
        mainHandler.post(sendDataTask)
    }

    fun pauseReading(view: View) {
        this.resume = false
        mainHandler.removeCallbacks(checkSoundStatusTask)
        mainHandler.removeCallbacks(sendDataTask)
    }
}