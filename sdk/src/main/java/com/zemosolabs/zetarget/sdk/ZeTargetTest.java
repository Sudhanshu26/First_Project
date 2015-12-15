package com.zemosolabs.zetarget.sdk;

/**
 * Created by praveen on 28/01/15.
 */
import android.app.Application;
import android.content.Context;
import android.test.ApplicationTestCase;
import android.util.Log;
import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */

public class ZeTargetTest extends ApplicationTestCase<Application> {
    public ZeTargetTest() {
        super(Application.class);
    }

    private static final String TAG = "ZeTarget.ZeTargetTest";

    private static final String promotionsForScreensJSON = "[{\"campaignId\": \"campaign1\",\"screenId\": \"SampleApp\",\"template\": {\"title\": \"Season sale! Heavy discounts!!!\", \"message\": \"Buy the stuff now and save a loot of money and use that money to buy some more stuff!!!\", \"imageUrl\": \"http://news.bbcimg.co.uk/media/images/81539000/jpg/_81539447_95ca831d-7a1d-4b02-b3ca-0c9968649937.jpg\",\"onClickUrl\": \"http://www.google.com\",\"templateType\": \"REGULAR\",\"definition\": { \"actionType\": \"LINK\", \"actionButton\": {\"url\": \"http://www.flipkart.com\",\"buttonText\": \"GO\"},\"dismissButtonText\": \"NO THANKS\"}}},{\"campaignId\": \"campaign2\",\"screenId\": \"Activity2\",\"template\": {\"title\": \"Season sale! Heavy discounts!!!\",\"message\": \"Buy the stuff now and save a loot of money and use that money to buy some more stuff!!! JUST RATE OUR APP\", \"imageUrl\": \"http://news.bbcimg.co.uk/media/images/81539000/jpg/_81539447_95ca831d-7a1d-4b02-b3ca-0c9968649937.jpg\", \"onClickUrl\": \"http://www.google.com\", \"templateType\": \"REGULAR\", \"definition\": {\"actionType\": \"LINK\", \"actionButton\": {\"buttonText\": \"RATE US\"},\"dismissButtonText\": \"DON'T ASK ME AGAIN\",\"remindLaterButtonText: \"REMIND ME LATER\"}}},\"campaignId\": \"campaign3\",\"screenId\": \"Activity3\",\"template\": {\"title\": \"Season sale! Heavy discounts!!!\",\"message\": \"Invite your friends now and save a loot of money and use that money to buy some more stuff!!!\",\"imageUrl\": \"http://news.bbcimg.co.uk/media/images/81539000/jpg/_81539447_95ca831d-7a1d-4b02-b3ca-0c9968649937.jpg\",\"onClickUrl\": \"http://www.google.com\",\"templateType\": \"REGULAR\",\"definition\": {\"actionType\": \"SHARE\",\"actionButton\": {\"shareText\": \"This app is awesome!!! Check it out! Use promocode:sdeidk\",\"buttonText\": \"INVITE\"},\"dismissButtonText\": \"NO THANKS\"}}},{\"campaignId\": \"campaign4\",\"screenId\": \"Activity4\",\"template\": {\"title\": \"Season sale! Heavy discounts!!!\",\"message\": \"Buy the stuff now and save a loot of money and use that money to buy some more stuff!!!\",\"imageUrl\": \"http://news.bbcimg.co.uk/media/images/81539000/jpg/_81539447_95ca831d-7a1d-4b02-b3ca-0c9968649937.jpg\",\"onClickUrl\": \"http://www.google.com\",\"templateType\": \"REGULAR\",\"definition\": {\"actionType\":\"NONE\",\"dismissButtonText\": \"GOT IT\"}}}]";

    private DbHelper dbHelper;

    public void setUp(){
        createApplication();
        getContext().getSharedPreferences(Constants.Z_SHARED_PREFERENCE_DATASTORE_FILE_NAME,Context.MODE_PRIVATE).edit().clear().commit();

        getContext().getSharedPreferences(Constants.Z_SHARED_PREFERENCE_FILE_NAME,Context.MODE_PRIVATE).edit().clear().commit();
        dbHelper = DbHelper.getDatabaseHelper(getContext());
        ZeTarget.initializeWithContextAndKey(getContext(), "TestAndroidAPIKey");
        ZeTarget.enableDebugging();
    }

    public void tearDown(){
        getContext().deleteDatabase(Constants.Z_DB_NAME);
        terminateApplication();
    }

    public void testDefaultUserIdisSet(){
        assertNotNull(ZeTarget.getUserId());
    }

    public void testCustomUserId(){
        String customUserId = "CustomUserId";
        ZeTarget.setUserId(customUserId);
        assertEquals(customUserId, ZeTarget.getUserId());
    }

    public void testUserProperty(){
        String value = "John";
        String key = "firstname";
        assertEquals("default", ZeTarget.getUserProperty(key, "default"));
        ZeTarget.setUserProperty("firstname", value);
        assertEquals(value, ZeTarget.getUserProperty(key, "default"));
    }

    public void testLogEvent(){
        String eventName = "click";
        ZeTarget.logEvent(eventName);

        try {
            Thread.sleep(1000);
            Pair<Long,JSONArray> p = dbHelper.getEvents(-1,-1);
            boolean clickEventFound = false;
            for(int i=0; i < p.second.length();i++){
                JSONObject json = p.second.getJSONObject(i);
                if(json.getString("eventName").equals(eventName)){
                    clickEventFound = true;
                    break;
                }
            }
            assertEquals(true,clickEventFound);
        }
        catch (Exception e){
            Log.e(TAG, "Exception :" + e);
        }

    }
    public void testShowingPromotionsStoredInDb(){
        try{
            JSONArray promotions = new JSONArray(promotionsForScreensJSON);
            for(int i =0; i < promotions.length(); i++){
                JSONObject promotion = promotions.getJSONObject(i);
                JSONObject suppressionLogic = promotion.getJSONObject("suppressionLogic");

                dbHelper.addPromotion(promotion.toString(), promotion.getString("campaignId"), promotion.getString("screenId"),
                        suppressionLogic.getInt("maximumNumberOfTimesToShow"),
                        suppressionLogic.getInt("minimumDurationBeforeReshowInMin"));
            }
            Thread.sleep(5000);

        }catch (Exception e){
            Log.e(TAG,"Exception: " +e);
        }
    }

    public void testDownloadPromotionsAndDataStore(){
        //TODO use mocked API response
        try {
            ZeTarget.syncDataStore();
            ZeTarget.checkPromotions();
            Thread.sleep(20000);
            JSONObject promotion = dbHelper.getPromotionforScreen("ViewController4");
            assertNotNull(promotion);
            assertEquals("app", ZeTarget.getData("text", "default"));
            assertEquals("55", ZeTarget.getData("price", "default"));
            assertEquals("66", ZeTarget.getData("quantity", "default"));
        }
        catch (Exception e){
            Log.e(TAG, "Exception :" + e);
        }
    }
}
