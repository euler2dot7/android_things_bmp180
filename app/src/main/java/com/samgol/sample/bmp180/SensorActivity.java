package com.samgol.sample.bmp180;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;

import com.samgol.driver.bmp180.Bmp180;
import com.samgol.driver.bmp180.Bmp180SensorDriver;

import java.io.IOException;
import java.util.Arrays;

public class SensorActivity extends Activity implements SensorEventListener {
    public static final String TAG = SensorActivity.class.getSimpleName();
    private static final String I2C_PORT = "I2C1";


    private Bmp180SensorDriver mSensorDriver;
    private SensorManager mSensorManager;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerDynamicSensorCallback(new SensorManager.DynamicSensorCallback() {
            @Override
            public void onDynamicSensorConnected(Sensor sensor) {
                if (sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
                    Log.i(TAG, "Temperature sensor connected");
                    mSensorManager.registerListener(SensorActivity.this,
                            sensor, SensorManager.SENSOR_DELAY_NORMAL);
                } else if (sensor.getType() == Sensor.TYPE_PRESSURE) {
                    Log.i(TAG, "Pressure sensor connected");
                    mSensorManager.registerListener(SensorActivity.this,
                            sensor, SensorManager.SENSOR_DELAY_NORMAL);
                } else if (sensor.getType() == Sensor.TYPE_DEVICE_PRIVATE_BASE) {
                    if (sensor.getStringType().equalsIgnoreCase(Bmp180SensorDriver.BAROMETER_SENSOR)) {
                        Log.i(TAG, "Barometer sensor connected");
                        mSensorManager.registerListener(SensorActivity.this,
                                sensor, SensorManager.SENSOR_DELAY_NORMAL);
                    }
                }
            }
        });

        try {
            mSensorDriver = new Bmp180SensorDriver(I2C_PORT);
//            mSensorDriver.registerTemperatureSensor();
//            mSensorDriver.registerPressureSensor();
            mSensorDriver.registerBarometerSensor();
        } catch (IOException e) {
            Log.e(TAG, "Error configuring sensor ", e);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Closing sensor");
        if (mSensorDriver != null) {
            mSensorManager.unregisterListener(this);
            mSensorDriver.unregisterTemperatureSensor();
            mSensorDriver.unregisterPressureSensor();
            try {
                mSensorDriver.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing sensor", e);
            } finally {
                mSensorDriver = null;
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float data[] = Arrays.copyOf(event.values, 3);
        Log.i(TAG, "Sensor data: " + Arrays.toString(data));
        // for barometer sensor only
        if (data[2] != 0.0F) {
            Log.i(TAG, "Pressure: " + data[0]);
            Log.i(TAG, "Temperature: " + data[1]);
            Log.i(TAG, "Altitude: " + (Math.round(data[2] * 10) / 10.0F));
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(TAG, "sensor accuracy changed: " + accuracy);
    }

    private Bmp180 mBmp180;
    private static final String I2C_BUS = "I2C1";

    private void initSensor() {
        mBmp180 = new Bmp180(I2C_BUS);
    }

    private void readData() {
        try {
            float temp = mBmp180.readTemperature();
            float press = mBmp180.readPressure();
            double alt = mBmp180.readAltitude();
            Log.d(TAG, "loop: temp " + temp + " alt: " + alt + " press: " + press);
        } catch (IOException e) {
            Log.e(TAG, "Sensor loop  error : ", e);
        }
    }

    private void closeSensor() {
        try {
            mBmp180.close();
        } catch (IOException e) {
            Log.e(TAG, "closeSensor  error: ", e);
        } finally {
            mBmp180 = null;
        }

    }

}
