package com.zemosolabs.zetarget.sdk;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;


public class GeofenceTransitionsIntentService extends IntentService {
    private static final String TAG = "ZeTarget.GeofenceTransitionsIntentService";

    public GeofenceTransitionsIntentService() {
        super("GeofenceTransitionsIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(intent!=null){
            GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
            if (geofencingEvent.hasError()) {
                if(ZeTarget.isDebuggingOn()){
                    Log.e(TAG, Integer.valueOf(geofencingEvent.getErrorCode()).toString());
                }
                return;
            }

            List triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            for(Object geofence: triggeringGeofences){
                String requestId = ((Geofence)geofence).getRequestId();
                Intent intentToCampaignHandlingService = new Intent(this,CampaignHandlingService.class);
                intentToCampaignHandlingService.putExtra("action",Constants.Z_INTENT_EXTRA_CAMPAIGNS_ACTION_KEY_VALUE_HANDLE_GEO_TRIGGERS);
                intentToCampaignHandlingService.putExtra("reqId",requestId);
                startService(intentToCampaignHandlingService);
            }
        }
    }
}
