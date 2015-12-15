package com.zemosolabs.zetarget.sdk;

import android.content.Context;
import android.os.Vibrator;

/**
 * Created by sudhanshu on 15/12/15.
 */
public class VibrationManager {

    private static VibrationManager vm;
    private Context context;

    Vibrator v = null;

    public Vibrator getVibrator(){
        if(v == null){
            v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }
        return v;
    }

    public static VibrationManager getManager(Context context) {
        if(vm == null){
            vm = new VibrationManager();
        }
        vm.setContext(context);
        return vm;
    }

    private void setContext(Context context){
        this.context = context;
    }

    public void vibrate(long pattern){
        v.vibrate(pattern);

    }








}
