package com.zemosolabs.zetarget.sdk;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by vedaprakash on 30/3/15.
 */
public class TripleTapListener implements View.OnTouchListener {
    private int tapCount;
    private long delay = 1000L;
    private long lastTimestamp;

    TripleTapListener(){
        //Log.i("TripleTapListener","Created");
        tapCount = 0;
        lastTimestamp = 0;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        long currentTime = System.currentTimeMillis();
        //Log.i("TOUCH:","Detected");
        if(event.getAction()==MotionEvent.ACTION_DOWN) {
            //Log.i("TAP:","Single time");
            if (tapCount == 0) {
                lastTimestamp = currentTime;
                tapCount++;
            } else {
                if ((currentTime - lastTimestamp) < delay) {
                    lastTimestamp = currentTime;
                    tapCount++;
                } else
                    tapCount = 0;
            }
            if (tapCount == 2) {
                if(ZeTarget.isDebuggingOn()) {
                    ShakeListener.getInstance().purge();
                    ScreenCapture screenCapturer = ScreenCapture.getInstance();
                    screenCapturer.initialize();
                    screenCapturer.captureAndSend();

                }
            }
        }
        return false;
    }
}
