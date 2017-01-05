package com.samgol.sample.bmp180;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.samgol.driver.bmp180.Bmp180;
import com.samgol.driver.bmp180.Bmp180SensorDriver;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.samgol.sample.bmp180.SensorActivity.TAG;

public class SensorActivity extends Activity implements SensorEventListener {
    public static final String TAG = SensorActivity.class.getSimpleName();
    private static final String I2C_PORT = "I2C1";

    Bmp180Polling mBmp180Polling;

    private Bmp180SensorDriver mSensorDriver;
    private SensorManager mSensorManager;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
//        mBmp180Polling = new Bmp180Polling();
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
                }
            }
        });

        try {
            mSensorDriver = new Bmp180SensorDriver(I2C_PORT);
            mSensorDriver.registerTemperatureSensor();
            mSensorDriver.registerPressureSensor();
        } catch (IOException e) {
            Log.e(TAG, "Error configuring sensor ", e);
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
//        mBmp180Polling.end();
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
        Log.i(TAG, "sensor changed: " + event.values[0]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(TAG, "sensor accuracy changed: " + accuracy);
    }
}

class Bmp180Polling {
    private HandlerThread mHandlerThread;
    private Handler mBgHandler;
    private Bmp180 mBmp180;
    private boolean poll = true;

    Bmp180Polling() {
        mBmp180 = new Bmp180("I2C1");
        mHandlerThread = new HandlerThread("mBmp180");
        mHandlerThread.start();
        mBgHandler = new Handler(mHandlerThread.getLooper());
        mBgHandler.post(mSensorLoop);
    }


    void end() {
        poll = false;
        if (mBgHandler != null) {
            mBgHandler.removeCallbacks(mSensorLoop);
            mHandlerThread.quitSafely();
        }

        if (mBmp180 != null) {
            try {
                mBmp180.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mBmp180 = null;
        }
    }

    private Runnable mSensorLoop = new Runnable() {
        @Override
        public void run() {
            while (poll) {
                if (mBmp180 != null) {
                    try {
                        float temp = mBmp180.readTemperature();
                        float press = mBmp180.readPressure();
                        double alt = mBmp180.readAltitude();
                        Log.d(TAG, "loop: temp " + temp + " alt: " + alt + " press: " + press);
                    } catch (IOException e) {
                        Log.e(TAG, "Sensor loop  error : ", e);
                    }
                }

                try {
                    TimeUnit.MILLISECONDS.sleep(1000);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Interrupted : ", e);
                }
            }
        }
    };

}