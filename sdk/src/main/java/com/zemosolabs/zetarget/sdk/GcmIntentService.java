package com.zemosolabs.zetarget.sdk;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import org.json.JSONException;
import org.json.JSONObject;


public class GcmIntentService extends IntentService {
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    public static int notificationCount=0;
    private static final String TAG = "ZeTarget.GcmIntentService";

    public GcmIntentService() {
        super("GcmIntentService");
        //Log.i("log info ","GCMINTENT service --> gcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        //Log.i(TAG,"Notified of push");
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        //Log.i("log info ","GCMINTENT service --> onHandleintent");
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM
             * will be extended in the future with new message types, just ignore
             * any message types you're not interested in, or that you don't
             * recognize.
             */
            if (GoogleCloudMessaging.
                    MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                /*sendNotification("Send error: " + extras.toString());*/
            } else if (GoogleCloudMessaging.
                    MESSAGE_TYPE_DELETED.equals(messageType)) {
                /*sendNotification("Deleted messages on server: " +
                        extras.toString());*/
            } else if (GoogleCloudMessaging.
                    MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                sendNotification(extras);

            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.

    private void sendNotification(Bundle bundle){
        //Log.i(TAG,"sendNotification() called");
        String campaignId = bundle.getString("campaignId");
        String launcherClassName = null;
        launcherClassName = bundle.getString("url");
        if(launcherClassName==null||launcherClassName.isEmpty()) {
            launcherClassName = getApplicationContext().getPackageManager()
                    .getLaunchIntentForPackage(getApplicationContext().getPackageName())
                    .getComponent().getClassName();
        }
        int appIconId = getApplicationInfo().icon;
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);
        Class<?> launcher = null;
        try {
            launcher= Class.forName(launcherClassName);  //bundle.getString("launcherClass")
        } catch (ClassNotFoundException e) {
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG,"Launcher Class Not Found", e);
            }
        }
        PendingIntent contentIntent=null;
        if(launcher!=null){
            Intent notificationIntent = new Intent(this,launcher);
                //notificationIntent.putExtra(Constants.Z_BUNDLE_KEY_PUSH_NOTIFICATION_CAMPAIGN_ID, bundle.getString("campaignId"));
                notificationIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                contentIntent = PendingIntent.getActivity(this, 0,
                        notificationIntent, 0);
        }
        String title = bundle.getString("title");
        if(title==null||title.isEmpty()){
            return;
        }
        String message = bundle.getString("message");
        /*String buttonText = null;
        String buttonJson;
        if(bundle.getString("button")!=null){
            buttonJson = bundle.getString("button");
            Log.i("GCM button", buttonJson);
            try {
                JSONObject button = new JSONObject(buttonJson);
                buttonText = button.getString("buttonText");
            } catch (JSONException e) {
                Log.e("Error","json for button error in push");
            }
        }*/

        /*RemoteViews contentForNotification = new RemoteViews(getPackageName(),R.layout.notification);
        contentForNotification.setTextViewText(R.id.action_button_notification, "OK");
        if(buttonText!=null) {
            contentForNotification.setViewVisibility(R.id.action_button_notification, View.VISIBLE);
        }
        contentForNotification.setImageViewResource(R.id.image_left_notification, appIconId);
        contentForNotification.setTextViewText(R.id.title_text_notification, title);
        contentForNotification.setTextViewText(R.id.message_text_notification,message);*/
        //notificationCount++;
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this).setSmallIcon(appIconId)
        .setContentText(message);
        mBuilder.setContentTitle(title).setAutoCancel(true);
        mBuilder.setDefaults(NotificationCompat.DEFAULT_SOUND);
        //mBuilder.setContent(contentForNotification);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN) {
            if(notificationCount>1){
                mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(message).setSummaryText("+" +"more" + " messages"));
            }else{
                mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
            }
        }
        /*mBuilder.addAction(getResources().)*/
        if(contentIntent!=null) {
            mBuilder.setContentIntent(contentIntent);
            //Log.i(TAG, "Pending Intent added to the Notification");
        }
        Notification notification = mBuilder.build();
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT) {
            Bundle bundleForListener = notification.extras;
            bundleForListener.putString("campaignId", campaignId);
            notification.extras = bundleForListener;
        }
        mNotificationManager.notify(NOTIFICATION_ID, notification);

        notificationCount++;

        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.KITKAT) {
            JSONObject promoEvent = new JSONObject();
            try {
                promoEvent.put("campaignId", campaignId);
            } catch (JSONException e) {
                if (ZeTarget.isDebuggingOn()) {
                    Log.e(TAG, "campaign_id writing into event failed");
                }
            }
            ZeTarget.uncheckedLogEvent(Constants.Z_CAMPAIGN_VIEWED_EVENT, promoEvent);
        }
    }
}