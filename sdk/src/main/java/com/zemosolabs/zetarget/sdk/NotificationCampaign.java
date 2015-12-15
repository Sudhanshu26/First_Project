package com.zemosolabs.zetarget.sdk;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by vedaprakash on 8/5/15.
 */
abstract class NotificationCampaign {
    private static final String TAG = "ZeTarget.NotificationCampaign";
    protected long campaignEndTime;
    protected long campaignStartTime;
    protected long uniqueId;
    protected String campaignId;
    protected String campaignType;
    protected NotificationManager notifier;
    protected JSONObject template;
    protected boolean checkServerBeforeNotifying;
    protected int notificationId;
    protected int maximumNumberOfTimesToShow;
    protected int minimumMinutesBeforeReshow;
    protected String launchClassName = null;

    protected NotificationCampaign(JSONObject currentCampaign, int notificationId){
        try {
            /*if(ZeTarget.robolectricTesting) {
                System.out.println("Notification Campaign: Being constructed");
            }*/
            campaignId = currentCampaign.getString("campaignId");
            campaignType = currentCampaign.getString("type");
            campaignStartTime = currentCampaign.getLong("campaignStartTime");
            campaignEndTime = currentCampaign.getLong("campaignEndTime");
            uniqueId = currentCampaign.getInt("rowIdInTable");
            template = currentCampaign.getJSONObject("template");
            JSONObject suppressionLogic = currentCampaign.getJSONObject("suppressionLogic");
            maximumNumberOfTimesToShow = suppressionLogic.getInt("maximumNumberOfTimesToShow");
            minimumMinutesBeforeReshow = suppressionLogic.getInt("minimumDurationInMinutesBeforeReshow");
            checkServerBeforeNotifying = currentCampaign.getBoolean("checkServerBeforeNotifying");
            JSONObject nextScreen,contentClick;
            String launcherClassName;
            if(template.has("contentClick") && (contentClick = template.getJSONObject("contentClick"))!=JSONObject.NULL){
                if(contentClick.has("nextScreen") && (nextScreen = contentClick.getJSONObject("nextScreen"))!=JSONObject.NULL){
                    if(nextScreen.has("deepLink") && !(launcherClassName = nextScreen.getString("deepLink")).isEmpty()){
                        launchClassName = launcherClassName;
                        /*if(ZeTarget.robolectricTesting) {
                            System.out.println("Notification Campaign: " + launcherClassName);
                        }*/
                    }
                }
            }
        }catch (JSONException e){
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "campaign json inflation error", e);
            }
            /*if(ZeTarget.robolectricTesting) {
                e.printStackTrace();
            }*/
        }
        this.notificationId = notificationId;
    }

    void show(Context context,String details,long timeStamp){
        long lastShownTime = DbHelper.getDatabaseHelper(context).getLastShownTime(campaignId);
        if(lastShownTime>0){
            //Log.i(TAG, timeStamp+" "+(lastShownTime+minimumMinutesBeforeReshow*6000));
            if(timeStamp<lastShownTime+ minimumMinutesBeforeReshow *60000){
                //Log.i(TAG, "not yet to be shown");
                return;
            }
        }

        notifier = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        int appIconId = context.getApplicationInfo().icon;
        String title=null,message=null;
        try {
            title = template.getString("title");
            message = template.getString("message");
        } catch (JSONException e) {
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG,"title and message error",e);
            }
        }
        if(title!=null&&message!=null) {
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context).setSmallIcon(appIconId)
                    .setContentTitle(title).setContentText(message).setAutoCancel(true);
            Class<?> launcherClass = null;
            try {
                if(launchClassName!=null) {
                    launcherClass = Class.forName(launchClassName);
                }else{
                    launcherClass = Class.forName(context.getPackageManager().getLaunchIntentForPackage(context.getPackageName()).getComponent().getClassName());
                }
            } catch (ClassNotFoundException e) {
                if(ZeTarget.isDebuggingOn()){
                    Log.e(TAG,"launcher class not found",e);
                }
            }
            Intent launchIntent = null;
            if(launcherClass!=null) {
                 launchIntent = new Intent(context, launcherClass);
            }else{
                launchIntent = context.getApplicationContext().getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            }
            if(launchIntent!=null){
                launchIntent = addExtrasToIntent(launchIntent,details);
            }
            PendingIntent pendingIntent = PendingIntent.getActivity(context,0,launchIntent,0);
            notificationBuilder.setContentIntent(pendingIntent);
            notifier.notify(notificationId,notificationBuilder.build());
            DbHelper.getDatabaseHelper(context).updateCampaign(campaignId,timeStamp);
        }
    }

    abstract protected Intent addExtrasToIntent(Intent intent, String details);

    public boolean valid(Context context,long timeStamp) {
        //Log.i(TAG," "+campaignId+": "+timeStamp+" "+campaignEndTime);
        if(timeStamp>campaignEndTime) {
            return false;
        }
        int numberOfTimesShown = DbHelper.getDatabaseHelper(context).getNumberOfTimesShown(campaignId);
        //Log.i(TAG," "+ campaignId + ": " + numberOfTimesShown + " " + maximumNumberOfTimesToShow);
        if(numberOfTimesShown>=maximumNumberOfTimesToShow){
            return false;
        }
        return true;
    }
}
