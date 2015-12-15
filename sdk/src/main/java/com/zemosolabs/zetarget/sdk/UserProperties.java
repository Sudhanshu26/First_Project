package com.zemosolabs.zetarget.sdk;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

/**
 * Created by praveen on 21/01/15.
 */
public class UserProperties {

    private static final String TAG = "ZeTarget.UserProperties";

    protected static String getUserProperty(Context context,String key, String defaultValue){
        DbHelper dbHelper = DbHelper.getDatabaseHelper(context);
        String value = dbHelper.getUserProperty(key);
        if(value == null){
            return defaultValue;
        }
        return value;
    }

    protected static void setUserProperty(Context context,String key, String value){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(key,value);
            setUserProperties(context,jsonObject);
        }
        catch (Exception e){
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG,"Exception: "+e);
            }
        }
    }

    protected static void setUserProperties(Context context,JSONObject userProperties){
        DbHelper dbHelper = DbHelper.getDatabaseHelper(context);
        dbHelper.addUserProperties(userProperties);
    }

    protected static JSONObject getAllUserProperties(Context context){
        DbHelper dbHelper = DbHelper.getDatabaseHelper(context);
        return dbHelper.getUserProperties();
    }
}
