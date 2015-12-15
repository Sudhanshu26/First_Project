package com.zemosolabs.zetarget.sdk;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by vedaprakash on 22/4/15.
 */
public class ShakeListener implements SensorEventListener {
    Activity currentActivity;
    private static ShakeListener instance;
    private SensorManager sensorManager;
    private Sensor accSensor;
    private long timeOfOccurence = 0;
    private long thresholdTime = 1000000000;
    private long thresholdForStartOver = 1500000000;
    private enum state {STROKE1_1,STROKE1_2,STROKE2_1, STROKE2_2, STROKE3_1, STROKE3_2,NO_STROKE,INVALID};
    private state stateOfStroke;
    private Float lastAccelerationX;
    private Float lastAccelerationY;
    private Double lastAccMag;
    private static final String TAG = "ZeTarget.ShakeListener";
    private Worker delayer = new Worker("delayer");
    private boolean calibrated;
    private int calibrationCount;

    ShakeListener(Activity activity){
        currentActivity=activity;
        stateOfStroke = state.INVALID;
        //lastAccelerationX =0.0f;
        //lastAccelerationY =0.0f;
        calibrationCount=0;
        delayer.start();
    }

    static ShakeListener getInstance(){
        if(instance==null){
            instance = new ShakeListener(ZeTargetActivityLifecycleCallbacks.currentActivity);
        }else{
            instance.currentActivity= ZeTargetActivityLifecycleCallbacks.currentActivity;
        }
        return instance;
    }
    void initialize(){
        sensorManager = (SensorManager)currentActivity.getSystemService(Context.SENSOR_SERVICE);
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        if(accSensor!=null){
            sensorManager.registerListener(this,accSensor,2);
            //Log.i(TAG,"Linear Accelerometer");
        }else{
            accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this,accSensor,3);
            //Log.i(TAG,"Raw Accelerometer");
        }
        //Log.i(TAG, "initialized");
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor==accSensor){
           // Log.i("GRAVITY",Double.valueOf(getAccelerationMagnitude(event)).toString()+" x:"+event.values[0]+" y:"+event.values[1]+" z:"+event.values[2]);

                switch (stateOfStroke) {
                    case INVALID:
                        if(accelerationEqualsExternalGravity(getAccelerationMagnitude(event))){
                            lastAccelerationX =event.values[0];
                            lastAccelerationY =event.values[1];
                            lastAccMag = getAccelerationMagnitude(event);
                            stateOfStroke=state.NO_STROKE;
                            //Log.i(TAG,"Invalid state");
                        }
                        break;
                    case NO_STROKE:
                        if(event.values[0] - lastAccelerationX>2.0) {
                            if (event.timestamp - timeOfOccurence > thresholdForStartOver) {
                                timeOfOccurence = event.timestamp;
                                stateOfStroke = state.STROKE1_1;
                                //Log.i(TAG, "stroke1_1 received");
                            }
                        }
                        break;
                    case STROKE1_1:
                        if (event.values[0] < lastAccelerationX && event.values[1] < lastAccelerationY &&
                                (!accelerationEqualsExternalGravity(lastAccMag)||!accelerationEqualsExternalGravity(getAccelerationMagnitude(event))) ) {
                            if (event.timestamp - timeOfOccurence < thresholdTime) {
                                timeOfOccurence = event.timestamp;
                                stateOfStroke = state.STROKE1_2;
                                //Log.i(TAG, "stroke1_2 received");
                            }
                        } else {
                            if (event.timestamp - timeOfOccurence > thresholdForStartOver) {
                                stateOfStroke = state.INVALID;
                                //Log.i(TAG, "time lag in the strokes");
                            } else {
                                // Log.i(TAG,"Waiting for stroke1_2");
                            }
                        }
                        //Log.i(TAG,"STROKE1: "+"\nx: "+Float.valueOf(event.values[0]).toString());
                        break;
                    case STROKE1_2:
                        if (event.values[0] > lastAccelerationX && event.values[1] > lastAccelerationY
                            && (!accelerationEqualsExternalGravity(lastAccMag)||!accelerationEqualsExternalGravity(getAccelerationMagnitude(event))) ) {
                            if (event.timestamp - timeOfOccurence < thresholdTime) {
                                timeOfOccurence = event.timestamp;
                                stateOfStroke = state.STROKE2_1;
                                //Log.i(TAG, "stroke2_1 received");
                            }
                        } else {
                            if (event.timestamp - timeOfOccurence > thresholdForStartOver) {
                                stateOfStroke = state.INVALID;
                                //Log.i(TAG, "time lag in the strokes");
                            } else {
                                // Log.i(TAG,"Waiting for stroke2_1");
                            }

                        }
                        //Log.i(TAG,"STROKE2: "+"\nx: "+Float.valueOf(event.values[0]).toString()+"\ny: "+Float.valueOf(event.values[1]).toString());
                        break;
                    case STROKE2_1:
                        if (event.values[0] < lastAccelerationX && event.values[1] < lastAccelerationY &&
                                (!accelerationEqualsExternalGravity(lastAccMag)||!accelerationEqualsExternalGravity(getAccelerationMagnitude(event))) ) {
                            if (event.timestamp - timeOfOccurence < thresholdTime) {
                                timeOfOccurence = event.timestamp;
                                stateOfStroke = state.STROKE2_2;
                                //Log.i(TAG, "stroke2_2 received");
                            }
                        } else {
                            if (event.timestamp - timeOfOccurence > thresholdForStartOver) {
                                stateOfStroke = state.INVALID;
                                //Log.i(TAG, "time lag in the strokes");
                            } else {
                                // Log.i(TAG,"Waiting for stroke2_2");
                            }
                        }
                        // Log.i(TAG,"STROKE2: "+"\nx: "+Float.valueOf(event.values[0]).toString()+"\ny: "+Float.valueOf(event.values[1]).toString());
                        break;
                    case STROKE2_2:
                        if (event.values[0] > lastAccelerationX &&
                                (!accelerationEqualsExternalGravity(lastAccMag)||!accelerationEqualsExternalGravity(getAccelerationMagnitude(event))) ) {
                            if (event.timestamp - timeOfOccurence < thresholdTime) {
                                timeOfOccurence = event.timestamp;
                                stateOfStroke = state.STROKE3_1;
                                Log.i(TAG, "stroke3_1 received");
                            }
                        } else {
                            if (event.timestamp - timeOfOccurence > thresholdForStartOver) {
                                stateOfStroke = state.INVALID;
                                //Log.i(TAG, "time lag in the strokes");
                            } else {
                                // Log.i(TAG,"Waiting for stroke3_1");
                            }

                        }
                        // Log.i(TAG,"STROKE3: "+"\nx: "+Float.valueOf(event.values[0]).toString());
                        break;
                    case STROKE3_1:
                        if (event.values[0] <= lastAccelerationX &&
                                (!accelerationEqualsExternalGravity(lastAccMag)||!accelerationEqualsExternalGravity(getAccelerationMagnitude(event))) ) {
                            if (event.timestamp - timeOfOccurence < thresholdTime) {
                                timeOfOccurence = event.timestamp;
                                stateOfStroke = state.STROKE3_2;

                                //Log.i(TAG, "stroke3_2 received");
                                doActionWhenZfound();
                            }
                        } else {
                            if (event.timestamp - timeOfOccurence > thresholdForStartOver) {
                                stateOfStroke = state.INVALID;

                                //Log.i(TAG, "time lag in the strokes");
                            } else {
                                // Log.i(TAG,"Waiting for stroke3_2");
                            }

                        }
                        // Log.i(TAG,"STROKE3: "+Float.valueOf(event.values[0]).toString());

                        break;
                }
                lastAccelerationX =event.values[0];
                lastAccelerationY =event.values[1];
                lastAccMag = getAccelerationMagnitude(event);
        }
    }

    private boolean accelerationEqualsExternalGravity(double accel) {
        if(accel>9.5&&accel<10.1){
            return true;
        }else{
            return false;
        }
    }

    private double getAccelerationMagnitude(SensorEvent event) {
        double accelMag = Math.sqrt(event.values[0]*event.values[0]+event.values[1]*event.values[1]+event.values[2]*event.values[2]);
        return accelMag;
    }

    private void doActionWhenZfound() {

        //Log.i(TAG, "Z motion found");
        if(ZeTarget.isDebuggingOn()) {
            ScreenCapture screenCapturer = ScreenCapture.getInstance();
            screenCapturer.initialize();
            screenCapturer.captureAndSend();
        }
        Toast.makeText(currentActivity,"Z motion detected, capturing and sending screen details. Please Wait 2 sec before another attempt",Toast.LENGTH_SHORT).show();
        delayer.postDelayed(new Runnable() {
            @Override
            public void run() {
                stateOfStroke=state.INVALID;
            }
        },1500L);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    void purge(){
        sensorManager.unregisterListener(this);
        //Log.i(TAG,"sensorManager unregistered");
    }
}
