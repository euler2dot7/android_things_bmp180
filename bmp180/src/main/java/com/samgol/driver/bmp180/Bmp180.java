package com.samgol.driver.bmp180;

import android.support.annotation.IntDef;
import android.util.Log;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;
import java.lang.annotation.Retention;


import static java.lang.annotation.RetentionPolicy.SOURCE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/*
 * Altitude, Pressure, Temperature
 */
public class Bmp180 implements AutoCloseable {
    private static final String TAG = Bmp180.class.getSimpleName();
    // You can enable debug output to log
    //DEBUG = true
    private static final boolean DEBUG = false;
    // You can use the datasheet values to test the conversion results
    // boolean DEBUG_VALUES = true;
    private static final boolean DEBUG_VALUES = false;
    private static final int MIN_PERIOD_US = 50;
    public final static int BMP180_ADDRESS = 0x77;

    @Retention(SOURCE)
    @IntDef({BMP180_ULTRALOWPOWER, BMP180_STANDARD, BMP180_HIGHRES, BMP180_ULTRAHIGHRES})
    public @interface Mode {
    }

    private static final double POW_FACT = 0.1903;

    final static int BMP180_ULTRALOWPOWER = 0;
    final static int BMP180_STANDARD = 1;
    final static int BMP180_HIGHRES = 2;
    final static int BMP180_ULTRAHIGHRES = 3;


    static final float MAX_FREQ_HZ = 181f;
    static final float MIN_FREQ_HZ = 23.1f;

    static final float MAX_POWER_CONSUMPTION_TEMP_UA = 325f;
    static final float MAX_POWER_CONSUMPTION_PRESSURE_UA = 720f;

    public static final float MIN_TEMP_C = -40f;
    static final float MAX_TEMP_C = 85f;

    static final float MAX_PRESSURE_HPA = 1100f;

    private final static int BMP180_CAL_AC1 = 0xAA;
    private final static int BMP180_CAL_AC2 = 0xAC;
    private final static int BMP180_CAL_AC3 = 0xAE;
    private final static int BMP180_CAL_AC4 = 0xB0;
    private final static int BMP180_CAL_AC5 = 0xB2;
    private final static int BMP180_CAL_AC6 = 0xB4;
    private final static int BMP180_CAL_B1 = 0xB6;
    private final static int BMP180_CAL_B2 = 0xB8;
    private final static int BMP180_CAL_MB = 0xBA;
    private final static int BMP180_CAL_MC = 0xBC;
    private final static int BMP180_CAL_MD = 0xBE;

    private final static int BMP180_CONTROL = 0xF4;
    private final static int BMP180_TEMPDATA = 0xF6;
    private final static int BMP180_PRESSUREDATA = 0xF6;
    private final static int BMP180_READTEMPCMD = 0x2E;
    private final static int BMP180_READPRESSURECMD = 0x34;

    private int cal_AC1 = 0;
    private int cal_AC2 = 0;
    private int cal_AC3 = 0;
    private int cal_AC4 = 0;
    private int cal_AC5 = 0;
    private int cal_AC6 = 0;
    private int cal_B1 = 0;
    private int cal_B2 = 0;
    private int cal_MB = 0;
    private int cal_MC = 0;
    private int cal_MD = 0;


    private I2cDevice mDevice;

    private int mode = BMP180_STANDARD;

    private long rawTempTime = 0;
    private int rawTempVal = 0;
    private long rawPressureTime = 0;
    private int rawPresureVal = 0;

    public Bmp180(String i2cName) {
        try {
            mDevice = new PeripheralManagerService().openI2cDevice(i2cName, BMP180_ADDRESS);
            try {
                this.readCalibrationData();
            } catch (Exception e) {
                Log.e(TAG, "Bmp180 Error: ", e);
            }
        } catch (IOException e) {
            Log.e(TAG, "Bmp180 Error: ", e);
        }
    }

    public Bmp180(I2cDevice i2cDevice) {
        mDevice = i2cDevice;
        try {
            this.readCalibrationData();
        } catch (Exception e) {
            Log.e(TAG, "Bmp180 Error: ", e);
        }
    }


    public void setMode(@Mode int mode) {
        this.mode = mode;
    }

    private int readU16(int register) throws IOException {
        return I2cUtils.readU16BE(this.mDevice, register);
    }

    private int readS16(int register) throws IOException {
        return I2cUtils.readS16BE(this.mDevice, register);
    }

    private void readCalibrationData() throws Exception {
        // Reads the calibration data from the IC
        cal_AC1 = readS16(BMP180_CAL_AC1);
        cal_AC2 = readS16(BMP180_CAL_AC2);
        cal_AC3 = readS16(BMP180_CAL_AC3);
        cal_AC4 = readU16(BMP180_CAL_AC4);
        cal_AC5 = readU16(BMP180_CAL_AC5);
        cal_AC6 = readU16(BMP180_CAL_AC6);
        cal_B1 = readS16(BMP180_CAL_B1);
        cal_B2 = readS16(BMP180_CAL_B2);
        cal_MB = readS16(BMP180_CAL_MB);
        cal_MC = readS16(BMP180_CAL_MC);
        cal_MD = readS16(BMP180_CAL_MD);
        showCalibrationData();
    }

    private void showCalibrationData() {
        // Displays the calibration values for debugging purposes
        printDbg("AC1 = " + cal_AC1);
        printDbg("AC2 = " + cal_AC2);
        printDbg("AC3 = " + cal_AC3);
        printDbg("AC4 = " + cal_AC4);
        printDbg("AC5 = " + cal_AC5);
        printDbg("AC6 = " + cal_AC6);
        printDbg("B1  = " + cal_B1);
        printDbg("B2  = " + cal_B2);
        printDbg("MB  = " + cal_MB);
        printDbg("MC  = " + cal_MC);

    }

    private void printDbg(String msg) {
        if (DEBUG)
            Log.d(TAG, "printDbg: " + msg);
    }

    private int readRawTemp() throws IOException {
        // Reads the raw (uncompensated) temperature from the sensor
        if (System.currentTimeMillis() - rawTempTime < MIN_PERIOD_US) {
            return rawTempVal;
        }

        mDevice.writeRegByte(BMP180_CONTROL, (byte) BMP180_READTEMPCMD);
        waitFor(5);
        int raw = readU16(BMP180_TEMPDATA);

        printDbg("Raw Temp: " + (raw & 0xFFFF) + ", " + raw);

        rawTempTime = System.currentTimeMillis();
        rawTempVal = raw;
        return raw;
    }

    private int readRawPressure() throws IOException {

        if (System.currentTimeMillis() - rawPressureTime < MIN_PERIOD_US) {
            return rawPresureVal;
        }

        // Reads the raw (uncompensated) pressure level from the sensor
        mDevice.writeRegByte(BMP180_CONTROL, (byte) (BMP180_READPRESSURECMD + (this.mode << 6)));
        if (this.mode == BMP180_ULTRALOWPOWER)
            waitFor(5);
        else if (this.mode == BMP180_HIGHRES)
            waitFor(14);
        else if (this.mode == BMP180_ULTRAHIGHRES)
            waitFor(26);
        else
            waitFor(8);
        int msb = mDevice.readRegByte(BMP180_PRESSUREDATA);
        int lsb = mDevice.readRegByte(BMP180_PRESSUREDATA + 1);
        int xlsb = mDevice.readRegByte(BMP180_PRESSUREDATA + 2);
        int raw = ((msb << 16) + (lsb << 8) + xlsb) >> (8 - this.mode);

        printDbg("Raw Pressure: " + (raw & 0xFFFF) + ", " + raw);
        rawPressureTime = System.currentTimeMillis();
        rawPresureVal = raw;
        return raw;
    }

    private float temperatureVal;
    private long temperatureTime;

    public synchronized float readTemperature() throws IOException {

        if (System.currentTimeMillis() - temperatureTime < MIN_PERIOD_US) {
            return temperatureVal;
        }
        // Gets the compensated temperature in degrees celsius
        int UT, X1, X2, B5;
        float temp;

        // Read raw temp before aligning it with the calibration values
        UT = this.readRawTemp();
        X1 = ((UT - this.cal_AC6) * this.cal_AC5) >> 15;
        X2 = (this.cal_MC << 11) / (X1 + this.cal_MD);
        B5 = X1 + X2;
        temp = ((B5 + 8) >> 4) / 10.0f;

        printDbg("Calibrated temperature = " + temp + " C");
        temperatureTime = System.currentTimeMillis();
        temperatureVal = temp;

        return temp;
    }

    private int pressureVal;
    private long pressureTime;

    public synchronized int readPressure() throws IOException {
        // Gets the compensated pressure in pascal
        if (System.currentTimeMillis() - pressureTime < MIN_PERIOD_US) {
            return pressureVal;
        }

        int UT, UP, B3, B5, B6, X1, X2, X3;
        long B4, B7, p;


        UT = this.readRawTemp();
        UP = this.readRawPressure();

        if (DEBUG_VALUES) {
            UT = 27898;
            UP = 23843;
            this.cal_AC6 = 23153;
            this.cal_AC5 = 32757;
            this.cal_MB = -32768;
            this.cal_MC = -8711;
            this.cal_MD = 2868;
            this.cal_B1 = 6190;
            this.cal_B2 = 4;
            this.cal_AC3 = -14383;
            this.cal_AC2 = -72;
            this.cal_AC1 = 408;
            this.cal_AC4 = 32741;
            this.mode = BMP180_ULTRALOWPOWER;

            this.showCalibrationData();
        }
        // True Temperature Calculations
        X1 = ((UT - this.cal_AC6) * this.cal_AC5) >> 15;
        X2 = (this.cal_MC << 11) / (X1 + this.cal_MD);
        B5 = X1 + X2;

        printDbg("X1 = " + X1);
        printDbg("X2 = " + X2);
        printDbg("B5 = " + B5);
        printDbg("True Temperature = " + (((B5 + 8) >> 4) / 10.0) + " C");

        // Pressure Calculations
        B6 = B5 - 4000;
        X1 = (this.cal_B2 * (B6 * B6) >> 12) >> 11;
        X2 = (this.cal_AC2 * B6) >> 11;
        X3 = X1 + X2;
        B3 = (((this.cal_AC1 * 4 + X3) << this.mode) + 2) / 4;

        printDbg("B6 = " + B6);
        printDbg("X1 = " + X1);
        printDbg("X2 = " + X2);
        printDbg("X3 = " + X3);
        printDbg("B3 = " + B3);

        X1 = (this.cal_AC3 * B6) >> 13;
        X2 = (this.cal_B1 * ((B6 * B6) >> 12)) >> 16;
        X3 = ((X1 + X2) + 2) >> 2;
        B4 = (this.cal_AC4 * (X3 + 32768)) >> 15;
        B7 = (UP - B3) * (50000 >> this.mode);

        printDbg("X1 = " + X1);
        printDbg("X2 = " + X2);
        printDbg("X3 = " + X3);
        printDbg("B4 = " + B4);
        printDbg("B7 = " + B7);


        if (B7 < 0x80000000) {
            p = (B7 * 2) / B4;
        } else {
            p = (B7 / B4) * 2;
        }

        printDbg("X1 = " + X1);

        X1 = (int) ((p >> 8) * (p >> 8));
        X1 = (X1 * 3038) >> 16;
        X2 = (int) (-7357 * p) >> 16;

        printDbg("p  = " + p);
        printDbg("X1 = " + X1);
        printDbg("X2 = " + X2);

        p = p + ((X1 + X2 + 3791) >> 4);

        printDbg("Pressure = " + p + " Pa");

        pressureTime = System.currentTimeMillis();
        pressureVal = (int) p;
        return pressureVal;
    }

    private int standardSeaLevelPressure = 103425;

    public synchronized void setStandardSeaLevelPressure(int standardSeaLevelPressure) {
        this.standardSeaLevelPressure = standardSeaLevelPressure;
    }

    public synchronized float readAltitude() throws IOException {
        float pressure = readPressure();
        return (float) (44330.0 * (1.0 - Math.pow(pressure / standardSeaLevelPressure, POW_FACT)));


    }

    private static void waitFor(long howMuch) {
        try {
            MILLISECONDS.sleep(howMuch);
        } catch (InterruptedException e) {
            Log.e(TAG, "waitFor: ", e);
        }
    }

    @Override
    public synchronized void close() throws IOException {
        if (mDevice != null) {
            try {
                mDevice.close();
            } finally {
                mDevice = null;
            }
        }
    }
}

//
//    public static void main(String[] args)  {
//        final NumberFormat NF = new DecimalFormat("##00.00");
//        Bmp180 sensor = new Bmp180("I2C1");
//        float press = 0;
//        float temp = 0;
//        double alt = 0;
//
//        try {
//            press = sensor.readPressure();
//        } catch (Exception ex) {
//            System.err.println(ex.getMessage());
//            ex.printStackTrace();
//        }
//        sensor.setStandardSeaLevelPressure((int) press); // As we ARE at the sea level (in San Francisco).
//        try {
//            alt = sensor.readAltitude();
//        } catch (Exception ex) {
//            System.err.println(ex.getMessage());
//            ex.printStackTrace();
//        }
//        try {
//            temp = sensor.readTemperature();
//        } catch (Exception ex) {
//            System.err.println(ex.getMessage());
//            ex.printStackTrace();
//        }
//
//        Log.d(TAG,"Temperature: " + NF.format(temp) + " C");
//        Log.d(TAG,"Pressure   : " + NF.format(press / 100) + " hPa");
//        Log.d(TAG,"Altitude   : " + NF.format(alt) + " m");
//
//    }