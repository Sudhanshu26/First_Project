package com.zemosolabs.zetarget.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.util.Log;

/**
 * Created by sudhanshu on 9/10/15.
 */
public class ZContext extends ContextWrapper {

    private final String TAG ="ZeTarget.ZContext";

    private ZContext(Context ctx) {
        super(ctx);
    }

    private static ZContext zContext = null;
    private static ZResources swappedOne = null;
    private static String currentActivityName = null;

    synchronized static ZContext getInstance(Context ctx,Activity activity){
        currentActivityName = ZeTarget.getActivityClassName(activity);
        if(zContext == null){
            zContext = new ZContext(ctx);
        }
        return zContext;
    }

    @Override
    public Resources getResources() {

        if (swappedOne==null) {
            Resources orig = super.getResources();
            swappedOne = new ZResources(getAssets(),orig.getDisplayMetrics(),orig.getConfiguration(),orig);
        }
        if(ZeTarget.isDebuggingOn()) {
            Log.d(TAG, "method getResources called for" + getClass().getName());
        }
        swappedOne.setCurrentActivityName(currentActivityName);
        return swappedOne;
    }

}
