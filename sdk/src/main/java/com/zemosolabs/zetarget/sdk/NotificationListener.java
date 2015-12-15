package com.zemosolabs.zetarget.sdk;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.zemosolabs.zetarget.sdk.Constants;
import com.zemosolabs.zetarget.sdk.ZeTarget;

import org.json.JSONException;
import org.json.JSONObject;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class NotificationListener extends NotificationListenerService {
    private final String TAG = "ZeTarget.NotifiListener";
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        String packageName = sbn.getPackageName();
        if(packageName!=getApplicationContext().getPackageName()){
            Log.d(TAG,"Package Name not same "+packageName+", "+getApplicationContext().getPackageName());
            return;
        }
        String campaignId = sbn.getNotification().extras.getString("campaignId");
        JSONObject promoEvent = new JSONObject();
        try {
            promoEvent.put("campaignId",campaignId);
        } catch (JSONException e) {
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG,"campaign_id writing into event failed");
            }
        }
        ZeTarget.uncheckedLogEvent(Constants.Z_CAMPAIGN_VIEWED_EVENT, promoEvent);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        String packageName = sbn.getPackageName();
        if(packageName!=getApplicationContext().getPackageName()){
            Log.d(TAG,"Package Name not same "+packageName+", "+getApplicationContext().getPackageName());
            return;
        }
        String campaignId = sbn.getNotification().extras.getString("campaignId");
        JSONObject promoEvent = new JSONObject();
        try {
            promoEvent.put("campaignId",campaignId);
        } catch (JSONException e) {
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG,"campaign_id writing into event failed");
            }
        }
        ZeTarget.uncheckedLogEvent(Constants.Z_CAMPAIGN_REJECTED_EVENT, promoEvent);
    }
}
