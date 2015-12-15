package com.zemosolabs.zetarget.sdk;

import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by praveen on 21/01/15.
 */
public class HttpHelper {

    private static HttpHelper httpHelper;

    private static final String TAG = "ZeTarget.HttpHelper";


    private HttpHelper(){

    }

    synchronized static HttpHelper getHttpHelper(){
        if(httpHelper == null){
            httpHelper = new HttpHelper();
        }
        return httpHelper;
    }

    static String doPost(String url, JSONObject postParams){

        postParams = addRequiredParams(postParams);

        HttpResponse response;
        String stringResponse = null;
        HttpPost postRequest = new HttpPost(Constants.Z_BASE_URL+url);
        HttpClient client = new DefaultHttpClient();
        try {
            String jsonString = postParams.toString();
            postRequest.setHeader("Accept", "application/json");
            postRequest.setHeader("Content-type", "application/json");
            postRequest.setEntity(new StringEntity(jsonString));
            response = client.execute(postRequest);
            stringResponse = EntityUtils.toString(response.getEntity());
        } catch (org.apache.http.conn.HttpHostConnectException e) {
             Log.w(TAG, "No internet connection found, unable to upload events");
        } catch (java.net.UnknownHostException e) {
             Log.w(TAG, "No internet connection found, unable to upload events");
        } catch (ClientProtocolException e) {
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, e.toString());
            }
        } catch (IOException e) {
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, e.toString());
            }
        } catch (AssertionError e) {
            // This can be caused by a NoSuchAlgorithmException thrown by DefaultHttpClient
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "Exception:", e);
            }
        } catch (Exception e) {
            // Just log any other exception so things don't crash on upload
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG, "Exception:", e);
            }
        } finally {
            if (client.getConnectionManager() != null) {
                client.getConnectionManager().shutdown();
            }
        }
        if(ZeTarget.isDebuggingOn()){
            //Log.d(TAG, "Post Response is: " + stringResponse);
        }
        return stringResponse;
    }

    private static JSONObject addRequiredParams(JSONObject postParams){
        try {
            postParams.put("apiKey",CommonUtils.replaceWithJSONNull(ZeTarget.getApiKey()));
            postParams.put("userId",CommonUtils.replaceWithJSONNull(ZeTarget.getUserId()));
            postParams.put("locale",CommonUtils.replaceWithJSONNull(DeviceDetails.getLocaleString()));
            postParams.put("deviceId", CommonUtils.replaceWithJSONNull(ZeTarget.getDeviceId()));
            postParams.put("sdkId", CommonUtils.replaceWithJSONNull(Constants.Z_SDK_ID));
            postParams.put("deviceModel", CommonUtils.replaceWithJSONNull(DeviceDetails.getModel()));
            postParams.put("appVersion",CommonUtils.replaceWithJSONNull(ZeTarget.deviceDetails.getVersionName()));
            return postParams;
        }
        catch (Exception e){
            if(ZeTarget.isDebuggingOn()){
                Log.e(TAG,"Exception: "+e);
            }
        }
        return postParams;
    }
}
