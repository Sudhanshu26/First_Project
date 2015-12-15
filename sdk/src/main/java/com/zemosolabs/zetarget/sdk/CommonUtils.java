package com.zemosolabs.zetarget.sdk;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by praveen on 21/01/15.
 */
public class CommonUtils {

    static Object replaceWithJSONNull(Object obj) {
        return obj == null ? JSONObject.NULL : obj;
    }

    static String bytesToHexString(byte[] bytes) {
        final char[] hexArray = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c',
                'd', 'e', 'f' };
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    static SharedPreferences getSharedPreferences(Context context){
        return context.getSharedPreferences(
                Constants.Z_SHARED_PREFERENCE_FILE_NAME, Context.MODE_PRIVATE);
    }

    /*static String getCurrentDateTime(){
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.Z_DATE_TIME_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        return sdf.format(new Date(System.currentTimeMillis()));
    }*/

    static String getCurrentDateTime(long timestamp){
        return new SimpleDateFormat(Constants.Z_DATE_TIME_FORMAT).format(new Date(timestamp));
    }
}
