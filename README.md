# android_things_bmp180
Android Things Bosh BMP85/BMP180 Driver Example

##Driver for BMP85/BMP180 for Android Things 

##Example how to use BMP85/BMP180 with Android Things 

### BMP180 sensor connection 


![Alt text](/img/BMP180.png?raw=true "I2C connection")


### One  can use the sensor directly without SensorManager

```java

    private Bmp180 mBmp180;
    private static final String I2C_BUS = "I2C1";
    
    private void initSensor(){
        mBmp180 = new Bmp180(I2C_BUS);
    }

    private void readData(){
        try {
            float temp = mBmp180.readTemperature();
            float press = mBmp180.readPressure();
            double alt = mBmp180.readAltitude();
            Log.d(TAG, "loop: temp " + temp + " alt: " + alt + " press: " + press);
        } catch (IOException e) {
            Log.e(TAG, "Sensor loop  error : ", e);
        }
    }

    private void closeSensor(){
        try {
            mBmp180.close();
        } catch (IOException e) {
            Log.e(TAG, "closeSensor  error: ", e);
        }
        mBmp180 = null;
    }

```

### You can also use this driver with SensorManager

Registers the sensor and attach the listener
```java
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerDynamicSensorCallback(new SensorManager.DynamicSensorCallback() {
            @Override
            public void onDynamicSensorConnected(Sensor sensor) {
                if (sensor.getType() == Sensor.TYPE_DEVICE_PRIVATE_BASE) {
                    if (sensor.getStringType().equalsIgnoreCase(Bmp180SensorDriver.BAROMETER_SENSOR)) {
                        Log.i(TAG, "Barometer sensor connected");
                        mSensorManager.registerListener(mListener,
                                sensor, SensorManager.SENSOR_DELAY_NORMAL);
                    }
                }
            }
	});


        try {
            mSensorDriver = new Bmp180SensorDriver(I2C_PORT);
            mSensorDriver.registerBarometerSensor();
        } catch (IOException e) {
            Log.e(TAG, "Error configuring sensor ", e);
	}


```

Receives data through SensorEventListener
```java
   @Override
    public void onSensorChanged(SensorEvent event) {
       float data[] = Arrays.copyOf(event.values, 3);
       Log.i(TAG, "Pressure: " + data[0]);
       Log.i(TAG, "Temperature: " + data[1]);
       Log.i(TAG, "Altitude: " + (Math.round(data[2] * 10) / 10.0F));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(TAG, "sensor accuracy changed: " + accuracy);
    }
```

### Live photo

![Alt text](/img/foto.jpg?raw=true "photo")


