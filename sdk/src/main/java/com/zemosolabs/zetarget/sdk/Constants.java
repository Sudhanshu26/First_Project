package com.zemosolabs.zetarget.sdk;

/**
 * Created by praveen on 19/01/15.
 */
class Constants {
    static final String Z_PLATFORM = "Android";
    static final String Z_VERSION = "0.4";
    static final String Z_SDK_ID = Z_PLATFORM+"-"+Z_VERSION;
    public static final long Z_TIMEOUT_BETWEEN_IN_APP = 60000l;
    static String Z_BASE_URL = "http://api.zetarget.com/";
    //static String Z_BASE_URL = "http://devapi.zetarget.com/";
    static final String Z_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss Z";

    //static final String Z_API_VERSION = "0.1";
    static final String Z_NAMESPACE = Constants.class.getPackage().getName();
    static final String Z_SHARED_PREFERENCE_FILE_NAME = Z_NAMESPACE;
    static final String Z_SHARED_PREFERENCE_DATASTORE_FILE_NAME = Z_NAMESPACE+".datastore";
    static final String Z_PREFKEY_USER_ID = Z_NAMESPACE+"userId";
    static final String Z_PREFKEY_DEVICE_ID = Z_NAMESPACE+"deviceId";
    static final String Z_PREFKEY_OLD_USER_ID = Z_NAMESPACE+"old_userId";
    static final String Z_PREFKEY_FIRSTTIME_FLAG = Z_NAMESPACE+".firsttime";
    static final String Z_PREFKEY_GCM_REGISTRATION_ID = Z_NAMESPACE+".gcm_registration_id";
    static final String Z_PREFKEY_GCM_REGISTRATION_ID_SYNC_TIME = Z_NAMESPACE+".gcm_registration_id_sync_time";
    static final long Z_GCM_REGISTRATION_ID_RENEWAL_PERIOD = 7*24*3600*1000;//1 day
    static final String ZeTarget_keyForUserPropertyFName = "fname";
    static final String ZeTarget_keyForUserPropertyLName = "lname";
    static final String ZeTarget_keyForUserPropertyAge = "age";
    static final String ZeTarget_keyForUserPropertyDOB = "dob";
    static final String ZeTarget_keyForUserPropertyGender = "gender";


    static final String Z_PREFKEY_APP_VERSION = Z_NAMESPACE+".app_version";

    //DataStore
    static final String Z_PREFKEY_LAST_DATASTORE_SYNC_TIME = Z_NAMESPACE+".last_datastore_sync_time";
    static final String Z_PREFKEY_LAST_DATASTORE_VERSION = Z_NAMESPACE+".last_datastore_version";
    //static String Z_DATASTORE_SYNCH_URL = "http://private-anon-fe9d7be63-dummysdkapi.apiary-mock.com/fetchDatastore";
    static final String Z_DATASTORE_SYNCH_URL = "fetchDatastore";



    //session related
    static final long Z_SESSION_TIMEOUT = 30*1000;//in milliseconds
    static final String Z_SESSION_START_EVENT = "zmobile.session_started";
    static final String Z_INIT_EVENT = "zmobile.app_init";
    static final String Z_SESSION_END_EVENT = "zmobile.session_ended";
    static final String Z_CAMPAIGN_VIEWED_EVENT = "zmobile.campaign_viewed";
    static final String Z_CAMPAIGN_REJECTED_EVENT = "zmobile.campaign_rejected";
    static final String Z_SESSION_END_EVENT_SESSION_LENGTH_PARAMS = "session_length";
//    static final String Z_CAMPAIGN_SHOW_LATER_EVENT = "zmobile.campaign_later";
//    static final String Z_CAMPAIGN_RATE_EVENT = "zmobile.campaign_rate";
//    static final String Z_CAMPAIGN_DONOT_SHOW_EVENT = "zmobile.campaign_dont_show";
    static long Z_MIN_TIME_BETWEEN_SESSIONS_MILLIS = 15 * 1000; // 15s
    static final String Z_PREFKEY_LAST_SESSION_TIME = Z_NAMESPACE+".last_session_time";
    static final String Z_PREFKEY_LAST_END_SESSION_TIME = Z_NAMESPACE+".last_end_session_time";
    static final String Z_PREFKEY_LAST_END_SESSION_ID = Z_NAMESPACE+".last_end_session_id";
    static final String Z_PREFKEY_LAST_END_SESSION_EVENT_ID = Z_NAMESPACE+".last_end_session_event_id";


    //DB related constants
    static final String Z_DB_NAME = Z_NAMESPACE;
    static final int Z_DB_VERSION = 10;
    static final String Z_DB_EVENT_TABLE_NAME = "events";
    static final String Z_DB_EVENT_ID_FIELD_NAME = "id";
    static final String Z_DB_EVENT_EVENTS_FIELD_NAME = "event";

    static final String Z_DB_PROMOTION_TABLE_NAME = "promotions";
    static final String Z_DB_PROMOTION_ID_FIELD_NAME = "id";
    static final String Z_DB_PROMOTION_PROMOTION_FIELD_NAME = "promotion";

    static final String Z_DB_SCREEN_FIX_TABLE_NAME ="screenFix";
    static final String Z_DB_SCREEN_FIX_ID_FIELD_NAME = "id";
    static final String Z_DB_SCREENFIX_PROMOTION_FIELD_NAME = "changes";

    static final String Z_DB_GEO_CAMPAIGNS_TABLE_NAME = "geoCampaigns" ;
    static final String Z_DB_GEO_CAMPAIGNS_ID_FIELD_NAME = "id";
    static final String Z_DB_GEO_CAMPAIGNS_PROMOTION_FIELD_NAME = "geoPromotion";

    static final String Z_DB_SIMPLE_EVENT_CAMPAIGNS_TABLE_NAME = "simpleEventCampaigns" ;
    static final String Z_DB_SIMPLE_EVENT_CAMPAIGNS_ID_FIELD_NAME = "id";
    static final String Z_DB_SIMPLE_EVENT_CAMPAIGNS_PROMOTION_FIELD_NAME = "simpleEventPromotion";

    static final String Z_DB_SUPPRESSION_LOGIC_TABLE_NAME = "suppressionLogic";
    static final String Z_DB_SUPPRESSION_LOGIC_ID_FIELD_NAME = "id";
    static final String Z_DB_SUPPRESSION_LOGIC_CAMPAIGN_ID_FIELD_NAME="campaign_id";

    static final String Z_DB_USER_PROPERTIES_TABLE_NAME = "userproperties";
    static final String Z_DB_USER_PROPERTIES_ID_FIELD_NAME = "id";

    static final String Z_DB_INAPPTEXT_TABLE_NAME = "inapptexts";

    static final String Z_PURCHASE_COMPLETED_EVENT = "zmobile.purchase_completed";
    static final String Z_PURCHASE_ATTEMPTED_EVENT = "zmobile.purchase_attempt_start";

    //Events related
    static final long Z_EVENT_UPLOAD_THRESHOLD = 10;
    static int Z_EVENT_UPLOAD_MAX_BATCH_SIZE = 15;
    //static String Z_EVENT_LOG_URL = "http://private-anon-fe9d7be63-dummysdkapi.apiary-mock.com/sendEvent";
    static final String Z_EVENT_LOG_URL = "sendEvent";

    //static String Z_SET_USER_URL = "http://private-anon-fe9d7be63-dummysdkapi.apiary-mock.com/changeUser";
    static final String Z_SET_USER_URL = "changeUser";
    //static String Z_START_SESSION_EVENT_LOG_URL = "http://private-anon-fe9d7be63-dummysdkapi.apiary-mock.com/sessionStart";
    static final String Z_START_SESSION_EVENT_LOG_URL = "sessionStart";
    //static String Z_INIT_LOG_URL = "http://private-anon-fe9d7be63-dummysdkapi.apiary-mock.com/init";
    static final String Z_INIT_LOG_URL = "init";
    static String Z_SEND_SNAPSHOT_URL = "synchScreenToServer" ;

    //Promotions related
    static final String Z_PROMOTION_URL = "fetchPromo";
    static final String Z_PREFKEY_LAST_CAMPAIGN_SYNC_TIME = Z_NAMESPACE+".lastcampaignsynctime";

    static int Z_EVENT_MAX_COUNT = 100;
    static long Z_EVENT_REMOVE_BATCH_SIZE = 100;
    static long Z_EVENT_UPLOAD_PERIOD_MILLIS = 15*1000;

    //User Properties
    //static String Z_USER_PROPERTIES_LOG_URL = "http://private-9d06c-dummysdkapi.apiary-mock.com/sendUserProperties";
    static final String Z_USER_PROPERTIES_LOG_URL = "sendUserProperties";
    static long Z_USER_PROPS_UPLOAD_PERIOD_MILLIS = 15*1000;

    static final String Z_BUNDLE_KEY_PUSH_NOTIFICATION_CAMPAIGN_ID = Z_NAMESPACE+".pushNotificationCampaignId";

    static final String Z_CAMPAIGN_TYPE_GEOCAMPAIGN = "GEO";
    static final String Z_CAMPAIGN_TYPE_SIMPLE_EVENT_CAMPAIGN = "SIMPLE EVENT";
    static final String Z_CAMPAIGN_TYPE_SCREEN_FIX_CAMPAIGN = "screenFix";
    static final String Z_CAMPAIGN_TYPE_BEACON_CAMPAIGN = "IBEACON";
    static final String Z_CAMPAING_TYPE_PROMOTION_CAMPAIGN = "promotion";

    static final String Z_INTENT_EXTRA_CAMPAIGNS_ACTION_KEY_VALUE_UPDATE_CAMPAIGNS = Z_NAMESPACE+".syncDatabaseToLiveCampaigns";
    static final String Z_INTENT_EXTRA_CAMPAIGNS_ACTION_KEY_VALUE_HANDLE_GEO_TRIGGERS = Z_NAMESPACE+".handleGeofenceTriggers";
    static final String Z_INTENT_EXTRA_CAMPAIGNS_ACTION_KEY_VALUE_HANDLE_SIMPLE_EVENT_TRIGGERS = Z_NAMESPACE+".handleSimpleEventTriggers";

    static final String DEFAULT_SCREEN = Z_NAMESPACE+".any_screen";
    static final String KEY_IN_APP_LAST_SHOWN_TIME = Z_NAMESPACE+".inAppLastShownTimeInMS";

    public static final String Z_EVENT_TYPE = Z_NAMESPACE+".eventType";
    public static final String Z_CAMPAIGN_TYPE = Z_NAMESPACE+".campaignType" ;

    static final class InAppTexts {
        static final String METHOD_SETTEXT = "setText";
        static final String METHOD_SETHINT = "setHint";
        public static final String KEY_METHODTYPE = "methodType";
    }

}
