package com.zemosolabs.zetarget.sdk;

/**
 * Created by praveen on 19/01/15.
 */
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

public class DeviceDetails {

    private static final String TAG = "ZeTarget.DevDetails";

    private boolean locationListening = true;

    private static Context context;


    private static String versionName;
    private static final String osFamily= "android";
    private static final String osVersion = Build.VERSION.RELEASE;
    private static final String brand = Build.BRAND;
    private static final String manufacturer = Build.MANUFACTURER;
    private static final String model =Build.MODEL;
    private static String carrier;
    private static int ostzOffset = TimeZone.getDefault().getRawOffset();
    private static final String language = Locale.getDefault().getLanguage();

    // Cached properties, since fetching these take time
    private String advertisingId;
    private String country;

    public DeviceDetails(Context context) {
        DeviceDetails.context = context;
    }

    public void getadditionalDetails(){
        setVersionName();
        setCarrier();
    }

    public static Locale getLocale() {
        return Locale.getDefault();
    }

    public static String getLocaleString(){
        return getLocale().toString();
    }

    public static String getOstz(){
        StringBuilder sB = new StringBuilder();
        if(ostzOffset>0){
            sB.append("+");
        }else{
            sB.append("-");
        }
        int hours = ostzOffset/(3600*1000);
        int mins = (ostzOffset%(3600*1000))/(60*1000);
        sB.append(String.format("%02d",hours));
        sB.append(String.format("%02d",mins));
        String offset = sB.toString();
        //Log.i("ostzOffset",offset);
        return offset;
    }

    public String getVersionName() {
        return versionName;
    }

    private static void setVersionName() {
        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            versionName = packageInfo.versionName;
        } catch (NameNotFoundException e) {
            versionName = null;
        }

    }

    public static int getAppVersionCode() {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    public static String getApplicationName() {
        try {
            int stringId = context.getApplicationInfo().labelRes;
            return context.getString(stringId);
        }
        catch (Exception e){
            return "";
        }

    }

    public String getOSName() {
        String osName="";
        Field[] fields = Build.VERSION_CODES.class.getFields();
        for(Field field:fields){
            try {
                if(field.getInt(new Object())==Build.VERSION.SDK_INT){
                    osName = field.getName();
                    break;
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        //Log.i("osName",osName);
        return osName;
    }

    public static String getOsFamily() {
        return osFamily;
    }

    public static String getOSVersion() {
        return osVersion;
    }

    public static String getBrand() {
        return brand;
    }

    public static String getManufacturer() {
        return manufacturer;
    }

    public static String getModel() {
        return model;
    }

    public String getCarrier() {
        return carrier;
    }

    private static void setCarrier() {
        TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        carrier= manager.getNetworkOperatorName();
    }

    public String getCountry() {
        if (country == null) {
            country = getCountryUncached();
        }
        return country;
    }

    // @VisibleForTesting
    protected Geocoder getGeocoder() {
        return new Geocoder(context, Locale.ENGLISH);
    }

    private String getCountryFromLocation() {
        if (!isLocationListening()) { return null; }

        Location recent = getMostRecentLocation();
        if (recent != null) {
            try {
                Geocoder geocoder = getGeocoder();
                List<Address> addresses = geocoder.getFromLocation(recent.getLatitude(),
                        recent.getLongitude(), 1);
                if (addresses != null) {
                    for (Address address : addresses) {
                        if (address != null) {
                            return address.getCountryCode();
                        }
                    }
                }
            } catch (IOException e) {
                // Failed to reverse geocode location
            }
        }
        return null;
    }

    private String getCountryFromNetwork() {
        TelephonyManager manager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        if (manager.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) {
            String country = manager.getNetworkCountryIso();
            if (country != null) {
                return country.toUpperCase(Locale.US);
            }
        }
        return null;
    }

    private String getCountryFromLocale() {
        return Locale.getDefault().getCountry();
    }

    private String getCountryUncached() {
        // This should not be called on the main thread.

        // Prioritize reverse geocode, but until we have a result from that,
        // we try to grab the country from the network, and finally the locale
        String country = getCountryFromLocation();
        if (!TextUtils.isEmpty(country)) {
            return country;
        }

        country = getCountryFromNetwork();
        if (!TextUtils.isEmpty(country)) {
            return country;
        }
        return getCountryFromLocale();
    }



    public static String getLanguage() {
        return language;
    }

    public String getAdvertisingId() {
        // This should not be called on the main thread.
        if (advertisingId == null) {
            try {
                Class AdvertisingIdClient = Class
                        .forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");
                Method getAdvertisingInfo = AdvertisingIdClient.getMethod("getAdvertisingIdInfo",
                        Context.class);
                Object advertisingInfo = getAdvertisingInfo.invoke(null, context);
                Method isLimitAdTrackingEnabled = advertisingInfo.getClass().getMethod(
                        "isLimitAdTrackingEnabled");
                Boolean limitAdTrackingEnabled = (Boolean) isLimitAdTrackingEnabled
                        .invoke(advertisingInfo);

                if (limitAdTrackingEnabled) {
                    return null;
                }
                Method getId = advertisingInfo.getClass().getMethod("getId");
                advertisingId = (String) getId.invoke(advertisingInfo);
            } catch (ClassNotFoundException e) {
                if(ZeTarget.isDebuggingOn()){
                    Log.w(TAG, "Google Play Services SDK not found!");
                }
            } catch (Exception e) {
                if(ZeTarget.isDebuggingOn()){
                    Log.e(TAG, "Encountered an error connecting to Google Play Services", e);
                }
            }
        }
        return advertisingId;
    }

    public String generateUUID() {
        return UUID.randomUUID().toString();
    }

    public Location getMostRecentLocation() {

        if (!isLocationListening()) { return null; }

        LocationManager locationManager = (LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        List<Location> locations = new ArrayList<Location>();
        for (String provider : providers) {
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
                locations.add(location);
            }
        }

        long maximumTimestamp = -1;
        Location bestLocation = null;
        for (Location location : locations) {
            if (location.getTime() > maximumTimestamp) {
                maximumTimestamp = location.getTime();
                bestLocation = location;
            }
        }

        return bestLocation;
    }

    public boolean isLocationListening() {
        return locationListening;
    }

    public void setLocationListening(boolean locationListening) {
        this.locationListening = locationListening;
    }

    int getScreenDensity(){
        return DisplayMetrics.DENSITY_DEFAULT;
    }
    String getScreenResolution() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        return screenHeight+" X "+screenWidth;
    }

}
