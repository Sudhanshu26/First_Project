package com.zemosolabs.zetarget.sdk;

/**
 * Created by praveen on 21/01/15.
 */
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;
import android.util.Pair;

class DbHelper extends SQLiteOpenHelper {

    private static DbHelper instance;
    private static final String TAG = "ZeTarget.DbHelper";

    private static final String EVENT_TABLE_NAME = Constants.Z_DB_EVENT_TABLE_NAME;
    private static final String PROMOTION_TABLE_NAME = Constants.Z_DB_PROMOTION_TABLE_NAME;
    private static final String GEO_CAMPAIGN_TABLE_NAME = Constants.Z_DB_GEO_CAMPAIGNS_TABLE_NAME;
    private static final String SCREEN_FIX_TABLE_NAME = Constants.Z_DB_SCREEN_FIX_TABLE_NAME;
    private static final String SIMPLE_EVENT_CAMPAIGN_TABLE_NAME = Constants.Z_DB_SIMPLE_EVENT_CAMPAIGNS_TABLE_NAME;
    private static final String SUPPRESSION_LOGIC_TABLE_NAME = Constants.Z_DB_SUPPRESSION_LOGIC_TABLE_NAME;
    private static final String INAPPTEXT_TABLE_NAME = Constants.Z_DB_INAPPTEXT_TABLE_NAME;

    private static final String ID_FIELD = Constants.Z_DB_EVENT_ID_FIELD_NAME;
    private static final String EVENT_FIELD = Constants.Z_DB_EVENT_EVENTS_FIELD_NAME;

    private static final String CREATE_EVENTS_TABLE = "CREATE TABLE IF NOT EXISTS "
            + EVENT_TABLE_NAME + " ("
            + Constants.Z_DB_EVENT_ID_FIELD_NAME + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + Constants.Z_DB_EVENT_EVENTS_FIELD_NAME + " TEXT);";

    private static final String CREATE_PROMOTIONS_TABLE = "CREATE TABLE IF NOT EXISTS "
            + PROMOTION_TABLE_NAME + " ("
            + Constants.Z_DB_PROMOTION_ID_FIELD_NAME + " INTEGER PRIMARY KEY AUTOINCREMENT, campaign_id TEXT, screen_id TEXT, status INTEGER DEFAULT 0,"
            + Constants.Z_DB_PROMOTION_PROMOTION_FIELD_NAME + " TEXT);";

    private static final String CREATE_USERPROPERTIES_TABLE = "CREATE TABLE IF NOT EXISTS "
            + Constants.Z_DB_USER_PROPERTIES_TABLE_NAME + " ("
            + Constants.Z_DB_USER_PROPERTIES_ID_FIELD_NAME + " INTEGER PRIMARY KEY AUTOINCREMENT, propname TEXT, propvalue TEXT, synched INTEGER DEFAULT 0 );";

    private static final String CREATE_GEOCAMPAIGNS_TABLE = "CREATE TABLE IF NOT EXISTS "
            +Constants.Z_DB_GEO_CAMPAIGNS_TABLE_NAME + " ("
            +Constants.Z_DB_GEO_CAMPAIGNS_ID_FIELD_NAME + " INTEGER PRIMARY KEY AUTOINCREMENT, campaign_id TEXT, "
            +Constants.Z_DB_GEO_CAMPAIGNS_PROMOTION_FIELD_NAME + " TEXT);";

    private static final String CREATE_SIMPLE_EVENT_CAMPAIGNS_TABLE = "CREATE TABLE IF NOT EXISTS "
            +Constants.Z_DB_SIMPLE_EVENT_CAMPAIGNS_TABLE_NAME + " ("
            +Constants.Z_DB_SIMPLE_EVENT_CAMPAIGNS_ID_FIELD_NAME + " INTEGER PRIMARY KEY AUTOINCREMENT, campaign_id TEXT,"
            +Constants.Z_DB_SIMPLE_EVENT_CAMPAIGNS_PROMOTION_FIELD_NAME + " TEXT);";

    private static final String CREATE_SUPPRESSION_LOGIC_TABLE_FOR_CAMPAIGNS = "CREATE TABLE IF NOT EXISTS "
            +Constants.Z_DB_SUPPRESSION_LOGIC_TABLE_NAME + " ("
            +Constants.Z_DB_SUPPRESSION_LOGIC_ID_FIELD_NAME + " INTEGER PRIMARY KEY AUTOINCREMENT, campaign_id TEXT, maximumNumberOfTimesToShow INTEGER, minimumDurationBeforeReshowInMin INTEGER, numberOfTimesShown INTEGER DEFAULT 0, lastShownTimeUnixTimeStamp INTEGER DEFAULT 0);";

    private static final String CREATE_UNIQUE_INDEX_ON_CAMPAIGN_ID_SIMPLE_EVENT_CAMPAIGNS = "CREATE UNIQUE INDEX simpleEventcampaign_id_idx" +
            " on "+ SIMPLE_EVENT_CAMPAIGN_TABLE_NAME+" (campaign_id);";

    private static final String CREATE_UNIQUE_INDEX_ON_CAMPAIGN_ID_GEOCAMPAIGNS = "CREATE UNIQUE INDEX geocampaign_id_idx" +
            " on "+ GEO_CAMPAIGN_TABLE_NAME+" (campaign_id);";

    private static final String CREATE_UNIQUE_INDEX_ON_CAMPAIGN_ID = "CREATE UNIQUE INDEX campaign_id_idx" +
            " on "+ PROMOTION_TABLE_NAME+" (campaign_id);";

    private static final String CREATE_UNIQUE_INDEX_ON_PROPNAME = "CREATE UNIQUE INDEX propname_idx" +
            " on "+ Constants.Z_DB_USER_PROPERTIES_TABLE_NAME+" (propname);";

    private static final String CREATE_SCREEN_FIX_TABLE = "CREATE TABLE IF NOT EXISTS "
            + SCREEN_FIX_TABLE_NAME + "(" + Constants.Z_DB_SCREEN_FIX_ID_FIELD_NAME + " INTEGER PRIMARY KEY AUTOINCREMENT, campaign_id TEXT, screen_id TEXT, "
            +Constants.Z_DB_SCREENFIX_PROMOTION_FIELD_NAME + " MEDIUMTEXT);";

    private static final String CREATE_UNIQUE_INDEX_ON_SCREEN_ID = "CREATE UNIQUE INDEX screen_idx" +
            " on "+ Constants.Z_DB_SCREEN_FIX_TABLE_NAME +" (screen_id);";

    private static final String CREATE_UNIQUE_INDEX_ON_CAMPAIGN_ID_SUPPRESSION = "CREATE UNIQUE INDEX suppression_campaign_idx" + " on "
            + Constants.Z_DB_SUPPRESSION_LOGIC_TABLE_NAME +" (campaign_id);";

    private static final String CREATE_UNIQUE_INDEX_ON_LOCALE = "CREATE UNIQUE INDEX locale_idx" +
            " on "+ INAPPTEXT_TABLE_NAME+" (locale);";

    private static final String CREATE_INAPPTEXT_TABLE = "CREATE TABLE IF NOT EXISTS "
            + INAPPTEXT_TABLE_NAME + " ("
            + "locale TEXT, texts TEXT, status INTEGER DEFAULT 0"
            + ");";

    private File file;

    static DbHelper getDatabaseHelper(Context context) {
        if (instance == null) {
            instance = new DbHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DbHelper(Context context) {
        super(context, Constants.Z_DB_NAME, null, Constants.Z_DB_VERSION);
        file = context.getDatabasePath(Constants.Z_DB_NAME);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // INTEGER PRIMARY KEY AUTOINCREMENT guarantees that all generated values
        // for the field will be monotonically increasing and unique over the
        // lifetime of the table, even if rows get removed
        db.execSQL(CREATE_EVENTS_TABLE);
        db.execSQL(CREATE_PROMOTIONS_TABLE);
        db.execSQL(CREATE_USERPROPERTIES_TABLE);
        db.execSQL(CREATE_SCREEN_FIX_TABLE);
        db.execSQL(CREATE_GEOCAMPAIGNS_TABLE);
        db.execSQL(CREATE_SIMPLE_EVENT_CAMPAIGNS_TABLE);
        db.execSQL(CREATE_SUPPRESSION_LOGIC_TABLE_FOR_CAMPAIGNS);
        db.execSQL(CREATE_INAPPTEXT_TABLE);
        db.execSQL(CREATE_UNIQUE_INDEX_ON_CAMPAIGN_ID);
        db.execSQL(CREATE_UNIQUE_INDEX_ON_PROPNAME);
        db.execSQL(CREATE_UNIQUE_INDEX_ON_SCREEN_ID);
        db.execSQL(CREATE_UNIQUE_INDEX_ON_CAMPAIGN_ID_GEOCAMPAIGNS);
        db.execSQL(CREATE_UNIQUE_INDEX_ON_CAMPAIGN_ID_SIMPLE_EVENT_CAMPAIGNS);
        db.execSQL(CREATE_UNIQUE_INDEX_ON_CAMPAIGN_ID_SUPPRESSION);
        db.execSQL(CREATE_UNIQUE_INDEX_ON_LOCALE);
        if(ZeTarget.isDebuggingOn()){
            Log.d(TAG, "Db created successfully");
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + EVENT_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + PROMOTION_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Constants.Z_DB_USER_PROPERTIES_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS "+ GEO_CAMPAIGN_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS "+ SIMPLE_EVENT_CAMPAIGN_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + SCREEN_FIX_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + SUPPRESSION_LOGIC_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + INAPPTEXT_TABLE_NAME);
        db.execSQL(CREATE_EVENTS_TABLE);
        db.execSQL(CREATE_PROMOTIONS_TABLE);
        db.execSQL(CREATE_USERPROPERTIES_TABLE);
        db.execSQL(CREATE_SCREEN_FIX_TABLE);
        db.execSQL(CREATE_GEOCAMPAIGNS_TABLE);
        db.execSQL(CREATE_SIMPLE_EVENT_CAMPAIGNS_TABLE);
        db.execSQL(CREATE_SUPPRESSION_LOGIC_TABLE_FOR_CAMPAIGNS);
        db.execSQL(CREATE_INAPPTEXT_TABLE);
        db.execSQL(CREATE_UNIQUE_INDEX_ON_CAMPAIGN_ID);
        db.execSQL(CREATE_UNIQUE_INDEX_ON_PROPNAME);
        db.execSQL(CREATE_UNIQUE_INDEX_ON_SCREEN_ID);
        db.execSQL(CREATE_UNIQUE_INDEX_ON_CAMPAIGN_ID_GEOCAMPAIGNS);
        db.execSQL(CREATE_UNIQUE_INDEX_ON_CAMPAIGN_ID_SIMPLE_EVENT_CAMPAIGNS);
        db.execSQL(CREATE_UNIQUE_INDEX_ON_CAMPAIGN_ID_SUPPRESSION);
        db.execSQL(CREATE_UNIQUE_INDEX_ON_LOCALE);
        if(ZeTarget.isDebuggingOn()){
            Log.d(TAG, "Db updated successfully");
        }
    }


    //Events related
    synchronized long addEvent(String event) {
        long result = -1;
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put(EVENT_FIELD, event);
            result = db.insert(EVENT_TABLE_NAME, null, contentValues);
            if (result == -1) {
                if(ZeTarget.isDebuggingOn()){
                    Log.e(TAG, "Event Insert failed");
                }
            }
        } catch (SQLiteException e) {
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "SQLiteException", e);
            }
            // Not much we can do, just start fresh
            delete();
        } finally {
            close();
        }
        return result;
    }

    synchronized Pair<Long, JSONArray> getEvents(long lessThanId, int limit) throws JSONException {
        long maxId = -1;
        JSONArray events = new JSONArray();
        Cursor cursor = null;
        try {
            SQLiteDatabase db = getReadableDatabase();
            cursor = db.query(EVENT_TABLE_NAME, new String[]{ID_FIELD, EVENT_FIELD},
                    lessThanId >= 0 ? ID_FIELD + " < " + lessThanId : null, null, null,
                    null, ID_FIELD + " ASC", limit >= 0 ? "" + limit : null);

            while (cursor.moveToNext()) {
                long eventId = cursor.getLong(0);
                String event = cursor.getString(1);

                JSONObject obj = new JSONObject(event);
                obj.put("event_id", eventId);
                events.put(obj);

                maxId = eventId;
            }
        } catch (SQLiteException e) {
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "getEvents failed", e);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            close();
        }
        return new Pair<Long, JSONArray>(maxId, events);
    }

    synchronized long getEventCount() {
        long numberRows = 0;
        try {
            SQLiteDatabase db = getReadableDatabase();
            String query = "SELECT COUNT(*) FROM " + EVENT_TABLE_NAME;
            SQLiteStatement statement = db.compileStatement(query);
            numberRows = statement.simpleQueryForLong();
        } catch (SQLiteException e) {
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "getEventCount failed", e);
            }
        } finally {
            close();
        }
        return numberRows;
    }

    synchronized long getNthEventId(long n) {
        long nthEventId = -1;
        try {
            SQLiteDatabase db = getReadableDatabase();
            String query = "SELECT " + ID_FIELD + " FROM " + EVENT_TABLE_NAME
                    + " LIMIT 1 OFFSET " + (n - 1);
            SQLiteStatement statement = db.compileStatement(query);
            nthEventId = -1;
            try {
                nthEventId = statement.simpleQueryForLong();
            } catch (SQLiteDoneException e) {
                if(ZeTarget.isDebuggingOn()){
                    Log.e(TAG,"SqliteDoneException", e);
                }
            }
        } catch (SQLiteException e) {
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "getNthEventId failed", e);
            }
        } finally {
            close();
        }
        return nthEventId;
    }

    synchronized void removeEvents(long maxId) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            db.delete(EVENT_TABLE_NAME, ID_FIELD + " <= " + maxId, null);
        } catch (SQLiteException e) {
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "removeEvents failed", e);
            }
        } finally {
            close();
        }
    }

    synchronized void removeEvent(long id) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            db.delete(EVENT_TABLE_NAME, ID_FIELD + " = " + id, null);
        } catch (SQLiteException e) {
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "removeEvent failed", e);
            }
        } finally {
            close();
        }
    }

    //Promotions related
    synchronized long addPromotion(String promotion, String campaign_id, String screen_id,int maximumNumberOfTimesToShow,int minimumDurationBeforeReshowInMin) {
        long result = -1;
        long result2 = -1;
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put(Constants.Z_DB_PROMOTION_PROMOTION_FIELD_NAME, promotion);
            contentValues.put("campaign_id", campaign_id);
            contentValues.put("screen_id", screen_id);
            //Log.i(TAG,"Promotion for screenId: "+screen_id+" added");
            ContentValues contentValues2 = new ContentValues();
            contentValues2.put("campaign_id", campaign_id);
            contentValues2.put("maximumNumberOfTimesToShow", maximumNumberOfTimesToShow);
            contentValues2.put("minimumDurationBeforeReshowInMin", minimumDurationBeforeReshowInMin);

            result = db.insertWithOnConflict(PROMOTION_TABLE_NAME, null, contentValues,5);
            result2 = db.insertWithOnConflict(SUPPRESSION_LOGIC_TABLE_NAME,null,contentValues2,5);
            if (result == -1 || result2== -1) {
                if(ZeTarget.isDebuggingOn()){
                    Log.e(TAG, "Insert failed in add promotions");
                }
            }

        } catch (SQLiteException e) {
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "Insert failed in add promotions", e);
            }
            // Not much we can do, just start fresh
            delete();
        } finally {
            close();
        }
        return result;
    }

    synchronized void removeSeenPromotions() {
        try {
            SQLiteDatabase db = getWritableDatabase();
            int count = db.delete(PROMOTION_TABLE_NAME, "status = 1", null);
        } catch (SQLiteException e) {
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "removePromotion failed", e);
            }
        } finally {
            close();
        }
    }

    synchronized void markPromotionAsSeen(String campaign_id) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put("status", 1);
            int count = db.update(PROMOTION_TABLE_NAME, contentValues, "campaign_id = ?", new String[]{campaign_id});
            //Log.i(TAG,"Updating Promotions: "+count+" "+campaign_id);
        } catch (SQLiteException e) {
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "markPromotionAsSeen failed", e);
            }
        } finally {
            close();
        }
    }

    synchronized JSONObject getPromotionforScreen(String screen_id){
        JSONObject promotion = new JSONObject();
        Cursor cursor = null;
        try {
            SQLiteDatabase db = getReadableDatabase();
            cursor = db.query(PROMOTION_TABLE_NAME, null, "screen_id = ? AND status = ?", new String[]{screen_id, "0"}, null,
                    null, Constants.Z_DB_PROMOTION_ID_FIELD_NAME + " DESC", null);
            //Log.i(TAG,"Promotion count: "+cursor.getCount());
            for(int i=0;i<cursor.getCount();i++) {
                cursor.moveToNext();
                String campaignId = cursor.getString(1);
                if(checkCampaignValidity(campaignId,System.currentTimeMillis(),true)){
                    String p = cursor.getString(4);
                    //Log.i(TAG,"Length of Promotion fetched is"+promotion.length());
                    promotion = new JSONObject(p);
                    break;
                }else{
                    //Log.i(TAG, "Promotion Campaign InValid for now " + campaignId);
                }
            }
        } catch (SQLiteException e) {
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "getPromotionforScreen failed", e);
            }
        } catch (Exception e){
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, " getPromotionforScreen falied", e);
            }
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
            //Log.i(TAG,"Closing DB from getPromotionForScreen()");
            close();
        }
        return promotion;
    }

    //User Properties related

    synchronized long addUserProperties(JSONObject userProperties) {
        long result = -1;
        try {
            SQLiteDatabase db = getWritableDatabase();

            for(int i=0; i < userProperties.length(); i++){
                ContentValues contentValues = new ContentValues();
                contentValues.put("propname", userProperties.names().getString(i));
                contentValues.put("propvalue", userProperties.getString(userProperties.names().getString(i)));
                result = db.insertWithOnConflict(Constants.Z_DB_USER_PROPERTIES_TABLE_NAME, null, contentValues,5);
                if (result == -1) {
                    if(ZeTarget.isDebuggingOn()){
                        Log.e(TAG, "UserProperty Insert failed");
                    }
                }
            }
        } catch (SQLiteException e) {
            if(ZeTarget.isDebuggingOn()) {
                Log.e(TAG, "adding UserProperty failed", e);
            }
            // Not much we can do, just start fresh
            delete();
        } catch(Exception e){
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG,"add user property"+e);
            }
        } finally {
            close();
        }
        return result;
    }

    synchronized String getUserProperty(String propname){
        String value = null;
        Cursor cursor = null;
        try {
            SQLiteDatabase db = getReadableDatabase();
            cursor = db.query(Constants.Z_DB_USER_PROPERTIES_TABLE_NAME, null,"propname = ?", new String[]{propname}, null,
                    null, Constants.Z_DB_USER_PROPERTIES_ID_FIELD_NAME + " DESC", "1");

            while (cursor.moveToNext()) {
                value = cursor.getString(2);
            }
        } catch (SQLiteException e) {
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "getUserProperty failed", e);
            }
        } catch (Exception e){
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, " getUserProperty falied", e);
            }
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
            close();
        }
        return value;
    }

    synchronized JSONObject getUserProperties(){
        JSONObject userProperties = new JSONObject();
        Cursor cursor = null;
        try {
            SQLiteDatabase db = getReadableDatabase();
            cursor = db.query(Constants.Z_DB_USER_PROPERTIES_TABLE_NAME, null,null, null, null,
                    null, Constants.Z_DB_USER_PROPERTIES_ID_FIELD_NAME + " DESC", null);

            while (cursor.moveToNext()) {
                String key = cursor.getString(1);
                String value = cursor.getString(2);
                userProperties.put(key,value);
            }
        } catch (SQLiteException e) {
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "getUserProperties failed", e);
            }
        } catch (Exception e){
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, " getUserProperties falied", e);
            }
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
            close();
        }
        return userProperties;
    }

    private void delete() {
        try {
            close();
            file.delete();
        } catch (SecurityException e) {
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "delete failed", e);
            }
        }
    }

    synchronized long addScreenFix(String change, String campaign_id, String screen_id) {
        long result = -1;
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put(Constants.Z_DB_SCREENFIX_PROMOTION_FIELD_NAME, change);
            contentValues.put("campaign_id", campaign_id);
            contentValues.put("screen_id", screen_id);

            result = db.insertWithOnConflict(SCREEN_FIX_TABLE_NAME, null, contentValues, 5);
            if (result == -1) {
                if(ZeTarget.isDebuggingOn()){
                    Log.e(TAG, "Insert failed in addScreenFix");
                }
            }

        } catch (SQLiteException e) {
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "adding screenfix failed", e);
            }
            // Not much we can do, just start fresh
            delete();
        } finally {
            close();
        }
        return result;
    }

    synchronized JSONObject getScreenFixesFor(String screen_id){
        JSONObject promotion = new JSONObject();
        Cursor cursor = null;
        try {
            SQLiteDatabase db = getReadableDatabase();
             cursor = db.query(SCREEN_FIX_TABLE_NAME, null, "screen_id = ?", new String[]{screen_id}, null,
                     null, Constants.Z_DB_SCREEN_FIX_ID_FIELD_NAME + " DESC", "1");

            while (cursor.moveToNext()) {
                String p = cursor.getString(3);

                promotion = new JSONObject(p);
            }
        } catch (SQLiteException e) {
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "getScreenFixesFor() failed", e);
            }
        } catch (Exception e){
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, " getScreenFixesFor() falied", e);
            }
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
            close();
        }
        return promotion;
    }

    synchronized boolean addGeoCampaign(String promotion, String campaign_id, int maximumNumberOfTimesToShow,int minimumDurationBeforeReshowInMin) {
        boolean success = false;
        long result1 = -1,result2 = -1;
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues contentValues1 = new ContentValues();
            contentValues1.put(Constants.Z_DB_GEO_CAMPAIGNS_PROMOTION_FIELD_NAME, promotion);
            contentValues1.put("campaign_id", campaign_id);
            ContentValues contentValues2 = new ContentValues();
            contentValues2.put("campaign_id", campaign_id);
            contentValues2.put("maximumNumberOfTimesToShow", maximumNumberOfTimesToShow);
            contentValues2.put("minimumDurationBeforeReshowInMin", minimumDurationBeforeReshowInMin);

            result1 = db.insertWithOnConflict(GEO_CAMPAIGN_TABLE_NAME, null, contentValues1,5);
            result2 = db.insertWithOnConflict(SUPPRESSION_LOGIC_TABLE_NAME, null, contentValues2, 5);
            if (result1 == -1||result2 == -1) {
                success = false;
                if(ZeTarget.isDebuggingOn()){
                    Log.e(TAG, "Insert failed in addGeoCampaign");
                }
            }else{
                success =true;
            }
        } catch (SQLiteException e) {
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "addGeoCampaign failed:"+result1 + "," + result2);
            }
            // Not much we can do, just start fresh
            delete();
        } finally {
            close();
        }
        return success;
    }

    synchronized JSONArray getGeoFenceCampaigns(){
        JSONArray promotion = new JSONArray();
        Cursor cursor = null;
        try {
            SQLiteDatabase db = getReadableDatabase();
            String[] columnArray = new String[]{Constants.Z_DB_GEO_CAMPAIGNS_ID_FIELD_NAME,Constants.Z_DB_GEO_CAMPAIGNS_PROMOTION_FIELD_NAME};
            cursor = db.query(GEO_CAMPAIGN_TABLE_NAME,columnArray,null, null, null,
                    null, Constants.Z_DB_GEO_CAMPAIGNS_ID_FIELD_NAME + " DESC");

            while (cursor.moveToNext()) {
                String rowId = cursor.getString(0);
                String p = cursor.getString(1);

                promotion.put(new JSONObject(p).put("rowIdInTable",rowId));
            }
        } catch (SQLiteException e) {
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "getGeoFenceCampaigns() failed", e);
            }
        } catch (Exception e){
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, " getGeoFenceCampaigns() failed", e);
            }
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
            close();
        }
        return promotion;
    }

    synchronized JSONArray getSimpleEventCampaigns(){
        JSONArray promotion = new JSONArray();
        Cursor cursor = null;
        try {
            SQLiteDatabase db = getReadableDatabase();
            String[] columnArray = new String[]{Constants.Z_DB_SIMPLE_EVENT_CAMPAIGNS_ID_FIELD_NAME,Constants.Z_DB_SIMPLE_EVENT_CAMPAIGNS_PROMOTION_FIELD_NAME};
            cursor = db.query(SIMPLE_EVENT_CAMPAIGN_TABLE_NAME,columnArray,null, null, null,
                    null, Constants.Z_DB_SIMPLE_EVENT_CAMPAIGNS_ID_FIELD_NAME + " DESC");

            while (cursor.moveToNext()) {
                String rowId = cursor.getString(0);
                String p = cursor.getString(1);
                promotion.put(new JSONObject(p).put("rowIdInTable",rowId));
            }
        } catch (SQLiteException e) {
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "getSimpleEventCampaigns() failed", e);
            }
        } catch (Exception e){
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, " getSimpleEventCampaigns() failed", e);
            }
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
            close();
        }
        return promotion;
    }

    synchronized boolean addSimpleEventCampaign(String promotion, String campaign_id, int maximumNumberOfTimesToShow,int minimumDurationBeforeReshowInMin) {
        long result1 = -1,result2 = -1;
        boolean success = false;
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues contentValues1 = new ContentValues();
            contentValues1.put(Constants.Z_DB_SIMPLE_EVENT_CAMPAIGNS_PROMOTION_FIELD_NAME, promotion);
            contentValues1.put("campaign_id", campaign_id);

            ContentValues contentValues2 = new ContentValues();
            contentValues2.put("campaign_id", campaign_id);
            contentValues2.put("minimumDurationBeforeReshowInMin", minimumDurationBeforeReshowInMin);
            contentValues2.put("maximumNumberOfTimesToShow", maximumNumberOfTimesToShow);

            result1 = db.insertWithOnConflict(SIMPLE_EVENT_CAMPAIGN_TABLE_NAME, null, contentValues1,5);
            result2 = db.insertWithOnConflict(SUPPRESSION_LOGIC_TABLE_NAME,null,contentValues2,5);
            if (result1 == -1||result2 == -1) {
                success = false;
                if(ZeTarget.isDebuggingOn()){
                    Log.e(TAG, "Insert failed in addSimpleEventCampaign");
                }
            }else{
                success =true;
            }

        } catch (SQLiteException e) {
            if(ZeTarget.isDebuggingOn()) {
                Log.e(TAG, "add Simple Event failed", e);
            }
            delete();
        } finally {
            close();
        }
        //Log.i("DB SimpleEvent", result1+","+result2);
        return success;

    }

    synchronized boolean checkCampaignValidity(String campaignId, long timeStampOfOccurence){
       return checkCampaignValidity(campaignId,timeStampOfOccurence,false);
    }

    synchronized boolean checkCampaignValidity(String campaignId,long timeStampOfOccurence, boolean internalCall){
        Cursor cursor = null;
        boolean valid = false;
        try {
            SQLiteDatabase db = getReadableDatabase();
            String[] selectArgs = {campaignId};
            //String[] columnArray = new String[]{Constants.Z_DB_SUPPRESSION_LOGIC_CAMPAIGN_ID_FIELD_NAME, "lastShownTimeUnixTimeStamp"};
            cursor = db.query(SUPPRESSION_LOGIC_TABLE_NAME,null,"campaign_id = ?", selectArgs, null,
                    null, Constants.Z_DB_SUPPRESSION_LOGIC_CAMPAIGN_ID_FIELD_NAME + " DESC");

            cursor.moveToNext();
            long lastShownTime = cursor.getLong(5);
            long minimumDurationBeforeReshow = cursor.getLong(3);
            int numberOfTimesShown = cursor.getInt(4);
            int maxNumberOfTimesToShow = cursor.getInt(2);
            if(timeStampOfOccurence>=lastShownTime+minimumDurationBeforeReshow*60*1000 &&
                    maxNumberOfTimesToShow>numberOfTimesShown){
                valid = true;
            }
        } catch (SQLiteException e) {
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "updating simpleEventCampaign failed: "+campaignId, e);
            }
        } catch (Exception e){
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "updating simpleEventCampaign failed not on SQLiteException: "+campaignId, e);
            }
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
            if(!internalCall) {
                close();
            }
        }
        return valid;
    }

    synchronized void updateCampaign(String campaignId, long timeStampOfOccurence) {
        Cursor cursor = null;
        try {
            SQLiteDatabase db = getReadableDatabase();
            String[] selectArgs = {campaignId};
            String[] columnArray = new String[]{Constants.Z_DB_SUPPRESSION_LOGIC_CAMPAIGN_ID_FIELD_NAME, "numberOfTimesShown"};
            cursor = db.query(SUPPRESSION_LOGIC_TABLE_NAME,columnArray,"campaign_id = ?", selectArgs, null,
                    null, Constants.Z_DB_SUPPRESSION_LOGIC_CAMPAIGN_ID_FIELD_NAME + " DESC");

            cursor.moveToNext();
            int numberOfTimesShown = cursor.getInt(1);
            numberOfTimesShown++;
            ContentValues contentValues = new ContentValues();
            contentValues.put("numberOfTimesShown", numberOfTimesShown);
            contentValues.put("lastShownTimeUnixTimeStamp", timeStampOfOccurence);
            db.update(SUPPRESSION_LOGIC_TABLE_NAME, contentValues, "campaign_id = ?", selectArgs);
            //Log.i(TAG, "updated suppression Logic Table");
        } catch (SQLiteException e) {
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "updating suppressionLogic table failed: "+campaignId, e);
            }
        } catch (Exception e){
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "updating suppressionLogic table failed not on SQLiteException: "+campaignId, e);
            }
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
            close();
        }
    }



    synchronized void removeSimpleEventCampaign(String campaignId) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            String[] whereArgs = {campaignId};
            db.delete(Constants.Z_DB_SIMPLE_EVENT_CAMPAIGNS_TABLE_NAME, "campaign_id = ?", whereArgs);
            db.delete(Constants.Z_DB_SUPPRESSION_LOGIC_TABLE_NAME, "campaign_id = ?", whereArgs);
        }catch(SQLiteException e){
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG,"failure in removing simple event campaign: "+campaignId,e);
            }
        } catch (Exception e){
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "removing simpleEventCampaign failed: "+campaignId, e);
            }
        }
    }

    synchronized void removeGeoCampaign(String campaignId) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            String[] whereArgs = {campaignId};
            db.delete(Constants.Z_DB_GEO_CAMPAIGNS_TABLE_NAME, "campaign_id = ?", whereArgs);
            db.delete(Constants.Z_DB_SUPPRESSION_LOGIC_TABLE_NAME, "campaign_id = ?", whereArgs);
        }catch(SQLiteException e){
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG,"failure in removing geo campaign: "+campaignId,e);
            }
        } catch (Exception e){
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "removing geo campaign failed: "+campaignId, e);
            }
        }
    }

    synchronized long getLastShownTime(String campaignId) {
        Cursor cursor = null;
        long lastShownTime = 0;
        try {
            SQLiteDatabase db = getReadableDatabase();
            String[] selectArgs = {campaignId};
            String[] columnArray = new String[]{Constants.Z_DB_SUPPRESSION_LOGIC_CAMPAIGN_ID_FIELD_NAME, "lastShownTimeUnixTimeStamp"};
            cursor = db.query(SUPPRESSION_LOGIC_TABLE_NAME,columnArray,"campaign_id = ?", selectArgs, null,
                    null, Constants.Z_DB_SUPPRESSION_LOGIC_CAMPAIGN_ID_FIELD_NAME + " DESC");

            cursor.moveToNext();
            lastShownTime = cursor.getLong(1);
        } catch (SQLiteException e) {
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "getLastShownTime failed: "+campaignId, e);
            }
        } catch (Exception e){
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "getLastShownTime failed not on SQLiteException: "+campaignId, e);
            }
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
            close();
        }
        return lastShownTime;
    }

    synchronized int getNumberOfTimesShown(String campaignId) {
        Cursor cursor = null;
        int numberOfTimesShown = -1;
        try {
            SQLiteDatabase db = getReadableDatabase();
            String[] selectArgs = {campaignId};
            String[] columnArray = new String[]{Constants.Z_DB_SUPPRESSION_LOGIC_CAMPAIGN_ID_FIELD_NAME, "numberOfTimesShown"};
            cursor = db.query(SUPPRESSION_LOGIC_TABLE_NAME,columnArray,"campaign_id = ?", selectArgs, null,
                    null, Constants.Z_DB_SUPPRESSION_LOGIC_CAMPAIGN_ID_FIELD_NAME + " DESC");

            cursor.moveToNext();
            numberOfTimesShown= cursor.getInt(1);

        } catch (SQLiteException e) {
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "getNumberOfTimesShown failed: "+campaignId, e);
            }
        } catch (Exception e){
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "getNumberOfTimesShown failed not on SQLiteException: "+campaignId, e);
            }
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
            close();
        }
        return numberOfTimesShown;
    }

    //In App Text related
    //texts is JSON array to string text
    synchronized long addInAppText(String locale, String texts) {
        long result = -1;
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put("locale", locale);
            contentValues.put("texts", texts);
            result = db.insertWithOnConflict(INAPPTEXT_TABLE_NAME, null, contentValues, 5);
            if (result == -1) {
                if(ZeTarget.isDebuggingOn()){
                    Log.e(TAG, "Insert failed in addInAppText");
                }
            }

        } catch (SQLiteException e) {
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "Insert failed in addInAppText", e);
            }
            // Not much we can do, just start fresh
            delete();
        } finally {
            close();
        }
        return result;
    }

    synchronized JSONArray getInAppTexts(String locale){
        JSONArray texts = null;
        Cursor cursor = null;
        try {
            SQLiteDatabase db = getReadableDatabase();
            cursor = db.query(INAPPTEXT_TABLE_NAME, null, "locale = ?", new String[]{locale}, null,
                    null, null, "1");
            for(int i=0;i<cursor.getCount();i++) {
                cursor.moveToNext();
                String changedText = cursor.getString(1);
                texts = new JSONArray(changedText);
            }
        } catch (SQLiteException e) {
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "getInAppTexts failed", e);
            }
        } catch (Exception e){
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "getInAppTexts falied", e);
            }
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
            //Log.i(TAG,"Closing DB from getInAppTexts()");
            close();
        }
        return texts;
    }

    synchronized void clearInAppTextForLocale(String locale) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            db.delete(INAPPTEXT_TABLE_NAME, "locale = ?", new String[]{locale});
        } catch (SQLiteException e) {
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "clearInAppText failed", e);
            }
        } finally {
            close();
        }
    }
}