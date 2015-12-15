package com.zemosolabs.zetarget.sdk;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;

/**
 * Created by praveen on 21/01/15.
 */
public class DataStore extends UserProperties{
    private static DataStore dataStore;

    private DataStore(){

    }

    public synchronized static DataStore getDataStore(){
        if(dataStore == null){
            dataStore = new DataStore();
        }
        return dataStore;
    }

    static String getData(Context context,String key, String defaultValue){
        return getSharedPreferences(context).getString(key, defaultValue);
    }

    static void setData(Context context,String key, String value){
        getSharedPreferences(context).edit().putString(key, value).apply();
    }

    static void setData(Context context,Map<String,String> values){
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        for(Map.Entry<String, String> e : values.entrySet()) {
            String key = e.getKey();
            String value = e.getValue();
            editor.putString(key,value);
        }
        editor.apply();
    }

    static void setLastSyncTime(Context context,String value){
        getSharedPreferences(context).edit().putString(Constants.Z_PREFKEY_LAST_DATASTORE_SYNC_TIME, value).apply();
    }

    static String getLastSyncTime(Context context){
        return getSharedPreferences(context).getString(Constants.Z_PREFKEY_LAST_DATASTORE_SYNC_TIME, null);
    }

    static void setDataStoreVersion(Context context,String value){
        getSharedPreferences(context).edit().putString(Constants.Z_PREFKEY_LAST_DATASTORE_VERSION, value).apply();
    }

    static String getDataStoreVersion(Context context){
        return getSharedPreferences(context).getString(Constants.Z_PREFKEY_LAST_DATASTORE_VERSION, null);
    }

    static Map<String,?> getAllData(Context context){
        return getSharedPreferences(context).getAll();
    }

    static SharedPreferences getSharedPreferences(Context context){
        return context.getSharedPreferences(
                Constants.Z_SHARED_PREFERENCE_DATASTORE_FILE_NAME, Context.MODE_PRIVATE);
    }
}
