package com.example.kunalsutradhar.heartratemonitor;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by kunal.
 */

public class Accelerometer extends Service implements SensorEventListener{
    SensorManager sensorManager;
    Sensor Accel;
    long lastUpdate=0;
    float xValue, yValue, zValue;
    int threshold=1000;
    SQLiteDatabase myDatabase;
    final static String MY_ACTION = "MY_ACTION";
    @Override
    public void onCreate()
    {

        Log.i("check","accelerometer class");
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, Accel, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {

        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        Sensor accelSensor = event.sensor;
        if (accelSensor.getType() == Sensor.TYPE_ACCELEROMETER)
        {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            long curTime = System.currentTimeMillis();

            Intent intent = new Intent();
            intent.setAction(MY_ACTION);

            intent.putExtra("X", x);
            intent.putExtra("Y", y);
            intent.putExtra("Z", z);
            System.out.println("Service Data");

            sendBroadcast(intent);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
