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

### You can use this driver with SensorManager

```java
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


```

```java
   @Override
    public void onSensorChanged(SensorEvent event) {
        Log.i(TAG, "sensor changed: " + event.values[0]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(TAG, "sensor accuracy changed: " + accuracy);
    }
```

### Live photo

![Alt text](/img/foto.jpg?raw=true "foto")


