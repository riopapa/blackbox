package com.urrecliner.blackbox;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import static android.content.Context.SENSOR_SERVICE;
import static android.hardware.Sensor.TYPE_ACCELEROMETER;
import static com.urrecliner.blackbox.Vars.azimuth;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mContext;
import static com.urrecliner.blackbox.Vars.startStopExit;

public class DirectionSensor implements SensorEventListener{

    private String logID = "DirSensor";
    private SensorManager sensorManager;
    private Sensor rotationVector, accelerometer, magnetometer;
    private float[] rotationMatrix = new float[9];
    private float[] orientation = new float[9];
    private float[] lastAccelerometer = new float[3];
    private float[] lastMagnetometer = new float[3];
    private boolean haveSensor = false, haveSensor2 = false;
    private boolean lastAccelerometerSet = false;
    private boolean lastMagnetometerSet = false;

    public void start(){
        sensorManager = (SensorManager)mActivity.getSystemService(SENSOR_SERVICE);

        if(sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) == null) {
            if(sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) == null || sensorManager.getDefaultSensor(TYPE_ACCELEROMETER) == null) {
                noSensorAlert();
            }
            else {
                accelerometer = sensorManager.getDefaultSensor(TYPE_ACCELEROMETER);
                magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

                haveSensor = sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                haveSensor2 = sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
        else {
            rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            haveSensor = sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int type = event.sensor.getType();
        if( type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            azimuth = (float)(Math.toDegrees(SensorManager.getOrientation(rotationMatrix, orientation)[0]) + 360 + 90);
        }
        else if(event.sensor.getType() == TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.length);
            lastAccelerometerSet = true;
        }
        else if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.length);
            lastMagnetometerSet = true;
        }
        if(lastAccelerometerSet && lastMagnetometerSet) {
            SensorManager.getRotationMatrix(rotationMatrix, null, lastAccelerometer, lastMagnetometer);
            SensorManager.getOrientation(rotationMatrix, orientation);
            azimuth = (float)(Math.toDegrees(SensorManager.getOrientation(rotationMatrix, orientation)[0]) + 360 + 90);
        }

        azimuth = Math.round(azimuth);

//        utils.log(logID, String.format(Locale.getDefault(),"%d° %s", azimuth, where));
    }

    void stop(){
        if(haveSensor && haveSensor2){
            sensorManager.unregisterListener(this, accelerometer);
            sensorManager.unregisterListener(this, magnetometer);
        }
        else if(haveSensor) {
            sensorManager.unregisterListener(this, rotationVector);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
//        if ((!haveSensor || sensor.getType() != Sensor.TYPE_ROTATION_VECTOR)
//                && (!haveSensor2 || (sensor.getType() != Sensor.TYPE_ACCELEROMETER
//                && sensor.getType() != Sensor.TYPE_MAGNETIC_FIELD))) {
//            return;
//        }
//
//        if (accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW && !calibrateVid.isPlaying()) {
//            calibrateVid.setVisibility(View.VISIBLE);
//            compassImg.setVisibility(View.INVISIBLE);
//            azimuthTxt.setVisibility(View.INVISIBLE);
//            calibrateVid.start();
//        }
//        else {
//            calibrateVid.setVisibility(View.INVISIBLE);
//            compassImg.setVisibility(View.VISIBLE);
//            azimuthTxt.setVisibility(View.VISIBLE);
//            calibrateVid.stopPlayback();
//        }
    }

    private void noSensorAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
        alertDialog.setMessage("Your device does not support the simply_compass.")
                .setCancelable(false)
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        startStopExit.exitBlackBoxApp();
                    }
                });
    }
}
