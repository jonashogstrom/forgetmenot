package com.jht.forgetmenot

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_first.*


class FirstFragment() : Fragment(), SensorEventListener {

    private lateinit var ringtone: Ringtone
    private var lastVib: Long = 0L
    private lateinit var vibrator: Vibrator
    private var lastAccumulatedMovement: Float = 0F
    private var accumulatedMovement: Float = 0F;
    private var sensorEventCounter: Int = 0
    private var lastMode: String = "--"
    private var counter: Int = 0
    private var delay: Long = 5000;
    private lateinit var sensorManager: SensorManager
    private var mSensor: Sensor? = null
    private val effect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE);


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sensorManager = context?.getSystemService(Context.SENSOR_SERVICE) as SensorManager;

        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            val sensors: List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER)
            // Use the version 3 gravity sensor.
            mSensor = sensors.firstOrNull()
        } else {
            // Failure! No magnetometer.
        }

        vibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        val notification =RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ringtone = RingtoneManager.getRingtone(context, notification)


        val delayedHandler: Handler = Handler()
        val r = Runnable{checkChargeStatus(true)}
        delayedHandler.postDelayed(r, this.delay)

        view.findViewById<Button>(R.id.button_first).setOnClickListener {
            LostWirelessCharging();
            checkChargeStatus(false);

        }
    }

    fun checkChargeStatus(reschedule: Boolean)
    {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context?.registerReceiver(null, ifilter)
        }

        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL

        val chargePlug: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val usbCharge: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
        val acCharge: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_AC

        counter++;
        var s = counter.toString()+" status: "+status.toString()+ " chargePlug: "+chargePlug.toString() + "\n\n";
        s += "movement: " + lastAccumulatedMovement + "\n\n"
        s += "delay: " + delay + "\n\n"
        s += "acc: " + lastSensorString + "\n\n"
        s += "eventCounter: " +sensorEventCounter + "\n\n"
        var mode = "--"
        if (isCharging) {
            if (usbCharge)
                mode = "USB"
            else if (acCharge)
                mode = "AC"
            else
            {
                mode = "QI"
                delay = 500;
            }
        }
        s += mode
        if ((lastMode == "QI" || lastMode == "USB") && mode == "--")
        {
            LostWirelessCharging();
        }

        this.lastMode = mode;

        textview_first.text = s

        if (reschedule) {
            val delayedHandler = Handler()
            delayedHandler.postDelayed({
                checkChargeStatus(true)
            }, delay)
        }

    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Do something here if sensor accuracy changes.
    }

    private var lastSensortimeStamp: Long = 0L
    private var lastSensorValue: Float = 0F
    private var lastSensorString: String = ""

    override fun onSensorChanged(event: SensorEvent) {
        // The light sensor returns a single value.
        // Many sensors return 3 values, one for each axis.
        if (lastSensortimeStamp != 0L && lastSensortimeStamp != event.timestamp)
        {
            val movement = Math.abs(lastSensorValue - event.values[0])
            var t = System.currentTimeMillis();
            if (movement >5)
            {
                lastSensorString = "motion detected: " + movement.toString()
                sensorManager.unregisterListener(this)
                delay=5000;
            }
            else if (t-lastVib > 800)
            {
                lastSensorString = "low motion: " + movement.toString()
//                lastSensorString = "${event.timestamp}, ${event.values[0]}, ${event.values[1]}, ${event.values[2]}"
                accumulatedMovement = movement;
                vibrator.vibrate(effect);
                if (!ringtone.isPlaying)
                    ringtone.play()
                lastVib = t;
            }
        }

        lastSensortimeStamp = event.timestamp
        lastSensorValue = event.values[0]
    }


    private fun LostWirelessCharging() {
        lastSensortimeStamp = 0
        lastSensorValue=0F
        sensorEventCounter = 0
        accumulatedMovement = 0F
        delay = 500
        lastVib = System.currentTimeMillis() + 2000
        mSensor?.also { acc -> sensorManager.registerListener(
            this,
            acc,
            SensorManager.SENSOR_DELAY_NORMAL
        ) }
    }
}