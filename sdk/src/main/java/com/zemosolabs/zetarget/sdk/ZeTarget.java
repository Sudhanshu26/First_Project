    package com.zemosolabs.zetarget.sdk;

    import android.app.Activity;
    import android.app.Application;
    import android.app.Fragment;
    import android.app.FragmentTransaction;
    import android.content.Context;
    import android.content.Intent;
    import android.content.SharedPreferences;
    import android.content.res.Resources;
    import android.content.res.XmlResourceParser;
    import android.location.Location;
    import android.text.TextUtils;
    import android.util.Log;
    import android.util.Pair;
    import android.view.View;
    import android.widget.Button;
    import android.widget.EditText;
    import android.widget.TextView;
    import android.widget.Toast;

    import com.google.android.gms.common.ConnectionResult;
    import com.google.android.gms.common.GooglePlayServicesUtil;
    import com.google.android.gms.gcm.GoogleCloudMessaging;

    import org.json.JSONArray;
    import org.json.JSONException;
    import org.json.JSONObject;
    import org.xmlpull.v1.XmlPullParser;
    import org.xmlpull.v1.XmlPullParserException;

    import java.io.IOException;
    import java.util.ArrayList;
    import java.util.Calendar;
    import java.util.HashMap;
    import java.util.Iterator;
    import java.util.List;
    import java.util.Map;
    import java.util.UUID;
    import java.util.concurrent.atomic.AtomicBoolean;

    /**
     * Created by praveen on 19/01/15.
     */
    public class ZeTarget {

        private static final String TAG = "ZeTarget.ZeTarget";
        private static Context context;
        private static String apiKey;
        private static String userId;
        private static String deviceId;
        private static String googleApiProjectNumber;


        private static final DataStore dataStore = DataStore.getDataStore();
        private static Map<String,String> inAppTexts = new HashMap<String,String>();
        private static Map<String,Map<String,JSONArray>> stringIdMap = new HashMap<String,Map<String,JSONArray>>();

        private static List<Integer> parsedLayouts = new ArrayList<Integer>();
        private static boolean inAppTextsLoaded = false;

        static DeviceDetails deviceDetails;

        private static long sessionId = -1;
        private static boolean isSessionOpen = false;
        private static final long sessionTimeoutMillis = Constants.Z_SESSION_TIMEOUT;
        private static Runnable endSessionRunnable;

        private static AtomicBoolean updateScheduled = new AtomicBoolean(false);
        private static AtomicBoolean uploadingCurrently = new AtomicBoolean(false);
        private static AtomicBoolean synchingDataStoreCurrently = new AtomicBoolean(false);
        private static AtomicBoolean fetchingPromotionsCurrently = new AtomicBoolean(false);
        private static AtomicBoolean updatingUserPropsCurrently = new AtomicBoolean(false);
        private static AtomicBoolean showingPromotion = new AtomicBoolean(false);
        static AtomicBoolean needTocallsetText = new AtomicBoolean(true);

        private static boolean DEBUG = false;


        private static final String START_SESSION_EVENT = Constants.Z_SESSION_START_EVENT;
        private static final String END_SESSION_EVENT = Constants.Z_SESSION_END_EVENT;

        private static boolean isInitialzed = false;
        private static Worker logWorker = new Worker("logWorker");
        private static Worker httpWorker = new Worker("httpWorker");
        private static Worker campaignWorker = new Worker("campaignWorker");


        private static String currentActivityName;
        private static String currentActivityLabel;
        static Activity currentActivity;
        // public static boolean robolectricTesting = false;

        static {
            logWorker.start();
            httpWorker.start();
            campaignWorker.start();
        }

        private static ZeTargetInAppNotification customDialogFragment = null;
        private static String classNameOfCustomDialogFragment = null;
        private static String classNameOfDefaultDialogFragment = "com.zemosolabs.zetarget.user_interfaces.DefaultInAppNotification";


        private ZeTarget(){

        }

        static Map<String, String> getInAppTexts() {
            return inAppTexts;
        }

        synchronized static void resetInAppTexts() {
            ZeTarget.inAppTexts = new HashMap<String,String>();
        }

        static Map<String, Map<String,JSONArray>> getStringIdMap() {
            return stringIdMap;
        }

        static synchronized void addStringIdMap(String activityName, Map<String,JSONArray> value) {
            Map<String,JSONArray> existingValues = ZeTarget.getStringIdMap().get(activityName);
            if(existingValues == null){
                ZeTarget.stringIdMap.put(activityName,value);
            }
            else {
                Iterator it = value.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry) it.next();
                    JSONArray existingArrayForKey = existingValues.get(pair.getKey());

                        try {
                            if(existingArrayForKey == null) {
                                existingValues.put(pair.getKey().toString(), new JSONArray(pair.getValue().toString()));
                            }
                            else {
                                JSONArray newToAdd = new JSONArray(pair.getValue().toString());
                                for(int j=0;j<newToAdd.length();j++){
                                    existingArrayForKey.put(newToAdd.getJSONObject(j));
                                }
                                existingValues.put(pair.getKey().toString(), existingArrayForKey);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                }
                ZeTarget.stringIdMap.put(activityName,existingValues);
            }
        }

        static List<Integer> getParsedLayouts() {
            return parsedLayouts;
        }

        static void addParsedLayouts(Integer id) {
            if(!ZeTarget.parsedLayouts.contains(id)){
                ZeTarget.parsedLayouts.add(id);
            }
        }

        static void registerZeTargetActivityLifecycleCallbacks(){
            if(android.os.Build.VERSION.SDK_INT >= 16) { //Only available for API >=16
                if (context.getApplicationContext() instanceof Application) {
                    final Application app = (Application) context.getApplicationContext();
                    app.registerActivityLifecycleCallbacks((new ZeTargetActivityLifecycleCallbacks()));
                    if(ZeTarget.isDebuggingOn()) {
                        Log.d(TAG, "Registered for ActivityLifecycleCallback successfully");
                    }
                }
            }
            else {
                Log.e(TAG,"This android version is not supported by ZeTarget. Zetarget requires API level >=16");
            }
        }

        static void updateActivityDetails(String label, String name){
            currentActivityLabel = label;
            currentActivityName = name;
        }

        static String getApiKey(){
            return apiKey;
        }

        static String getUserId(){
            return userId;
        }

        static String getDeviceId(){
            return deviceId;
        }

        /*public static void deriveDeviceidFromUserid(){

        }

        public static void doNotCaptureEventsAutomaticallyExceptForCampaignTracking(){

        }*/

        /**
         * Method to change the default ZeTarget URL: This method can be used to change the ZeTarget URL's domain
         * to your custom domain in case one has been assigned to you. Custom domains are available only for
         * enterprise accounts.
         *
         * This method has to be called before any of the initialize methods are called.
         *
         *
         * @param url the alternate url to the default ZeTarget URL
         *
         */

        public static void setZeTargetURL(String url){
            String url_desired_format = "^[([hH][Tt][Tt][Pp]://)|([Hh][Tt][Tt][Pp][Ss]://)]?.*[/]?";
            if(url.matches(url_desired_format)) {
                if(!url.matches("/$")){
                    url = url +"/";
                }
                if (!isInitialzed) {
                    Constants.Z_BASE_URL = url;
                    if(ZeTarget.isDebuggingOn()) {
                        Log.d(TAG, "Setting Z_BASE_URL to: "+url);
                    }
                }
            }
        }

        /**
         *
         * Method to get ZeTarget SDK version
         *
         */
        public static String getSDKVersion() {
            return Constants.Z_SDK_ID;
        }


        /**
         *
         * Method to initialize ZeTarget SDK
         *
         * @param context the application context
         * @param apiKey the api key found at zetarget account
         *
         */
        public static void initializeWithContextAndKey(Context context, String apiKey) {
            /*if(ZeTarget.isDebuggingOn()){
                Log.d(TAG, "initializeWithContextAndKey() called");
            }*/
            initialize(context, apiKey,null);
        }

        /**
         *
         * Method to initialize ZeTarget SDK
         *
         * @param context the application context
         * @param apiKey the api key found at zetarget account
         * @param googleApiProjectNumber the Google API Project Number for Push Notifications
         *
         */
        public static void initializeWithContextAndKey(Context context, String apiKey, String googleApiProjectNumber) {
            /*if(ZeTarget.isDebuggingOn()){
                Log.d(TAG, "initializeWithContextAndKey() called");
            }*/
            initialize(context, apiKey,googleApiProjectNumber);
        }

        /**
         * Method to initialize ZeTarget SDK
         *
         * @param context the application context
         * @param apiKey the api key found at zetarget account
         * @param googleApiProjectNumber the Google API Project Number for Push Notifications. This can be null if not
         *                               required.
         * @param classNameOfCustomDialogFrag The class name of the the custom DialogFragment which subclasses
         *                                    ZeTargetInAppNotification to handle the displaying of In App
         *                                    Promotions.
         *
         *    This initialize method should only be used if the user wants to handle the display of In App Promotions
         *    through the custom subclass of ZeTargetInAppNotification.class
         */

        public static void initializeWithContextAndKey(Context context, String apiKey, String googleApiProjectNumber,String classNameOfCustomDialogFrag) {
           /* if(ZeTarget.isDebuggingOn()){
                Log.d(TAG, "initializeWithContextAndKey() called");
            }*/
            initialize(context, apiKey, googleApiProjectNumber);
            classNameOfCustomDialogFragment = classNameOfCustomDialogFrag;
            Log.d(TAG,"hii");
        }

        /**
         * Method to initialize ZeTarget SDK
         *
         * @param context the application context
         * @param apiKey the api key found at zetarget account
         * @param userId In the case where ZeTarget user id is to be same as the user id used prior to ZeTarget integration,
         *               the previous user id needs to passed as a String value in this parameter
         *
         */
        public static void initializeWithContextAndKeyAndUserId(Context context, String apiKey, String userId){
            /*if(ZeTarget.isDebuggingOn()){
                Log.d(TAG, "initializeWithContextAndKey() called");
            }*/
            setUserIdAtInit(context, userId);
            initialize(context, apiKey,null);
        }

        /**
         * Method to initialize ZeTarget SDK
         *
         * @param context the application context
         * @param apiKey the api key found at zetarget account
         * @param userId In the case where ZeTarget user id is to be same as the user id used prior to ZeTarget integration,
         *               the previous user id needs to passed as a String value in this parameter
         * @param googleApiProjectNumber the Google API Project Number for Push Notifications. This can be null if not required.
         * @param classNameOfCustomDialogFrag The class name of the the custom DialogFragment which subclasses
         *                                    ZeTargetInAppNotification to handle the displaying of In App
         *                                    Promotions. This can be null if not required.
         *
         *
         */
        public static void initializeWithContextAndKeyAndUserId(Context context, String apiKey, String userId, String googleApiProjectNumber, String classNameOfCustomDialogFrag) {
            /*if(ZeTarget.isDebuggingOn()){
                Log.d(TAG, "initializeWithContextAndKey() called");
            }*/
            setUserIdAtInit(context, userId);
            if(classNameOfCustomDialogFrag!=null) {
                classNameOfCustomDialogFragment = classNameOfCustomDialogFrag;
            }
            initialize(context, apiKey,googleApiProjectNumber);
        }

        private synchronized static void initialize(Context context, String apiKey, String googleApiProjectNumber) {
            if (context == null) {
                Log.e(TAG, "Application context cannot be null in initializeWithContextAndKey()");
                return;
            }
            if (apiKey == null||TextUtils.isEmpty(apiKey) ) {
                Log.e(TAG, "Application apiKey cannot be null or blank in initializeWithContextAndKey()");
                return;
            }
            if (!isInitialzed) {
                setContext(context.getApplicationContext());
                setApiKey(apiKey);
                deviceDetails = new DeviceDetails(context);
                initializeDeviceDetails();
                deviceId = initializeDeviceId();
                userId = getSavedUserId();
                if(userId == null){
                    userId = deviceId;
                    CommonUtils.getSharedPreferences(context).edit().putString(Constants.Z_PREFKEY_USER_ID, userId).apply();
                }

                if(googleApiProjectNumber != null){
                    ZeTarget.googleApiProjectNumber = googleApiProjectNumber;
                }

                //Send init event if installed for the first time.
                if(isFirstTimeUse()) {
                    setFirstTimeFalse();
                    logWorker.post(new Runnable() {
                        @Override
                        public void run() {
                            sendEventToServer(Constants.Z_INIT_EVENT, System.currentTimeMillis(), Constants.Z_INIT_LOG_URL, true);
                        }
                    });
                }
                if(ZeTarget.isDebuggingOn()) {
                    Log.d(TAG, "Initialization complete with userId:"+userId+", deviceId:"+deviceId);
                }
                registerZeTargetActivityLifecycleCallbacks();
                isInitialzed = true;
            }
        }

        private static void registerForPushNotifications(){
            if(googleApiProjectNumber == null){
                if(ZeTarget.isDebuggingOn()) {
                    Log.d(TAG, "Google API Project number is not provided, hence Push Notifications will not work");
                }
                return;
            }
            if (checkPlayServices()) {

                if (getRegistrationId().isEmpty()) {
                    logWorker.post(new Runnable() {
                        @Override
                        public void run() {
                            registerInBackground();
                        }
                    });

                }
               // Log.i("gcmClientId",getRegistrationId());
            } else {
               // Log.i(TAG, "No valid Google Play Services APK found.");
            }
        }

        private static void registerInBackground(){
            /*if(ZeTarget.isDebuggingOn()){
                Log.d(TAG, "registerInBackground() called");
            }*/
            try {
                GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);

                String regid = gcm.register(googleApiProjectNumber);
                //Log.i("gcmClientId",regid);
                setUserProperty("deviceToken",regid);
                saveRegistrationId(regid);
                /*if(ZeTarget.isDebuggingOn()){
                    Log.d(TAG,"Recieved registration id from GCM: "+regid);
                }*/
            } catch (IOException ex) {
                if(ZeTarget.isDebuggingOn()){
                    Log.e(TAG,"Exception in registering GCM client id",ex);
                }
            }
        }

        private static boolean checkPlayServices() {
            int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
            //Log.i("resultCode", Integer.valueOf(resultCode).toString() + ";" + Integer.valueOf(ConnectionResult.SUCCESS).toString());
            if (resultCode != ConnectionResult.SUCCESS) {
                if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                    if(ZeTarget.isDebuggingOn()){
                        Log.e(TAG,"Google Play services is not installed, Push notifications will not work.");
                    }
                } else {
                    if(ZeTarget.isDebuggingOn()){
                        Log.e(TAG, "This device is not supported for Push Notifications.");
                    }
                }
                return false;
            }
            return true;
        }

        static void sendSnapshot(final JSONObject snapShot){
            if(!ScreenCapture.isVibrate)
                Toast.makeText(context,"Vibrate permission not given",Toast.LENGTH_SHORT).show();
            httpWorker.post(new Runnable(){

                @Override
                public void run() {
                    HttpHelper.doPost(Constants.Z_SEND_SNAPSHOT_URL,snapShot);
                }
            });
        }

        private static String getRegistrationId() {
            final SharedPreferences prefs = CommonUtils.getSharedPreferences(context);
            String registrationId = prefs.getString(Constants.Z_PREFKEY_GCM_REGISTRATION_ID, "");
            if (registrationId.isEmpty()) {
                return "";
            }
            // Check if app was updated; if so, it must clear the registration ID
            // since the existing regID is not guaranteed to work with the new
            // app version.
            int registeredVersion = prefs.getInt(Constants.Z_PREFKEY_APP_VERSION, Integer.MIN_VALUE);
            int currentVersion = DeviceDetails.getAppVersionCode();
            if (registeredVersion != currentVersion) {
                return "";
            }
            long currenttime = System.currentTimeMillis();
            long last_saved_time = CommonUtils.getSharedPreferences(context).getLong(Constants.Z_PREFKEY_GCM_REGISTRATION_ID_SYNC_TIME,0);
            if(currenttime - last_saved_time > Constants.Z_GCM_REGISTRATION_ID_RENEWAL_PERIOD){
                return "";
            }
            return registrationId;
        }


        /**
         * Method to enable ZeTarget SDK logging, useful for debugging
         */
        public static void enableDebugging(){
            DEBUG = true;
        }

        /**
         * Method to disable ZeTarget SDK logging.
         */

        public static void disableDebugging(){
            DEBUG = false;
        }

        static boolean isDebuggingOn(){
            return DEBUG;
        }


        private static boolean isFirstTimeUse(){
            return CommonUtils.getSharedPreferences(context).getBoolean(Constants.Z_PREFKEY_FIRSTTIME_FLAG,
                    true);
        }

        private static void setFirstTimeFalse(){
            CommonUtils.getSharedPreferences(context).edit().putBoolean(Constants.Z_PREFKEY_FIRSTTIME_FLAG,
                    false).apply();
        }

        /**
         * Method to provide the instance of a custom dialog Fragment
         * @param customDialogFrag is the instance of a subclass of ZeTargetInAppNotification class provided
         *                         by the user of the sdk.
         *
         *   This initialize method should only be used if the user wants to handle the display of In App Promotions
         *   through the custom subclass of ZeTargetInAppNotification.class
         */

        public static void setCustomDialogFragment(ZeTargetInAppNotification customDialogFrag){
            customDialogFragment = customDialogFrag;
        }

        static void showPromotion(final Activity currentActivity){
            //The developer has to label all his activities where he needs to show promotions.
            //android:label is a optional attribute for all activities. Using the same as screen id
            //would work.
            if(showingPromotion.get()){
                return;
            }
            long lastShown = getSharedPreferenceValueByKey(Constants.KEY_IN_APP_LAST_SHOWN_TIME);
            long nowMS = System.currentTimeMillis();
            if(lastShown>0&&(nowMS-lastShown<Constants.Z_TIMEOUT_BETWEEN_IN_APP)){
                if(ZeTarget.isDebuggingOn()) {
                    Log.d(TAG, "Too soon to show In App campaign, will not try to show any In App campaign, please wait for sometime");
                }
                return;
            }
            String screen_id =currentActivityLabel ;
            if(screen_id==null){
                if(ZeTarget.isDebuggingOn()) {
                    Log.d(TAG, "Somehow could not get Activity identifier, hence could not show In App campaign");
                }
                return;
            }
            //Log.i("ActivityDetails: ", currentActivityLabel + ", " + currentActivityName);
            if(ZeTarget.isDebuggingOn()) {
                Log.d(TAG, "Lets check db if we have any In App Campaign for Screen:"+screen_id);
            }

            DbHelper dbHelper = DbHelper.getDatabaseHelper(context);
            dbHelper.removeSeenPromotions();
            JSONObject promotionForScreen = dbHelper.getPromotionforScreen(screen_id);
            JSONObject defaultPromotions = dbHelper.getPromotionforScreen(Constants.DEFAULT_SCREEN);
            JSONObject probablePromotion;
            if(promotionForScreen == null||promotionForScreen.length() == 0){
                probablePromotion = defaultPromotions;
                if(ZeTarget.isDebuggingOn()) {
                    Log.d(TAG, "Did not find any In App Campaign for Screen:"+screen_id+"");
                    if(defaultPromotions == null || defaultPromotions.length() == 0){
                        Log.d(TAG, "Did not find any In App Campaign for Any Screen");
                    }
                }
            }else{
                probablePromotion = promotionForScreen;
                if(ZeTarget.isDebuggingOn()) {
                    Log.d(TAG, "Found one In App Campaign for Screen:"+screen_id);
                }
            }

            final JSONObject promotion = probablePromotion;

            if(promotion == null || promotion.length() == 0  ){
                if(ZeTarget.isDebuggingOn()){
                    Log.d(TAG,"No InApp Campaigns found for the user at this point of time");
                }
                return;
            }

            try {

                final String campaignId = promotion.getString("campaignId");
                final JSONObject template = promotion.getJSONObject("template");
                int maximumNumberOfTimesToShow = -1, minimumDurationInMinutesBeforeReshow = -1;
                if(promotion.has("suppressionLogic")) {
                    JSONObject suppressionLogic = promotion.getJSONObject("suppressionLogic");
                    if(suppressionLogic.has("maximumNumberOfTimesToShow")) {
                        maximumNumberOfTimesToShow = suppressionLogic.getInt("maximumNumberOfTimesToShow");
                    }
                    if(suppressionLogic.has("minimumDurationInMinutesBeforeReshow")) {
                        minimumDurationInMinutesBeforeReshow = suppressionLogic.getInt("minimumDurationInMinutesBeforeReshow");
                    }
                }
                long currentTime = System.currentTimeMillis();
                if(minimumDurationInMinutesBeforeReshow!=-1) {
                    if (currentTime < dbHelper.getLastShownTime(campaignId) + minimumDurationInMinutesBeforeReshow * 60 * 1000) {
                        if (ZeTarget.isDebuggingOn()) {
                            Log.d(TAG, "Not so soon, this campaign can be shown again after " +minimumDurationInMinutesBeforeReshow+" minutes "+ campaignId);
                        }
                        return;
                    }
                }// In case the minimumDurationInMinutes value is not found, then the campaign will be shown only once.
                else{
                    if(dbHelper.getNumberOfTimesShown(campaignId)>=1) {
                        dbHelper.markPromotionAsSeen(campaignId);
                        if(ZeTarget.isDebuggingOn()) {
                            Log.d(TAG, "Found one already shown In App campaign, cannot show it again");
                        }
                        return;
                    }
                }
                if(maximumNumberOfTimesToShow!=-1) {
                    if (dbHelper.getNumberOfTimesShown(campaignId) >= maximumNumberOfTimesToShow) {
                        if (ZeTarget.isDebuggingOn()) {
                            Log.d(TAG, "Already shown too many times " + campaignId);
                        }
                        dbHelper.markPromotionAsSeen(campaignId);
                        return;
                    }
                }// In case the maximumNumberOfTimesToShow value is not found, then the campaign will be shown only once.
                else{
                    if(dbHelper.getNumberOfTimesShown(campaignId)>=1) {
                        dbHelper.markPromotionAsSeen(campaignId);
                        if(ZeTarget.isDebuggingOn()) {
                            Log.d(TAG, "Found one already shown In App campaign, cannot show it again");
                        }
                        return;
                    }
                }

                currentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        FragmentTransaction ft = ZeTarget.currentActivity.getFragmentManager().beginTransaction();
                        Fragment prev = ZeTarget.currentActivity.getFragmentManager().findFragmentByTag("dialog");
                        if (prev != null) {
                            ft.remove(prev);
                        }
                        ft.addToBackStack(null);
                        ZeTargetInAppNotification newNotification = null;
                        if(customDialogFragment!=null){
                            newNotification = customDialogFragment;
                        }
                        else if(classNameOfCustomDialogFragment!=null){
                            try {
                                newNotification = (ZeTargetInAppNotification) Class.forName(classNameOfCustomDialogFragment).newInstance();
                            } catch (InstantiationException e) {
                                if(ZeTarget.isDebuggingOn()){
                                    Log.e(TAG,"Error in instantiating Custom DialogFragment class ", e);
                                }
                            } catch (IllegalAccessException e){
                                if(ZeTarget.isDebuggingOn()){
                                    Log.e(TAG,"Unable to call newInstance() on Custom DialogFragment class ", e);
                                }
                            } catch (ClassNotFoundException e){
                                if(ZeTarget.isDebuggingOn()){
                                    Log.e(TAG,"Custom DialogFragment class not found ", e);
                                }
                            }
                        }
                        else{
                            try {
                                newNotification = (ZeTargetInAppNotification) Class.forName(classNameOfDefaultDialogFragment).newInstance();
                            } catch (InstantiationException e) {
                                if(ZeTarget.isDebuggingOn()){
                                    Log.e(TAG,"Error in instantiating Custom DialogFragment class ", e);
                                }
                            } catch (IllegalAccessException e){
                                if(ZeTarget.isDebuggingOn()){
                                    Log.e(TAG,"Unable to call newInstance() on Custom DialogFragment class ", e);
                                }
                            } catch (ClassNotFoundException e){
                                if(ZeTarget.isDebuggingOn()){
                                    Log.e(TAG,"Custom DialogFragment class not found ", e);
                                }
                            }
                        }
                        newNotification.customize(context, campaignId, template);
                        if(ZeTarget.isDebuggingOn()) {
                            Log.d(TAG, "Preparing the campaign to show...");
                        }
                        if(ZeTarget.currentActivity==currentActivity) {
                            //Log.i(TAG, "same Activity before In App Promotion launched");
//                            long lastShown = getSharedPreferenceValueByKey(Constants.KEY_IN_APP_LAST_SHOWN_TIME);
//                            long nowMS = System.currentTimeMillis();
//                            if(lastShown>0&&(nowMS-lastShown<Constants.Z_TIMEOUT_BETWEEN_IN_APP)){
//                                return;
//                            }

                            //showingPromotion.set(true);
                            if(ZeTarget.isDebuggingOn()) {
                                Log.d(TAG, "Found one In App Campaign, lets show it if there is not already being shown");
                            }
                            if(!showingPromotion.getAndSet(true)){
                                if(ZeTarget.isDebuggingOn()){
                                    Log.d(TAG,"Showing In App campaign...");
                                }
                                newNotification.show(ft, "dialog");
                            }

                        }else{
                            if(ZeTarget.isDebuggingOn()){
                                Log.d(TAG,"Activity changed so could not show In App Campaign");
                            }
                            //Log.i(TAG,"Activity changed so dropping from launching In App Promotion");
                        }
                    }
                });
            }
            catch (Exception e){
                if(ZeTarget.isDebuggingOn()){
                    Log.e(TAG,"Error while showing promotion ",e);
                }
            }

        }

        static void _startSession(final Activity activity){
            logWorker.post(new Runnable() {
                @Override
                public void run() {
                    startSession();
                    showPromotion(activity);
                }
            });
        }

        static void _endSession(){
            logWorker.post(new Runnable() {
                @Override
                public void run() {
                    endSession();
                }
            });
        }

        /**
         * This method starts a session during app usage
         *
         */

        static void startSession() {
            /*if(ZeTarget.isDebuggingOn()){
                Log.d(TAG,"startSession() called");
            }*/
            if (!isContextAndApiKeySet("startSession()")) {
                return;
            }
            final long now = System.currentTimeMillis();

            runOnLogWorker(new Runnable() {
                @Override
                public void run() {
                    logWorker.removeCallbacks(endSessionRunnable);
                    long previousEndSessionId = getEndSessionId();
                    long lastEndSessionTime = getEndSessionTime();
                    if (previousEndSessionId != -1
                            && now - lastEndSessionTime < Constants.Z_MIN_TIME_BETWEEN_SESSIONS_MILLIS) {
                        DbHelper dbHelper = DbHelper.getDatabaseHelper(context);
                        dbHelper.removeEvent(previousEndSessionId);
                    }
                    //startSession() can be called in every activity by developer, hence upload events and sync datastore
                    // only if it is a new session
                    //syncToServerIfNeeded(now);
                    startNewSessionIfNeeded(now);

                    openSession();

                    // Update last event time
                    setLastEventTime(now);
                    //syncDataStore();
                    //uploadEvents();

                }
            });
        }

        static void checkPromotions(){
            /*if(ZeTarget.isDebuggingOn()){
                Log.d(TAG, "checkPromotions() called");
            }*/

            logWorker.post(new Runnable() {
                @Override
                public void run() {
                    getPromotions();
                }
            });
        }

        private static void getPromotions(){
            if(!fetchingPromotionsCurrently.getAndSet(true)) {
               /* if (ZeTarget.isDebuggingOn()) {
                    Log.d(TAG, "logWorker is now asking httpWorker to fetch Promotions");
                }*/
                httpWorker.post(new Runnable() {
                    @Override
                    public void run() {
                        fetchPromotions();
                    }
                });
            }
        }

        /**
         * Method to update the viewing of In App Promotion
         * @param promotionEvent promotionEvent should be a JSONObject containing the key campaignId which is passed to your custom subclass of ZeTargetInAppNotification used to
         *                   handle display of In App Promotions
         *
         * ZeTarget SDK keeps track of the number of times an In App Promotion is shown and the last time an In App Promotion
         * was shown. Based on the logic user sets through the ZeTarget account, the Promotions are suppressed when not necessary.
         *
         * When the user takes the responsibility of handling the display of the In App Promotions, he/she should also take the
         * responsibility of updating the promotions when seen and removing them when necessary based on the app user's feedback.
         */
        public static void updatePromotionAsSeen(JSONObject promotionEvent){
            setLastInAppSeen(System.currentTimeMillis());
            DbHelper dbHelper = DbHelper.getDatabaseHelper(context);
            String campaignId = null;
            try {
                campaignId = promotionEvent.getString("campaignId");
            } catch (JSONException e) {
                if(ZeTarget.isDebuggingOn()){
                    Log.e(TAG,"Failure in updating campaigns",e);
                }
            }
            dbHelper.updateCampaign(campaignId, System.currentTimeMillis());
            logEvent(Constants.Z_CAMPAIGN_VIEWED_EVENT, promotionEvent);
            showingPromotion.set(false);
        }

        /**
         * Method to remove Campaigns from database.
         * @param promotionEvent promotionEvent should be a JSONObject containing the key campaignId which is passed to your custom subclass of ZeTargetInAppNotification used to
         *                   handle display of In App Promotions
         *
         * ZeTarget SDK keeps track of the number of times an In App Promotion is shown and the last time an In App Promotion
         * was shown. Based on the logic user sets through the ZeTarget account, the Promotions are suppressed when not necessary.
         *
         * When the user takes the responsibility of handling the display of the In App Promotions, he/she should also take the
         * responsibility of updating the promotions when seen and removing them when necessary based on the app user's feedback.
         */

        public static void removePromotion(JSONObject promotionEvent) {
            setLastInAppSeen(System.currentTimeMillis());
            DbHelper dbHelper = DbHelper.getDatabaseHelper(context);
            String campaignId = null;
            try {
                campaignId = promotionEvent.getString("campaignId");
            } catch (JSONException e) {
                if(ZeTarget.isDebuggingOn()){
                    //Log.e(TAG,"campaign_updation_failed");
                }
            }
            dbHelper.markPromotionAsSeen(campaignId);
            logEvent(Constants.Z_CAMPAIGN_VIEWED_EVENT, promotionEvent);
            showingPromotion.set(false);
        }

        private static void setLastInAppSeen(long l) {
            //Log.i("LastSeenTime",l+"");
            CommonUtils.getSharedPreferences(context).edit().putLong(Constants.KEY_IN_APP_LAST_SHOWN_TIME,l).apply();
        }

        private static void fetchPromotions(){
            if(ZeTarget.isDebuggingOn()){
                Log.d(TAG,"Lets check server if there are any new campaigns for the user");
            }
            boolean fetchSuccess = false;
            try {

                JSONObject postParams = new JSONObject();
                postParams.put("lastCampaignSynchedTime", CommonUtils.replaceWithJSONNull(getLastCampaignSyncTime()));
                String response = HttpHelper.doPost(Constants.Z_PROMOTION_URL,postParams);

                if(response != null){
                    final JSONObject jsonResponse = new JSONObject(response);
                    /*ScreenCapture.getInstance().createNewFile();
                    ScreenCapture.getInstance().writeToFile(jsonResponse.toString());*/
                    fetchSuccess = true;
                    fetchingPromotionsCurrently.set(false);
                    logWorker.post(new Runnable() {
                        @Override
                        public void run() {
                            addPromotions(jsonResponse);
                        }
                    });
                }

            } catch (Exception e) {
                // Just log any other exception so things don't crash on upload
                if(ZeTarget.isDebuggingOn()){
                    Log.e(TAG, "Exception in fetching Promotions from server:", e);
                }
            }

            if (!fetchSuccess) {
                fetchingPromotionsCurrently.set(false);
            }
        }

        private static void addPromotions(JSONObject json){
            int addCount;
            if(!json.has("promotions")){
                return;
            }
            boolean gotNewInAppPromotion = false;
            try {
                JSONArray promotions = json.getJSONArray("promotions");
                DbHelper dbHelper = DbHelper.getDatabaseHelper(context);
                addCount = 0;
                if(ZeTarget.isDebuggingOn()){
                    Log.d(TAG,"Fetched campaigns from server: "+promotions.length());
                }
                for(int i =0; i < promotions.length(); i++){
                    JSONObject promotion = promotions.getJSONObject(i);
                    JSONObject suppressionLogic = new JSONObject();
                    if(promotion.has("suppressionLogic")) {
                        suppressionLogic = promotion.getJSONObject("suppressionLogic");
                    }
                    int maximumNumberOfTimesToShow = -1;
                    int minimumDurationInMinutesBeforeReshow = -1;
                    if(suppressionLogic.has("maximumNumberOfTimesToShow")) {
                        maximumNumberOfTimesToShow = suppressionLogic.getInt("maximumNumberOfTimesToShow");
                    }
                    if(suppressionLogic.has("minimumDurationInMinutesBeforeReshow")){
                        minimumDurationInMinutesBeforeReshow = suppressionLogic.getInt("minimumDurationInMinutesBeforeReshow");
                    }
                    //Log.i("promotions: ",i+". "+promotion.toString());
                    final SharedPreferences prefs = CommonUtils.getSharedPreferences(context);
                    int currentAppVersion = prefs.getInt(Constants.Z_PREFKEY_APP_VERSION,-1);
                    int appVersionFrom,appVersionTo;
                    if(currentAppVersion!=-1) {
                        //Log.i(TAG, Integer.valueOf(currentAppVersion).toString());
                        if (promotion.has("appVersionFrom")) {
                            appVersionFrom = promotion.getInt("appVersionFrom");
                            if (currentAppVersion < appVersionFrom) {
                                if(ZeTarget.isDebuggingOn()){
                                    Log.d(TAG, Integer.valueOf(currentAppVersion).toString());
                                    Log.d(TAG,"Sorry this In App Campaign requires min appVersion:"+appVersionFrom+" hence not storing this In App Campaign");
                                }
                                continue;
                            }
                        }
                        if (promotion.has("appVersionTo")) {
                            appVersionTo = promotion.getInt("appVersionTo");
                            if (currentAppVersion > appVersionTo) {
                                if(ZeTarget.isDebuggingOn()){
                                    Log.d(TAG, Integer.valueOf(currentAppVersion).toString());
                                    Log.d(TAG,"Sorry this In App Campaign requires max appVersion:"+appVersionTo+" hence not storing this In App Campaign");
                                }
                                continue;
                            }
                        }
                    }
                    long timeStampNow = System.currentTimeMillis();
                    if(promotion.has("campaignEndTime")){
                        long campaignEndTime = promotion.getLong("campaignEndTime");
                        if(campaignEndTime<timeStampNow){
                            if(ZeTarget.isDebuggingOn()){
                                Log.d(TAG,"Sorry this In App Campaign has expired, hence not storing this In App Campaign");
                            }
                            continue;
                        }
                    }
                    if(promotion.getString("type").equals("screenFix")){
                        if(ZeTarget.isDebuggingOn()){
                            Log.d(TAG,"Saving this screenFix type campaign");
                        }
                        dbHelper.addScreenFix(promotion.toString(), promotion.getString("campaignId"), promotion.getString("screenId"));
                        addCount++;
                    }else if(promotion.getString("type").equals("promotion")) {
                        gotNewInAppPromotion = true;
//Temperorily using showing the campaign on MainScreen when no screenId is available in the JSON.
                        if (promotion.has("screenId") && promotion.getString("screenId") != JSONObject.NULL) {
                            dbHelper.addPromotion(promotion.toString(), promotion.getString("campaignId"), promotion.getString("screenId"),
                                    maximumNumberOfTimesToShow,minimumDurationInMinutesBeforeReshow);
                            if(ZeTarget.isDebuggingOn()){
                                Log.d(TAG,"Saving this promotion type campaign: "+promotion.getString("campaignId")+" for screen: "+promotion.getString("screenId"));
                            }
                        } else {
                            dbHelper.addPromotion(promotion.toString(), promotion.getString("campaignId"), Constants.DEFAULT_SCREEN,
                                    maximumNumberOfTimesToShow, minimumDurationInMinutesBeforeReshow); //promotion.getString("screenId"));
                            if(ZeTarget.isDebuggingOn()){
                                Log.d(TAG,"Saving this promotion type campaign: "+promotion.getString("campaignId")+" for screen: "+Constants.DEFAULT_SCREEN);
                            }
                        }
                        addCount++;
                    }else if(promotion.getString("type").equalsIgnoreCase("GEO")){
                        if(ZeTarget.isDebuggingOn()){
                            Log.d(TAG,"Saving this GEO type campaign");
                        }
                        dbHelper.addGeoCampaign(promotion.toString(), promotion.getString("campaignId"),
                                maximumNumberOfTimesToShow, minimumDurationInMinutesBeforeReshow);
                        context.startService(new Intent(context, CampaignHandlingService.class).putExtra("action", Constants.Z_INTENT_EXTRA_CAMPAIGNS_ACTION_KEY_VALUE_UPDATE_CAMPAIGNS)
                                .putExtra("type", Constants.Z_CAMPAIGN_TYPE_GEOCAMPAIGN));
                        addCount++;
                    }else if(promotion.getString("type").equalsIgnoreCase(Constants.Z_CAMPAIGN_TYPE_SIMPLE_EVENT_CAMPAIGN)){
                        dbHelper.addSimpleEventCampaign(promotion.toString(), promotion.getString("campaignId"),
                                                            maximumNumberOfTimesToShow,minimumDurationInMinutesBeforeReshow);
                        if(ZeTarget.isDebuggingOn()){
                            Log.d(TAG,"Saving this simple event type campaign");
                        }
                        context.startService(new Intent(context, CampaignHandlingService.class).putExtra("action", Constants.Z_INTENT_EXTRA_CAMPAIGNS_ACTION_KEY_VALUE_UPDATE_CAMPAIGNS)
                                .putExtra("type", Constants.Z_CAMPAIGN_TYPE_SIMPLE_EVENT_CAMPAIGN));
                        addCount++;
                    }else if(promotion.getString("type").equalsIgnoreCase("IBEACON")){
                        addCount++;
                    }
                }
                setLastCampaignSyncTime(json.getString("lastCampaignSynchedTime"));
                if(gotNewInAppPromotion){
                    showPromotion(ZeTarget.currentActivity);
                }
            } catch (Exception e) {
                // Just log any other exception so things don't crash on upload
                if(ZeTarget.isDebuggingOn()){
                    Log.e(TAG, "Exception in adding promotion", e);
                }
            }
        }

        private static void sync(){
            syncDataStore();
            checkPromotions();
            uploadEvents();
            sendUserProperties();
            registerForPushNotifications();
        }

        /*private static void syncToServerIfNeeded(long timestamp){
            if (!isSessionOpen) {
                long lastEndSessionTime = getEndSessionTime();
                if (timestamp - lastEndSessionTime < Constants.Z_MIN_TIME_BETWEEN_SESSIONS_MILLIS) {
                    // Sessions close enough, set sessionId to previous sessionId

                    SharedPreferences preferences = CommonUtils.getSharedPreferences(context);
                    long previousSessionId = preferences.getLong(Constants.Z_PREFKEY_LAST_END_SESSION_ID,
                            -1);

                    if (previousSessionId == -1) {
                        sync();
                        Log.i("syncing", "1st case");
                    }
                } else {
                    // Sessions not close enough, create new sessionId
                    sync();
                    Log.i("syncing","2nd case");
                }
            } else {

                long lastEventTime = getLastEventTime();
                Log.i("syncing","just before 3rd case syncing"+sessionId+" "+(timestamp-lastEventTime));
                if (timestamp - lastEventTime > sessionTimeoutMillis || sessionId == -1) {
                    sync();
                    Log.i("syncing","3rd case");
                }
            }
        }*/

        /**
         * This method ends a usage session
         */
        static void endSession() {
            /*if(ZeTarget.isDebuggingOn()){
                Log.d(TAG,"endSession() called");
            }*/
            if (!isContextAndApiKeySet("endSession()")) {
                return;
            }
            final long timestamp = System.currentTimeMillis();
            runOnLogWorker(new Runnable() {
                @Override
                public void run() {
                    if (isSessionOpen) {
                        SharedPreferences preferences = CommonUtils.getSharedPreferences(context);
                        long startSessionTimeStamp = preferences.getLong(Constants.Z_PREFKEY_LAST_END_SESSION_ID,
                                -1);
                        JSONObject end_sessionParams = new JSONObject();
                        if(startSessionTimeStamp != -1){
                            try {
                                end_sessionParams.put(Constants.Z_SESSION_END_EVENT_SESSION_LENGTH_PARAMS, timestamp - startSessionTimeStamp);
                            } catch (JSONException e) {
                                end_sessionParams =null;
                                if(ZeTarget.isDebuggingOn()){
                                    Log.d(TAG,"JSONException",e);
                                }
                            }
                        }
                        else {
                            end_sessionParams = null;
                        }
                        long eventId = logEvent(END_SESSION_EVENT, end_sessionParams, null, timestamp,
                                false);

                        preferences.edit().putLong(Constants.Z_PREFKEY_LAST_END_SESSION_EVENT_ID, eventId)
                                .putLong(Constants.Z_PREFKEY_LAST_END_SESSION_TIME, timestamp)
                                .apply();
                    }
                    closeSession();
                }
            });
            // Queue up upload events 16 seconds later
            logWorker.removeCallbacks(endSessionRunnable);
            endSessionRunnable = new Runnable() {
                @Override
                public void run() {
                    clearEndSession();
                    uploadEvents();
                    //Log.i("EndSessionRunnable","Executed"+sessionId);

                }
            };
            logWorker
                    .postDelayed(endSessionRunnable, Constants.Z_MIN_TIME_BETWEEN_SESSIONS_MILLIS + 1000);
        }

        /**
         * This method is to specify user properties, which can be accessed at ZeTarget cloud
         * and hence can be used to target campaigns based on user properties
         *
         * @param key the key paramether for which the value has to be saved, e.g firstname, lastname etc
         * @param value the value for the key
         */
        public static void setUserProperty(String key, String value){

            UserProperties.setUserProperty(context, key, value);
            sendUserProperties(Constants.Z_USER_PROPS_UPLOAD_PERIOD_MILLIS);
        }

        /**
         * This method is to specify user properties, which can be accessed at ZeTarget cloud
         * and hence can be used to target campaigns based on user properties
         *
         * @param userproperties JSON containing multiple user properties
         */
        public static void setUserProperties(JSONObject userproperties){

            DataStore.setUserProperties(context, userproperties);
            sendUserProperties(Constants.Z_USER_PROPS_UPLOAD_PERIOD_MILLIS);
        }

        /**
         * This method is to get user properties set from the SDK or from the ZeTarget cloud
         *
         * @param key the key for which value needs to be accessed
         * @param defaultValue the default value in case the user property for the key has not been set yet
         * @return the value for the key
         */

        public static String getUserProperty(String key, String defaultValue){
            return DataStore.getUserProperty(context, key, defaultValue);
        }

        /**
         * This method is to get data store values set from ZeTarget cloud
         *
         * @param key the key for which value needs to be accessed
         * @param defaultValue the default value in case the user property for the key has not been set yet
         * @return the value for the key
         */
        public static String getData(String key, String defaultValue){
            return DataStore.getData(context, key, defaultValue);
        }

        private static void closeSession() {
            // Close the session. Events within the next MIN_TIME_BETWEEN_SESSIONS_MILLIS seconds
            // will stay in the session.
            // A startSession call within the next MIN_TIME_BETWEEN_SESSIONS_MILLIS seconds
            // will reopen the session.
            isSessionOpen = false;
        }

        private static void sendUserProperties(){
            if(!updatingUserPropsCurrently.getAndSet(true)){
                logWorker.post(new Runnable() {
                    @Override
                    public void run() {
                        sendUserPropertiesToServer();
                    }
                });
            }
        }

        private static void sendUserProperties(long delay){
            if(!updatingUserPropsCurrently.getAndSet(true)){
                logWorker.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sendUserPropertiesToServer();
                    }
                }, delay);
            }
        }

        private static void sendUserPropertiesToServer(){
            httpWorker.post(new Runnable() {
                @Override
                public void run() {
                    updateUserProperties();
                }
            });
        }

        private static void updateUserProperties(){
           /* if(ZeTarget.isDebuggingOn()){
                Log.d(TAG,"httpWorker is uploading user properties now");
            }
*/
            try {
                JSONObject postParams = new JSONObject();
                DbHelper dbHelper = DbHelper.getDatabaseHelper(context);
                JSONObject userProps = dbHelper.getUserProperties();
                if(userProps == null){
                    updatingUserPropsCurrently.set(false);
                    return;
                }
                postParams.put("userProperties",CommonUtils.replaceWithJSONNull(userProps));
                HttpHelper.doPost(Constants.Z_USER_PROPERTIES_LOG_URL,postParams);

            } catch (Exception e) {
                // Just log any other exception so things don't crash on upload
                if(ZeTarget.isDebuggingOn()){
                    Log.e(TAG, "Error in updating user properties", e);
                }
            }
            updatingUserPropsCurrently.set(false);

        }


        private static void initializeDeviceDetails() {

            runOnLogWorker(new Runnable() {

                @Override
                public void run() {

                    deviceDetails.getadditionalDetails();
                    /*if (ZeTarget.isDebuggingOn()) {
                        Log.d(TAG, "Device details initialization finished");
                    }*/
                }
            });
        }

        private static boolean isValidDeviceId(String deviceId){
            //Filter invalid device ids like empty string etc
            return deviceId != null;
        }

        private static synchronized String initializeDeviceId() {

            SharedPreferences preferences = CommonUtils.getSharedPreferences(context);
            String deviceId = preferences.getString(Constants.Z_PREFKEY_DEVICE_ID, null);
            if (deviceId != null) {
                return deviceId;
            }

            /*if(ZeTarget.isDebuggingOn()){
                Log.d(TAG,"DeviceId has to be created");
            }*/

            //TODO check if we can use advertizer id
            String randomId = deviceDetails.generateUUID();
            preferences.edit().putString(Constants.Z_PREFKEY_DEVICE_ID, randomId).apply();
            return randomId;

        }

        private static void startNewSessionIfNeeded(long timestamp) {
            if (!isSessionOpen) {
                long lastEndSessionTime = getEndSessionTime();

                if (timestamp - lastEndSessionTime < Constants.Z_MIN_TIME_BETWEEN_SESSIONS_MILLIS) {
                    // Sessions close enough, set sessionId to previous sessionId

                    SharedPreferences preferences = CommonUtils.getSharedPreferences(context);
                    long previousSessionId = preferences.getLong(Constants.Z_PREFKEY_LAST_END_SESSION_ID,
                            -1);
                    //Log.i("previousSessionId",Long.valueOf(previousSessionId).toString());

                    if (previousSessionId == -1) {
                        // Invalid session Id, create new sessionId

                        sync();
                        startNewSession(timestamp);
                    } else {

                        //Log.i("previousSessionId",Long.valueOf(previousSessionId).toString());
                        sessionId = previousSessionId;
                        //Log.i("sessionId","updated at startNewSessionIfNeeded");
                        /*if(ZeTarget.isDebuggingOn()){
                            Log.d(TAG,"starting new session is not required as very close previous session already exists");
                        }*/
                    }
                } else {
                    // Sessions not close enough, create new sessionId
                    sync();
                    startNewSession(timestamp);
                   /* if(ZeTarget.isDebuggingOn()){
                        Log.d(TAG,"starting new session as previous session was not close enough");
                    }*/
                }
            } else {
                long lastEventTime = getLastEventTime();
                if (timestamp - lastEventTime > sessionTimeoutMillis || sessionId == -1) {
                    sync();
                    startNewSession(timestamp);
                    /*if(ZeTarget.isDebuggingOn()){
                        Log.d(TAG,"starting new session as session timed out");
                    }*/
                }
                else {
                   /* if(ZeTarget.isDebuggingOn()){
                        Log.d(TAG,"Already previous session is open so not starting another session");
                    }*/
                }

            }
        }

        private static void startNewSession(final long timestamp) {
            // Log session start in events
            openSession();

            sessionId = timestamp;
            //Log.i("sessionId","updated at startNewSession()");
            SharedPreferences preferences = CommonUtils.getSharedPreferences(context);
            preferences.edit().putLong(Constants.Z_PREFKEY_LAST_END_SESSION_ID, sessionId).apply();

            logEvent(START_SESSION_EVENT, null, null, timestamp, false);
            logWorker.post(new Runnable() {
                @Override
                public void run() {
                    sendEventToServer(START_SESSION_EVENT, timestamp, Constants.Z_START_SESSION_EVENT_LOG_URL, true);
                }
            });

        }

        private static void sendEventToServer(final String eventType,final long timestamp,final String url, final boolean header){
            httpWorker.post(new Runnable() {
                @Override
                public void run() {
                    sendEvent(eventType, timestamp, url, header);
                }
            });
        }

        private static void sendEvent(String eventType, long timestamp, String url,boolean header){
            /*if(ZeTarget.isDebuggingOn()){
                Log.d(TAG,"Sending "+eventType+" separately");
            }*/

            try {
                JSONObject postParams = new JSONObject();

                postParams.put("appName",CommonUtils.replaceWithJSONNull(DeviceDetails.getApplicationName()));
                postParams.put("OSVersion", CommonUtils.replaceWithJSONNull(DeviceDetails.getOSVersion()));
                postParams.put("OSName",CommonUtils.replaceWithJSONNull(deviceDetails.getOSName()));
                postParams.put("OSFamily",CommonUtils.replaceWithJSONNull(DeviceDetails.getOsFamily()));
                postParams.put("deviceModel", CommonUtils.replaceWithJSONNull(DeviceDetails.getModel()));
                postParams.put("deviceDataProvider", CommonUtils.replaceWithJSONNull(deviceDetails.getCarrier()));
                postParams.put("language", CommonUtils.replaceWithJSONNull(DeviceDetails.getLanguage()));
                String deviceToken = getRegistrationId();
                if(deviceToken != null && deviceToken.length() > 5){
                    postParams.put("deviceToken", CommonUtils.replaceWithJSONNull(deviceToken));
                }

                JSONObject eventParams = new JSONObject();
                eventParams.put("sessionId",CommonUtils.replaceWithJSONNull(sessionId));

                postParams.put("eventParams", CommonUtils.replaceWithJSONNull(eventParams));
                //postParams.put("eventTime", CommonUtils.replaceWithJSONNull(CommonUtils.getCurrentDateTime()));
                postParams.put("ostz", CommonUtils.replaceWithJSONNull(DeviceDetails.getOstz()));


                //postParams.add(new BasicNameValuePair("deviceResoultion", Constants.Z_VERSION));//TODO
                Location location = deviceDetails.getMostRecentLocation();
                if(location != null){
                    postParams.put("isLocationAvailable",CommonUtils.replaceWithJSONNull(true));
                    postParams.put("lastglat", CommonUtils.replaceWithJSONNull(location.getLatitude()));
                    postParams.put("lastglong", CommonUtils.replaceWithJSONNull(location.getLongitude()));
                }
                else{
                    postParams.put("isLocationAvailable",CommonUtils.replaceWithJSONNull(false));
                }
                //postParams.add(new BasicNameValuePair("isPushEnabled", deviceDetails.getVersionName()));//TODO
                //postParams.add(new BasicNameValuePair("appLastOpenedTime", userId));
                //postParams.add(new BasicNameValuePair("lastReceivedCampaignTime", deviceId));

                //postParams.add(new BasicNameValuePair("lastPurchaseMadeTime", deviceDetails.getVersionName()));
                //postParams.add(new BasicNameValuePair("lastCustomEventTime", userId));
                //postParams.add(new BasicNameValuePair("appLastUpdatedTime", deviceId));
                //postParams.add(new BasicNameValuePair("lastAlertSentTime", deviceId));
                HttpHelper.doPost(url,postParams);
            } catch (Exception e) {
                // Just log any other exception so things don't crash on upload
                if(ZeTarget.isDebuggingOn()){
                    Log.e(TAG, "Exception in uploading events", e);
                }
            }
        }


        static void uploadEvents() {
            if (!isContextAndApiKeySet("uploadEvents()")) {
                return;
            }

            logWorker.post(new Runnable() {
                @Override
                public void run() {
                    updateServer();
                }
            });
        }

        static void syncDataStore(){
           /* if(ZeTarget.isDebuggingOn()){
                Log.d(TAG,"syncDataStore() called, asking logWorker to sync");
            }*/
            if (!isContextAndApiKeySet("syncDataStore()")) {
                return;
            }

            logWorker.post(new Runnable() {
                @Override
                public void run() {
                    _syncDataStore();
                }
            });
        }

        private static void checkAndUpdateDataStore(){

            boolean syncSuccess = false;
            try {
                JSONObject postParams = new JSONObject();
                postParams.put("lastDataStoreSynchedTime", CommonUtils.replaceWithJSONNull(DataStore.getDataStoreVersion(context)));
                String response = HttpHelper.doPost(Constants.Z_DATASTORE_SYNCH_URL, postParams);
                if(response != null){
                    final JSONObject jsonResponse = new JSONObject(response);
                    if ("OUT_OF_SYNCH".equals(jsonResponse.getString("status"))) {
                        /*if(ZeTarget.isDebuggingOn()){
                            Log.d(TAG,"DataStore is out of sync, asking logWorker to update local data store");
                        }*/
                        syncSuccess = true;
                        synchingDataStoreCurrently.set(false);
                        logWorker.post(new Runnable() {
                            @Override
                            public void run() {
                                updateDataStore(jsonResponse);
                            }
                        });
                    }
                    else {
                       /* if(ZeTarget.isDebuggingOn()){
                            Log.d(TAG,"DataStore already latest version, not updating local DataStore");
                        }*/
                    }
                }

            } catch (Exception e) {
                // Just log any other exception so things don't crash on upload
                if(ZeTarget.isDebuggingOn()){
                    Log.e(TAG, "Exception in checkAndUpdateDataStore:", e);
                }
            }

            if (!syncSuccess) {
                synchingDataStoreCurrently.set(false);
            }

        }

        private static void updateDataStore(JSONObject newDataStore){
            /*if(ZeTarget.isDebuggingOn()){
                Log.d(TAG,"logWorker id updating local data store with the fetched data store");
            }*/
            try {
                JSONObject variables = newDataStore.getJSONObject("variables");
                if(variables.length() > 0){
                    Map<String,String> values = new HashMap<>();
                    for(int i = 0; i<variables.names().length(); i++){
                        values.put(variables.names().getString(i),variables.getString(variables.names().getString(i)));
                    }
                    DataStore.setData(context, values);
                }

                //In App stuff
                JSONArray texts = newDataStore.getJSONArray("inapptext");
                if(texts.length() > 0){
                    DbHelper dbHelper = DbHelper.getDatabaseHelper(context);
                    if(ZeTarget.isDebuggingOn()){
                        Log.d(TAG,"Found changed texts from server");
                    }
                    for(int i=0; i < texts.length();i++){
                        JSONObject t = texts.getJSONObject(i);
                        String locale = t.getString("locale");
                        boolean needToUpdate = false;
                        if(locale.equalsIgnoreCase(DeviceDetails.getLocaleString())){
                            needToUpdate = true;
                        }
                        dbHelper.clearInAppTextForLocale(locale);
                        JSONArray localeTexts = t.getJSONArray("changed_text");
                        dbHelper.addInAppText(locale,localeTexts.toString());
                        if(needToUpdate){
                            ZeTarget.resetInAppTexts();
                            for(int j=0; j< localeTexts.length();j++){
                                JSONObject kV = localeTexts.getJSONObject(j);
                                inAppTexts.put(kV.getString("key"),kV.getString("value"));
                            }
                            if(ZeTarget.isDebuggingOn()){
                                Log.d(TAG,"Now we should see updated texts just fetched from server");
                            }
                        }
                    }
                }
                DataStore.setDataStoreVersion(context, newDataStore.getString("lastDataStoreSynchedTime"));
            } catch (Exception e){
               if(ZeTarget.isDebuggingOn()){
                    Log.e(TAG, "Exception in updating DataStore:", e);
                }
            }
           /* if(ZeTarget.isDebuggingOn()){
                Log.d(TAG, "DataStore update done, we have latest version now.");
            }*/

            synchingDataStoreCurrently.set(false);
        }

        private static void setData(String key, String value){
            DataStore.setData(context, key, value);
        }

        private static void makeEventUploadPostRequest(String url, String events, final long maxId) {
           /* if(ZeTarget.isDebuggingOn()){
                Log.d(TAG,"httpWorker is uploading events now - "+events);
            }*/

            boolean uploadSuccess = false;
            try {
                JSONObject postParams = new JSONObject();
                postParams.put("eventList",CommonUtils.replaceWithJSONNull(new JSONArray(events)));
                postParams.put("appName",CommonUtils.replaceWithJSONNull(deviceDetails.getVersionName()));
                String response = HttpHelper.doPost(url,postParams);
                if(response !=null){
                    JSONObject jsonResponse = new JSONObject(response);
                    if ("success".equals(jsonResponse.getString("status"))) {
                        uploadSuccess = true;
                        logWorker.post(new Runnable() {
                            @Override
                            public void run() {
                                /*if(ZeTarget.isDebuggingOn()){
                                    Log.d(TAG,"Events upload successful, trying to delete uploaded events");
                                }*/
                                DbHelper dbHelper = DbHelper.getDatabaseHelper(context);
                                dbHelper.removeEvents(maxId);
                                uploadingCurrently.set(false);
                                if (dbHelper.getEventCount() > Constants.Z_EVENT_UPLOAD_THRESHOLD) {
                                    /*if(ZeTarget.isDebuggingOn()){
                                        Log.d(TAG,"Still lot of events exist i.e greater than Z_EVENT_UPLOAD_THRESHOLD, asking logWorker to upload again");
                                    }*/
                                    logWorker.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            updateServer(false);
                                        }
                                    });
                                }
                            }
                        });
                    }
                }

            } catch (Exception e) {
                // Just log any other exception so things don't crash on upload
                if(ZeTarget.isDebuggingOn()){
                    Log.e(TAG, "Failure in uploading events to server", e);
                }
            }

            if (!uploadSuccess) {
                uploadingCurrently.set(false);
            }

        }

        private static void updateServer(boolean limit) {
            if (!uploadingCurrently.getAndSet(true)) {
                DbHelper dbHelper = DbHelper.getDatabaseHelper(context);
                try {
                    long endSessionId = getEndSessionId();
                    Pair<Long, JSONArray> pair = dbHelper.getEvents(endSessionId,
                            limit ? Constants.Z_EVENT_UPLOAD_MAX_BATCH_SIZE : -1);
                    final long maxId = pair.first;
                    final JSONArray events = pair.second;
                    if(events.length() == 0){
                        /*if(ZeTarget.isDebuggingOn()){
                            Log.d(TAG,"httpWorker tried uploading events but found zero event, hence not making server request");
                        }*/
                        uploadingCurrently.set(false);
                        return;
                    }
                    httpWorker.post(new Runnable() {
                        @Override
                        public void run() {
                            /*if(ZeTarget.isDebuggingOn()){
                                Log.d(TAG,"Asking httpWorker to upload "+events.length()+" events");
                            }*/
                            makeEventUploadPostRequest(Constants.Z_EVENT_LOG_URL, events.toString(),
                                    maxId);
                        }
                    });
                } catch (JSONException e) {
                    uploadingCurrently.set(false);
                    if(ZeTarget.isDebuggingOn()){
                        Log.e(TAG,"Error in uploading events",e);
                    }
                }
            }
            else {
                /*if(ZeTarget.isDebuggingOn()){
                    Log.d(TAG,"Already uploading events to the server hence not uploading now");
                }*/
            }
        }

        private static void _syncDataStore() {
            if (!synchingDataStoreCurrently.getAndSet(true)) {
                if(ZeTarget.isDebuggingOn()){
                    //Log.d(TAG,"Asking httpWorker to sync datastore");
                }
                httpWorker.post(new Runnable() {
                    @Override
                    public void run() {
                        checkAndUpdateDataStore();
                    }
                });
            }
            else {
                if(ZeTarget.isDebuggingOn()){
                    //Log.d(TAG,"sync datastore is already going on, so not syncing now");
                }
            }
        }



        private static void updateServer() {
            updateServer(true);
        }

        private static void updateServerLater(long delayMillis) {
            if (!updateScheduled.getAndSet(true)) {

                logWorker.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        updateScheduled.set(false);
                        updateServer();
                    }
                }, delayMillis);
            }
        }

        private static void openSession() {
            //clearEndSession();
            isSessionOpen = true;
        }

        /**
         * Method to log custom events
         *
         * @param eventType the type of event e.g click,purchase etc
         */
        public static void logEvent(String eventType) {
            logEvent(eventType, null);
        }

        /**
         * Method to log custom events with additional parameters
         *
         * @param eventType the type of event e.g click,purchase etc
         * @param eventProperties additional parameters. e.g for purchase event additional parameters might be- amount,quantity etc
         */
        public static void logEvent(String eventType, JSONObject eventProperties) {
            /*if(ZeTarget.robolectricTesting) {
                System.out.println("ZETARGET: logEvent - " + eventType);
            }*/
            checkedLogEvent(eventType, eventProperties, null, System.currentTimeMillis(), true);
        }

        private static void checkedLogEvent(final String eventType, final JSONObject eventProperties,
                                            final JSONObject apiProperties, final long timestamp, final boolean checkSession) {
            if (TextUtils.isEmpty(eventType)) {
                if(ZeTarget.isDebuggingOn()){
                    Log.e(TAG, "Argument eventType cannot be null or blank in logEvent()");
                }
                return;
            }
            if (!isContextAndApiKeySet("logEvent()")) {
                return;
            }
            runOnLogWorker(new Runnable() {
                @Override
                public void run() {
                    logEvent(eventType, eventProperties, apiProperties, timestamp, checkSession);
                }
            });

        }
        static void uncheckedLogEvent(String eventType){
            uncheckedLogEvent(eventType, null);
        }
        static void uncheckedLogEvent(final String eventType,final JSONObject eventProperties){
            if (TextUtils.isEmpty(eventType)) {
                Log.e(TAG, "Argument eventType cannot be null or blank in logEvent()");
                return;
            }
            runOnLogWorker(new Runnable() {
                @Override
                public void run() {
                    logEvent(eventType, eventProperties, null, System.currentTimeMillis(), false);
                }
            });
        }

        private static long logEvent(final String eventType, JSONObject eventProperties,
                                     JSONObject apiProperties, long timestamp, boolean checkSession) {
            campaignWorker.post(new Runnable() {
                @Override
                public void run() {
                    if (context != null) {
                        Intent intentToStartCampaignHandler = new Intent(context, CampaignHandlingService.class);
                        intentToStartCampaignHandler.putExtra("action", Constants.Z_INTENT_EXTRA_CAMPAIGNS_ACTION_KEY_VALUE_HANDLE_SIMPLE_EVENT_TRIGGERS);
                        intentToStartCampaignHandler.putExtra("eventType", eventType);
                        context.startService(intentToStartCampaignHandler);
                    }
                }
            });
            if (checkSession) {
                startNewSessionIfNeeded(timestamp);
            }
            setLastEventTime(timestamp);

            JSONObject event = new JSONObject();
            try {
                event.put("eventName", CommonUtils.replaceWithJSONNull(eventType));
                if(eventProperties == null){
                    eventProperties = new JSONObject();
                }
                //Log.i("sessionId","In Log Event is "+Long.valueOf(sessionId).toString());
                eventProperties.put("sessionId",CommonUtils.replaceWithJSONNull(sessionId));
                event.put("eventParams",CommonUtils.replaceWithJSONNull(eventProperties));
                event.put("sessionId",CommonUtils.replaceWithJSONNull(sessionId));


                event.put("eventTime", CommonUtils.replaceWithJSONNull(CommonUtils.getCurrentDateTime(timestamp)));
            } catch (JSONException e) {
                if(ZeTarget.isDebuggingOn()){
                    Log.e(TAG, "failure in logging event", e);
                }
            }

            return logEvent(event);
        }



        private static long logEvent(JSONObject event) {
            DbHelper dbHelper = DbHelper.getDatabaseHelper(context);
            long eventId = dbHelper.addEvent(event.toString());

            if (dbHelper.getEventCount() >= Constants.Z_EVENT_MAX_COUNT) {
                dbHelper.removeEvents(dbHelper.getNthEventId(Constants.Z_EVENT_REMOVE_BATCH_SIZE));
            }

            if (dbHelper.getEventCount() >= Constants.Z_EVENT_UPLOAD_THRESHOLD) {
                updateServer();
            } else {
                updateServerLater(Constants.Z_EVENT_UPLOAD_PERIOD_MILLIS);
            }
            return eventId;
        }

        private static void runOnLogWorker(Runnable r) {
            if (Thread.currentThread() != logWorker) {
                logWorker.post(r);
            } else {
                r.run();
            }
        }


        private static String getSavedUserId(){
            return CommonUtils.getSharedPreferences(context).getString(Constants.Z_PREFKEY_USER_ID,null);
        }

        /**
         * Method to set custom user id.
         *
         * @param userId the string value to set
         */
        public synchronized static void setUserId(String userId){
            setUserId(context,userId);
        }

        private synchronized static void setUserIdAtInit(Context context, String userId){
            CommonUtils.getSharedPreferences(context).edit().putString(Constants.Z_PREFKEY_OLD_USER_ID, getUserId()).commit();
            CommonUtils.getSharedPreferences(context).edit().putString(Constants.Z_PREFKEY_USER_ID, userId).commit();
            ZeTarget.userId = userId;
        }

        private synchronized static void setUserId(Context context, String userId){
            CommonUtils.getSharedPreferences(context).edit().putString(Constants.Z_PREFKEY_OLD_USER_ID, getUserId()).commit();
            CommonUtils.getSharedPreferences(context).edit().putString(Constants.Z_PREFKEY_USER_ID, userId).commit();
            ZeTarget.userId = userId;
            logWorker.post(new Runnable() {
                @Override
                public void run() {
                    setUserId();
                }
            });
        }

        private static void setUserId(){
            httpWorker.post(new Runnable() {
                @Override
                public void run() {
                    setUserOnServer();
                }
            });
        }

        private static void setUserOnServer(){
            /*if(ZeTarget.isDebuggingOn()){
                Log.d(TAG, "Sending new user id to the server.");
            }*/

            try {
                JSONObject postParams = new JSONObject();
                postParams.put("oldUserID",getOldUserId());
                HttpHelper.doPost(Constants.Z_SET_USER_URL,postParams);
            } catch (Exception e) {
                // Just log any other exception so things don't crash on upload
                if(ZeTarget.isDebuggingOn()){
                    Log.e(TAG, "Failure in uploading userProperties", e);
                }
            }
        }

        private synchronized static boolean isContextAndApiKeySet(String methodName) {
            if (context == null) {
                if(ZeTarget.isDebuggingOn()){
                    Log.e(TAG, "context cannot be null, set context with initialize() before calling "
                            + methodName);
                }
                return false;
            }
            if (TextUtils.isEmpty(apiKey)) {
                if(ZeTarget.isDebuggingOn()){
                    Log.e(TAG, "apiKey cannot be null or empty, set apiKey with initialize() before calling "
                              + methodName);
                }
                return false;
            }
            return true;
        }

        private static String getUUID(){
            return UUID.randomUUID().toString();//TODO customize it
        }

        private static void setContext(Context context){
            ZeTarget.context = context;
        }

        private static void setApiKey(String apiKey){
            ZeTarget.apiKey = apiKey;
        }

        private static long getLastEventTime() {
            return getSharedPreferenceValueByKey(Constants.Z_PREFKEY_LAST_SESSION_TIME);


        }

        private static void setLastEventTime(long timestamp) {
            SharedPreferences preferences = CommonUtils.getSharedPreferences(context);
            preferences.edit().putLong(Constants.Z_PREFKEY_LAST_SESSION_TIME, timestamp).apply();
        }

        private static void clearEndSession() {
            SharedPreferences preferences = CommonUtils.getSharedPreferences(context);
            preferences.edit().remove(Constants.Z_PREFKEY_LAST_END_SESSION_TIME)
                    .remove(Constants.Z_PREFKEY_LAST_END_SESSION_EVENT_ID)
                    .remove(Constants.Z_PREFKEY_LAST_END_SESSION_ID).apply();
            sessionId=-1;
        }

        private static long getEndSessionTime() {
            return getSharedPreferenceValueByKey(Constants.Z_PREFKEY_LAST_END_SESSION_TIME);

        }

        private static long getEndSessionId() {
            return getSharedPreferenceValueByKey(Constants.Z_PREFKEY_LAST_END_SESSION_EVENT_ID);
        }

        private static long getSharedPreferenceValueByKey(String key){
            SharedPreferences preferences = CommonUtils.getSharedPreferences(context);
            return preferences.getLong(key, -1);
        }

        private static String getLastCampaignSyncTime(){
            return CommonUtils.getSharedPreferences(context).getString(Constants.Z_PREFKEY_LAST_CAMPAIGN_SYNC_TIME, "");
        }

        private static String getOldUserId(){
            return CommonUtils.getSharedPreferences(context).getString(Constants.Z_PREFKEY_OLD_USER_ID,"");
        }

        private static void setLastCampaignSyncTime(String lastCampaignSyncTime){
            CommonUtils.getSharedPreferences(context).edit().putString(Constants.Z_PREFKEY_LAST_CAMPAIGN_SYNC_TIME,lastCampaignSyncTime).apply();
        }

        private static void saveRegistrationId(String registrationId){
            SharedPreferences.Editor editor = CommonUtils.getSharedPreferences(context).edit();
            editor.putString(Constants.Z_PREFKEY_GCM_REGISTRATION_ID, registrationId);
            editor.putInt(Constants.Z_PREFKEY_APP_VERSION, DeviceDetails.getAppVersionCode());
            editor.putLong(Constants.Z_PREFKEY_GCM_REGISTRATION_ID_SYNC_TIME,System.currentTimeMillis());
            editor.apply();
        }

        /**
         * Method to log a purchase completed event
         * @param grandTotal The double value of Grand total of the purchase made
         */

        public static void logPurchaseCompleted(Double grandTotal) {
            logPurchaseCompleted(null, grandTotal, null, null, null, null, null, null, null);
        }

        /**
         * Method to log a purchase completed event
         * @param grandTotal The double value of Grand total of the purchase made
         * @param quantity The integer value of the number of items purchased.
         */

        public static void logPurchaseCompleted(Double grandTotal, Integer quantity){
            logPurchaseCompleted(null, grandTotal, null, null, null, quantity, null, null, null);
        }

        /**
         *  Method to log a purchase completed event
         * @param currency The String representation of the currency of transaction.
         * @param grandTotal The double value of Grand total of the purchase made.
         * @param quantity The integer value of the number of items purchased.
         *
         */
        public static void logPurchaseCompleted(String currency, Double grandTotal, Integer quantity){
            logPurchaseCompleted(currency, grandTotal, null, null, null, quantity, null, null, null);
        }

        /**
         *  Method to log a purchase completed event
         * @param grandTotal The double value of Grand total of the purchase made.
         * @param total The double value of the Total value of purchase excluding shipping and tax.
         * @param shipping The shipping costs for the purchased items.
         * @param tax The tax levied on the purchased goods.
         */
        public static void logPurchaseCompleted(Double grandTotal, Double total, Double shipping, Double tax) {
            logPurchaseCompleted(null, grandTotal, total, shipping, tax, null, null, null, null);
        }

        /**
         * Method to log a purchase completed event
         * @param grandTotal The double value of Grand total of the purchase made.
         * @param total The double value of the Total value of purchase excluding shipping and tax.
         * @param shipping The shipping costs for the purchased items.
         * @param tax The tax levied on the purchased goods.
         * @param quantity The integer value of the number of Items purchased.
         */
        public static void logPurchaseCompleted(Double grandTotal, Double total, Double shipping, Double tax, Integer quantity) {
            logPurchaseCompleted(null, grandTotal, total, shipping, tax, quantity, null, null, null);
        }

        /**
         *  Method to log a purchase completed event
         * @param currency The String representation of the currency of transaction.
         * @param grandTotal The double value of Grand total of the purchase made.
         * @param total The double value of the Total value of purchase excluding shipping and tax.
         * @param shipping The shipping costs for the purchased items.
         * @param tax The tax levied on the purchased goods.
         * @param quantity The integer value of the number of Items purchased.
         */
        public static void logPurchaseCompleted(String currency, Double grandTotal, Double total, Double shipping, Double tax, Integer quantity) {
            logPurchaseCompleted(currency, grandTotal, total, shipping, tax, quantity, null, null, null);
        }

        /**
         * Method to log a purchase completed event
         * @param currency The String representation of the currency of transaction.
         * @param grandTotal The double value of Grand total of the purchase made.
         * @param total The double value of the Total value of purchase excluding shipping and tax.
         * @param shipping The shipping costs for the purchased items.
         * @param tax The tax levied on the purchased goods.
         * @param quantity The integer value of the number of Items purchased.
         * @param orderId The String representation of the order id if any
         * @param receiptId The String representation of the receipt id if any
         * @param productsku The String representation of the product sku if any
         *
         * All values except @param grandTotal are optional for now.
         */
        public static void logPurchaseCompleted(String currency, Double grandTotal, Double total,
                                                Double shipping, Double tax, Integer quantity,
                                                String orderId, String receiptId, String productsku) {
            JSONObject purchaseDetails = new JSONObject();
            try {
                if(currency!=null) {
                    purchaseDetails.put("currency", currency);
                }
                if(grandTotal!=null){
                    purchaseDetails.put("grand_total",grandTotal);
                }else{
                    if(ZeTarget.isDebuggingOn()){
                        //Log.i("PurchaseCompletedEvent:", "grandTotal param missing in the method call");
                    }
                    return;
                }
                if(total!=null){
                    purchaseDetails.put("total",total);
                }
                if(shipping!=null){
                    purchaseDetails.put("shipping",shipping);
                }
                if(tax!=null){
                    purchaseDetails.put("tax",tax);
                }
                if(quantity!=null) {
                    purchaseDetails.put("quantity", quantity);
                }
                if(orderId!=null){
                    purchaseDetails.put("orderid",orderId);
                }
                if(receiptId!=null){
                    purchaseDetails.put("receiptid",receiptId);
                }
                if(productsku!=null){
                    purchaseDetails.put("productsku",productsku);
                }
            } catch (JSONException e) {
                if(ZeTarget.isDebuggingOn()){
                    Log.e(TAG,"Failure in adding Purchase Details",e);
                }
            }
            /*if(ZeTarget.robolectricTesting) {
                System.out.println("ZETARGET: logging purchase completed event");
            }*/
            logEvent(Constants.Z_PURCHASE_COMPLETED_EVENT, purchaseDetails);
        }

        /**
         * Method to log Purchase Attempted event
         */
        public static void logPurchaseAttempted() {
            /*if(ZeTarget.robolectricTesting) {
                System.out.println("ZETARGET: logging purchase attempted event");
            }*/
            logEvent(Constants.Z_PURCHASE_ATTEMPTED_EVENT);
        }
        /**
         * Method to set user property- first name
         */
        public static void setFirstName(String name){
            setUserProperty(Constants.ZeTarget_keyForUserPropertyFName, name);
        }
        /**
         * Method to set user property- last name
         */
        public static void setLastName(String name){
            setUserProperty(Constants.ZeTarget_keyForUserPropertyLName,name);
        }
        /**
         * Method to set user property- age
         */
        public static void setAge(String age){
            setUserProperty(Constants.ZeTarget_keyForUserPropertyAge, age);
        }
        /**
         * Method to set user property- Date of Birth
         * the parameter dob should be of the format yyyymmdd
         */
        public static void setDOB(String dob){
            setUserProperty(Constants.ZeTarget_keyForUserPropertyDOB,dob);
            int year = Integer.valueOf(dob.substring(0,4));
            int month = Integer.valueOf(dob.substring(4,6));
            int day = Integer.valueOf(dob.substring(6,8));
            Calendar cal =Calendar.getInstance();
            int currentYear = cal.get(Calendar.YEAR);
            int currentMonth = cal.get(Calendar.MONTH);
            int currentDay = cal.get(Calendar.DAY_OF_MONTH);
            int age = currentYear - year;
            if(month<currentMonth||(month==currentMonth&&day<currentDay)){
                age--;
            }
            setAge(age + "");
        }
        /**
         * Method to set user property- Gender
         */
        public static void setGender(String gender){
            setUserProperty(Constants.ZeTarget_keyForUserPropertyGender, gender);
        }

        public static Context attachBaseContext(Context ctx, final Activity activity)  {
            if(!ZeTarget.inAppTextsLoaded) {
                ZeTarget.fetchInAppTextsFromDb(ctx);
            }
            return ZContext.getInstance(ctx,activity);
        }

        public static synchronized void fetchInAppTextsFromDb(Context ctx){
            DbHelper dbHelper = DbHelper.getDatabaseHelper(ctx);
            JSONArray localeTexts = dbHelper.getInAppTexts(DeviceDetails.getLocaleString());
            if(localeTexts != null){
                for(int j=0; j< localeTexts.length();j++){
                    JSONObject kV = null;
                    try {
                        kV = localeTexts.getJSONObject(j);
                        inAppTexts.put(kV.getString("key"),kV.getString("value"));
                    } catch (JSONException e) {
                        if(ZeTarget.isDebuggingOn()) {
                            Log.e(TAG, "Exception in fetchInAppTextsFromDb",e);
                        }
                    }
                }
                if(ZeTarget.isDebuggingOn()){
                    Log.d(TAG,"Loaded changed texts from local db");
                }
            }
            inAppTextsLoaded = true;
        }

        static void setText(Activity activity){
            Map<String,JSONArray> textsForThisActivity = ZeTarget.getStringIdMap().get(ZeTarget.getActivityClassName(activity));
            if(textsForThisActivity == null){
                return;
            }
            Iterator it = textsForThisActivity.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                String replacement = ZeTarget.getInAppTexts().get(pair.getKey());
                //String replacement = "Done done";
                if(replacement != null){
                    try {
                        JSONArray textsIdArray = new JSONArray(pair.getValue().toString());
                        for(int i=0;i<textsIdArray.length();i++){
                            JSONObject obj = textsIdArray.getJSONObject(i);
                            String methodType = obj.getString(Constants.InAppTexts.KEY_METHODTYPE);
                            int Id = obj.getInt("id");
                            View v = activity.findViewById(Id);
                            if(v == null){
                                continue;
                            }
                            if(v instanceof TextView){
                                ((TextView) v).setText(replacement);
                                if(ZeTarget.isDebuggingOn()){
                                    Log.d(TAG,"Changing view: "+Id+" text to"+replacement);
                                }
                            }
                            else if(v instanceof Button){
                                ((Button) v).setText(replacement);
                                if(ZeTarget.isDebuggingOn()){
                                    Log.d(TAG,"Changing view: "+Id+" text to"+replacement);
                                }
                            }
                            else if(v instanceof EditText){
                                if(methodType.equals(Constants.InAppTexts.METHOD_SETHINT)){
                                    ((EditText) v).setHint(replacement);
                                    if(ZeTarget.isDebuggingOn()){
                                        Log.d(TAG,"Changing view: "+Id+" hint to"+replacement);
                                    }
                                }
                                if(methodType.equals(Constants.InAppTexts.METHOD_SETTEXT)){
                                    ((EditText) v).setText(replacement);
                                    if(ZeTarget.isDebuggingOn()){
                                        Log.d(TAG,"Changing view: "+Id+" text to"+replacement);
                                    }
                                }
                            }
                            //TODO check apart from textview and button where can we setText
                        }
                    } catch (JSONException e) {
                        if(ZeTarget.isDebuggingOn()){
                            Log.e(TAG,"JSONException",e);
                        }
                    }
                }
            }
        }

        static String getActivityClassName(Activity activity){
            return activity.getClass().getSimpleName();
        }

        static void parseLayout(final Resources resources, final int layoutId, final String currentActivityName){
            needTocallsetText.set(true);
            if(!ZeTarget.getParsedLayouts().contains(layoutId)){
                logWorker.post(new Runnable() {
                    @Override
                    public void run() {
                        _parseLayout(resources,layoutId,currentActivityName);
                    }
                });
            }
        }

        private static void _parseLayout(Resources resources, int layoutId, String currentActivityName){
            int eventType = 0;
            try {
                XmlResourceParser output = resources.getLayout(layoutId);
                eventType = output.getEventType();
                Map<String,JSONArray> textIdMap = new HashMap<String,JSONArray>();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        if (output.getName().equals("TextView") || output.getName().equals("Button") ||
                                output.getName().equals("EditText")) {
                            int androidId = 0;
                            String textKey = null;
                            String hintKey = null;
                            for (int i = 0; i < output.getAttributeCount(); i++) {
                                if (output.getAttributeName(i).equals("id")) {
                                    androidId = Integer.parseInt(output.getAttributeValue(i).replace("@", ""));
                                }
                                if (output.getAttributeName(i).equals("text")) {
                                    //Check if text is referenced from strings.xml
                                    if (output.getAttributeValue(i).charAt(0) == '@') {
                                        int stringId = Integer.parseInt(output.getAttributeValue(i).replace("@", ""));
                                        textKey = getKeyFromResourceName(resources, stringId);
                                    }
                                }
                                if (output.getAttributeName(i).equals("hint")) {
                                    //Check if text is referenced from strings.xml
                                    if (output.getAttributeValue(i).charAt(0) == '@') {
                                        int stringId = Integer.parseInt(output.getAttributeValue(i).replace("@", ""));
                                        hintKey = getKeyFromResourceName(resources, stringId);
                                    }
                                }
                            }
                            if (androidId != 0) {
                                if (textKey != null) {
                                    JSONObject jsonObject = new JSONObject()
                                            .put(Constants.InAppTexts.KEY_METHODTYPE, Constants.InAppTexts.METHOD_SETTEXT)
                                            .put("id", androidId);
                                    JSONArray existingForText = textIdMap.get(textKey);
                                    if(existingForText != null){
                                        textIdMap.put(textKey, existingForText.put(jsonObject));
                                    }
                                    else {
                                        textIdMap.put(textKey, new JSONArray().put(jsonObject));
                                    }
                                    if(ZeTarget.isDebuggingOn()){
                                        Log.d(TAG, "Found android:text " + textKey + " ,android:id " + androidId);
                                    }
                                }
                                if (hintKey != null) {
                                    JSONObject jsonObject = new JSONObject()
                                            .put(Constants.InAppTexts.KEY_METHODTYPE, Constants.InAppTexts.METHOD_SETHINT)
                                            .put("id", androidId);
                                    JSONArray existingForText = textIdMap.get(hintKey);
                                    if(existingForText != null){
                                        textIdMap.put(hintKey, existingForText.put(jsonObject));
                                    }
                                    else {
                                        textIdMap.put(hintKey, new JSONArray().put(jsonObject));
                                    }
                                    if(ZeTarget.isDebuggingOn()){
                                        Log.d(TAG,"Found android:hint "+hintKey+" ,android:id "+androidId);
                                    }
                                }
                            }
                        }
                    }
                    eventType = output.next();
                }
                if(textIdMap.size() > 0){
                    ZeTarget.addStringIdMap(currentActivityName,textIdMap);
                }
            } catch (XmlPullParserException e) {
                if(ZeTarget.isDebuggingOn()){
                    Log.e(TAG,"XmlPullParserException",e);
                }
            } catch (IOException e) {
                if(ZeTarget.isDebuggingOn()){
                    Log.e(TAG,"IOException",e);
                }
            } catch (JSONException e) {
                if(ZeTarget.isDebuggingOn()){
                    Log.e(TAG,"JSONException",e);
                }
            }
            ZeTarget.addParsedLayouts(layoutId);
        }

        static String getKeyFromResourceName(Resources resources, int resourceId){
            //getResourceName(id) = "com.zemoso.zetarget.sampleapp:string/hello_world"
            String resourceName = resources.getResourceName(resourceId);
            if(resourceName == null){
                return null;
            }
            return resourceName.substring(resourceName.lastIndexOf("/")+1,resourceName.length());
        }
    }
