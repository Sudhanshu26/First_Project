package com.zemosolabs.zetarget.sdk;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by praveen on 30/01/15.
 */
public class ZeTargetActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "ZeTarget.ZALcycleCbcks";
    static Activity currentActivity;
    private ScreenEditor UIEditor;
    private ShakeListener shakeListener=null;
    TripleTapListener tripleTapListener;

    public ZeTargetActivityLifecycleCallbacks() {

    }

    @Override
    public void onActivityStarted(final Activity activity) {
        currentActivity = activity;
        ZeTarget.currentActivity = activity;
    }


    @Override
    public void onActivityCreated(final Activity activity, Bundle savedInstanceState) {
        Intent startingIntent = activity.getIntent();
        if(startingIntent.hasExtra(Constants.Z_EVENT_TYPE)){
            if(startingIntent.getStringExtra(Constants.Z_EVENT_TYPE).equals(Constants.Z_CAMPAIGN_VIEWED_EVENT)){
                String campaignId = startingIntent.getStringExtra("campaignId");
                if(startingIntent.getStringExtra(Constants.Z_CAMPAIGN_TYPE).equals(Constants.Z_CAMPAIGN_TYPE_GEOCAMPAIGN)){
                    String geofenceId = startingIntent.getStringExtra("geofenceId");
                    JSONObject properties = new JSONObject();
                    try {
                        properties.put("campaignId",campaignId);
                        properties.put("geofenceId",geofenceId);
                    } catch (JSONException e) {
                        if(ZeTarget.isDebuggingOn()){
                            Log.e(TAG,"GeoNotification update failed",e);
                        }
                    }
                    ZeTarget.logEvent(Constants.Z_CAMPAIGN_VIEWED_EVENT, properties);
                }else if(startingIntent.getStringExtra(Constants.Z_CAMPAIGN_TYPE).equals(Constants.Z_CAMPAIGN_TYPE_SIMPLE_EVENT_CAMPAIGN)){
                    JSONObject properties = new JSONObject();
                    try {
                        properties.put("campaignId",campaignId);
                    } catch (JSONException e) {
                        if(ZeTarget.isDebuggingOn()){
                            Log.e(TAG,"SimpleEvent Notification update failed",e);
                        }
                    }
                    ZeTarget.logEvent(Constants.Z_CAMPAIGN_VIEWED_EVENT, properties);
                }
            }
        }
        activity.findViewById(android.R.id.content).addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
                                       int oldTop, int oldRight, int oldBottom) {
                if(ZeTarget.isDebuggingOn()){
                    Log.d(TAG,"LayoutChange called for view:"+v);
                }
                if(ZeTarget.needTocallsetText.getAndSet(false)) {
                    ZeTarget.setText(activity);
                }
            }
        });
    }

    @Override
    public void onActivityPaused(Activity activity) {
        ZeTarget._endSession();
        if(UIEditor!=null) {
            UIEditor.purge();
        }
        if(shakeListener!=null) {
            shakeListener.purge();
        }
    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }

    @Override
    public void onActivityResumed(Activity activity) {
        ZeTarget.setText(activity);

        String packageName = activity.getPackageName();
        PackageManager pm = activity.getPackageManager();

        Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
        if(launchIntent!=null) {
            ComponentName compName = launchIntent.getComponent();
            String launchingClassName = compName.getClassName();
            if (activity.getClass().getCanonicalName().equals(launchingClassName)) {
                //Log.i("LaunchingActivity","Launched");
                //if (currentActivity != null && activity == currentActivity) {
                    String campaignId = activity.getIntent()
                            .getStringExtra(Constants.Z_BUNDLE_KEY_PUSH_NOTIFICATION_CAMPAIGN_ID);
                    if (campaignId != null && !campaignId.isEmpty()) {
                        JSONObject promotionEvent = new JSONObject();
                        try {
                            promotionEvent.put("campaignId",campaignId);
                        } catch (JSONException e) {
                            if(ZeTarget.isDebuggingOn()){
                                Log.e(TAG,"PUSH VIEWED EVENT CREATION FAILURE",e);
                            }
                        }
                        ZeTarget.updatePromotionAsSeen(promotionEvent);
                        GcmIntentService.notificationCount=0;
                        //Log.i("PushNotificationViewed", campaignId);
                    }
               // }
            }
        }
        /*PackageManager pm = activity.getPackageManager();*/
        String name = activity.getComponentName().getClassName();
        String label = null;
        try {
            label = pm.getActivityInfo(activity.getComponentName(),0).loadLabel(pm).toString();
        } catch (PackageManager.NameNotFoundException e) {
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG,"PackageManager Not Found in ActivityLifeCycles",e);
            }
        }

        if(ZeTarget.isDebuggingOn()){
            Log.d(TAG,"updateActivityDetails with label:"+label);
        }
        ZeTarget.updateActivityDetails(label, name);
        ZeTarget._startSession(activity);

        if(ZeTarget.isDebuggingOn()) {
            shakeListener = ShakeListener.getInstance();
            shakeListener.initialize();
            tripleTapListener= new TripleTapListener();
            activity.getWindow().getDecorView().setOnTouchListener(tripleTapListener);
        }

        /*UIEditor = ScreenEditor.getInstance(activity);
        UIEditor.edit();*/

    }

    @Override
    public void onActivityStopped(Activity activity) { }
}
