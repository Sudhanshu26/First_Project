package com.zemosolabs.zetarget.sdk;

import android.content.Intent;

import org.json.JSONObject;

/**
 * Created by vedaprakash on 8/5/15.
 */
public class SimpleEventNotificationCampaign extends NotificationCampaign {

    protected SimpleEventNotificationCampaign(JSONObject currentCampaign, int notificationId) {
        super(currentCampaign,notificationId);
    }

    @Override
    protected Intent addExtrasToIntent(Intent launchIntent,String details) {
        launchIntent.putExtra("campaignId",campaignId);
        launchIntent.putExtra(Constants.Z_CAMPAIGN_TYPE,Constants.Z_CAMPAIGN_TYPE_SIMPLE_EVENT_CAMPAIGN);
        launchIntent.putExtra(Constants.Z_EVENT_TYPE,Constants.Z_CAMPAIGN_VIEWED_EVENT);
        return launchIntent;
    }
}
