package com.zemosolabs.zetarget.sdk;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class CampaignHandlingService extends Service implements ResultCallback<Status>,GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener{
    private static final String TAG = "ZeTarget.CampaignHandler";
    private static final String TAGGEO = TAG+".GEO";
    private static final String TAGSIMPLE = TAG+".SIMPLE";
    private static Worker fetcher = new Worker("campaignFetcher");
    private static Worker triggerHandler = new Worker("campaignTriggerHandler");
    private static HashMap<String,NotificationCampaign> liveCampaigns = new HashMap<>();
    private static HashMap<String,String> idsMappedToEventName = new HashMap<>();
    private static int notificationId = 0;
    ArrayList<Geofence> listOfGeofences;
    GoogleApiClient mGoogleApiClient;
    PendingIntent pendingIntentForGeofence = null;

    DbHelper dbHelper;

    static{
        fetcher.start();
        triggerHandler.start();
    }
    public CampaignHandlingService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent!=null) {
            //Log.i(TAG, "CampaignHandler started");
            /*if(ZeTarget.robolectricTesting) {
                System.out.println("Campaign Handler: started");
            }*/
            if (!intent.hasExtra("action")) {
                /*if(ZeTarget.robolectricTesting) {
                    System.out.println("Campaign Handler: Doesn't have extra action");
                }*/
                //Log.i(TAG, "intent does not have action");
                return START_STICKY;
            }
            if (intent.getStringExtra("action").equalsIgnoreCase(Constants.Z_INTENT_EXTRA_CAMPAIGNS_ACTION_KEY_VALUE_UPDATE_CAMPAIGNS)) {
                /*if(ZeTarget.robolectricTesting) {
                    System.out.println("Campaign Handler: campaign updation received");
                }*/
                //Log.i(TAG, "UPDATING LIVE CAMPAIGNS");
                final String type = intent.getStringExtra("type");
                fetcher.post(new Runnable() {
                    @Override
                    public void run() {
                        if (type != null) {
                            fetchNewCampaigns(type);
                            if (type.equals(Constants.Z_CAMPAIGN_TYPE_GEOCAMPAIGN)) {
                                //Log.i(TAGGEO, "Campaign update type geo");
                                registerGeofences();
                            }
                        }
                    }
                });
            } else if (intent.getStringExtra("action").equalsIgnoreCase(Constants.Z_INTENT_EXTRA_CAMPAIGNS_ACTION_KEY_VALUE_HANDLE_GEO_TRIGGERS)) {
                /*if(ZeTarget.robolectricTesting) {
                    System.out.println("Campaign Handler: geo trigger received");
                }*/
                //Log.i(TAG, "HANDLING GEO TRIGGERS");
                fetchNewCampaigns(Constants.Z_CAMPAIGN_TYPE_GEOCAMPAIGN);
                final String requestId = intent.getStringExtra("reqId");
                triggerHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (requestId != null) {
                            handleGeofenceTrigger(requestId);
                        }
                    }
                });
            } else if (intent.getStringExtra("action").equalsIgnoreCase(Constants.Z_INTENT_EXTRA_CAMPAIGNS_ACTION_KEY_VALUE_HANDLE_SIMPLE_EVENT_TRIGGERS)) {
                /*if(ZeTarget.robolectricTesting) {
                    System.out.println("Campaign Handler: simple event trigger received");
                }*/
                //Log.i(TAG, "HANDLING SIMPLE EVENT TRIGGERS");
                fetchNewCampaigns(Constants.Z_CAMPAIGN_TYPE_SIMPLE_EVENT_CAMPAIGN);
                final String eventName = intent.getStringExtra("eventType");
               /* if(ZeTarget.robolectricTesting) {
                    System.out.println("Campaign Handler: simple event name- " + eventName);
                }*/
                triggerHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (eventName != null) {
                            /*if(ZeTarget.robolectricTesting) {
                                System.out.println("Campaign Handler: simple event trigger being handled");
                            }*/
                            handleSimpleEventTrigger(eventName);
                            //Log.i(TAG, "simple event trigger occured" + eventName);
                        }
                    }
                });
            }
        }
        return START_STICKY;
    }

    private void handleSimpleEventTrigger(String eventType) {
       /* if(ZeTarget.robolectricTesting) {
            System.out.println(idsMappedToEventName);
        }*/
        if(idsMappedToEventName.containsKey(eventType)){
            /*if(ZeTarget.robolectricTesting) {
                System.out.println("ZETARGET: idsMapping found");
            }*/
            //Log.i(TAG,"id Mapping to EventName working fine");
            String campaignId = idsMappedToEventName.get(eventType);
            if(liveCampaigns.containsKey(campaignId)){
                //Log.i(TAG, "liveCampaign contains the key");
                long timeStamp = System.currentTimeMillis();
                if(liveCampaigns.get(campaignId).valid(getApplicationContext(), timeStamp)) {
                   /* if(ZeTarget.robolectricTesting) {
                        System.out.println("Campaign is valid");
                    }*/
                    liveCampaigns.get(campaignId).show(this, campaignId,timeStamp);
                }else{
                    //Log.i(TAG,"removing the campaign: "+campaignId);
                    liveCampaigns.remove(campaignId);
                    dbHelper.removeSimpleEventCampaign(campaignId);
                }
            }
        }
       // Log.i(TAG,"eventType: " +eventType);
    }

    private void handleGeofenceTrigger(String requestId) {
        String[] reqId = requestId.split("_");
        String campaignId = reqId[0];
        if(liveCampaigns.containsKey(campaignId)){
            long timeStamp = System.currentTimeMillis();
            if(liveCampaigns.get(campaignId).valid(getApplicationContext(),timeStamp)) {
                liveCampaigns.get(campaignId).show(this, requestId,timeStamp);
            }else{
                liveCampaigns.remove(campaignId);
                dbHelper.removeGeoCampaign(campaignId);
            }
        }
    }

    private void fetchNewCampaigns(String type) {
        dbHelper = DbHelper.getDatabaseHelper(getApplicationContext());
        if(type.equals(Constants.Z_CAMPAIGN_TYPE_GEOCAMPAIGN)){
            JSONArray geoCampaigns = dbHelper.getGeoFenceCampaigns();
            addNewCampaignsIn(geoCampaigns);
        }else if(type.equals(Constants.Z_CAMPAIGN_TYPE_SIMPLE_EVENT_CAMPAIGN)){
            JSONArray simpleEventCampaigns = dbHelper.getSimpleEventCampaigns();
            //Log.i(TAG,"fetched Simple event campaigns from database:"+simpleEventCampaigns.toString());
            addNewCampaignsIn(simpleEventCampaigns);
        }
    }

    private void addNewCampaignsIn(JSONArray campaignsInDb) {
        for(int i=0;i<campaignsInDb.length();i++){
            try {
                JSONObject currentCampaign = campaignsInDb.getJSONObject(i);
                //Log.i(TAG,currentCampaign.toString());
                if(currentCampaign.getString("type").equals(Constants.Z_CAMPAIGN_TYPE_SIMPLE_EVENT_CAMPAIGN)){
                    //Log.i(TAG,"Simple event campaign detected while adding new Campaigns");
                    String campaignId = currentCampaign.getString("campaignId");
                    String eventName = currentCampaign.getJSONObject("simple_event").getString("eventName");
                    int numberOfTimesToShow = currentCampaign.getJSONObject("suppressionLogic").getInt("maximumNumberOfTimesToShow");
                    idsMappedToEventName.put(eventName,campaignId);
                    SimpleEventNotificationCampaign simpleEventNotificationCampaign = new SimpleEventNotificationCampaign(currentCampaign, notificationId());
                    liveCampaigns.put(campaignId,simpleEventNotificationCampaign);
                }else if(currentCampaign.getString("type").equals(Constants.Z_CAMPAIGN_TYPE_GEOCAMPAIGN)){
                    String campaignId = currentCampaign.getString("campaignId");
                    int numberOfTimesToShow = currentCampaign.getJSONObject("suppressionLogic").getInt("maximumNumberOfTimesToShow");
                    GeoNotificationCampaign geoNotificationCampaign = new GeoNotificationCampaign(currentCampaign, notificationId());
                    //Log.i(TAG+"GEO","GEO Notification Campaign added to the liveCampaigns");
                    createListOfGeofences(currentCampaign);
                    liveCampaigns.put(campaignId,geoNotificationCampaign);
                }
            } catch (JSONException e) {
                /*if(ZeTarget.isDebuggingOn()){
                    Log.e(TAG,"jsonException",e);
                }*/
            }

        }
    }

    private void createListOfGeofences(JSONObject geoNotificationJSONCampaign) {
        listOfGeofences = new ArrayList<>();
        try {
            String campaignId = geoNotificationJSONCampaign.getString("campaignId");
            JSONObject geo = geoNotificationJSONCampaign.getJSONObject("geo");
            JSONArray geofencesData = geo.getJSONArray("geoFences");
            int timeDelay = 5000;
            if(geo.has("timeDelay")){
                timeDelay = geo.getInt("timeDelay");
            }
            for(int i = 0; i < geofencesData.length(); i++){
                JSONObject currentGeofenceData = (JSONObject)geofencesData.get(i);
                Geofence.Builder geofenceBuilder = new Geofence.Builder()
                        .setRequestId(campaignId + "_" + currentGeofenceData.getString("geofenceId"))
                        .setCircularRegion(
                                currentGeofenceData.getDouble("latitude"),
                                currentGeofenceData.getDouble("longitude"),
                                (float) currentGeofenceData.getDouble("radius")
                        )
                        .setExpirationDuration(geo.getLong("expirationTime"));
                String transType = geo.getString("transitionType");
                switch (transType) {
                        case "ENTER":
                            geofenceBuilder.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER);
                            break;
                        case "EXIT":
                            geofenceBuilder.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT);
                        case "DWELL":
                            geofenceBuilder.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL).setLoiteringDelay(timeDelay);
                        default:
                            break;
                }
                listOfGeofences.add(
                        geofenceBuilder
                        .build());
            }
            //Log.i(TAGGEO,"geofences added to the list"+listOfGeofences.size());
        } catch (JSONException e) {
            /*if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "geoNotificationCampaign Failure", e);
            }*/
        }
    }

    private void registerGeofences() {
        //Log.i(TAG+"GEO","registering Geofences");
        mGoogleApiClient  = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        mGoogleApiClient.connect();
    }

    private GeofencingRequest getGeofencingRequest() {
         //Log.i(TAGGEO,Integer.valueOf(listOfGeofences.size()).toString());
         GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
         builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL);
         builder.addGeofences(listOfGeofences);
         return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        return PendingIntent.getService(this, 0, intent, 0);
    }

    private int notificationId() {
        if(notificationId>=1000||notificationId<=0){
            notificationId=1;
        }
        return notificationId++;
    }

    @Override
    public IBinder onBind(Intent intent) {

        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onResult(Status status) {
        if(status.isSuccess()){
            //Log.i(TAGGEO,"Added Geofences");
        }else{
           if(ZeTarget.isDebuggingOn()){
               Log.e(TAGGEO,"Geofences failed. Status code: "+status.getStatusCode());
           }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        //Log.i(TAGGEO,"connected to GoogleApiClient");
        if(listOfGeofences!=null && listOfGeofences.size()>0) {
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    getGeofencingRequest(),
                    getGeofencePendingIntent()
            ).setResultCallback(this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        //Log.i(TAGGEO,"connection to GoogleApiClient suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if(ZeTarget.isDebuggingOn()){
            Log.e(TAGGEO,"connection failed on googleApiClient:"+connectionResult.getErrorCode());
        }
    }
}
