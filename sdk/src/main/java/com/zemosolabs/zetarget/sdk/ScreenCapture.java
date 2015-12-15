package com.zemosolabs.zetarget.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.Environment;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import android.os.Vibrator;

/**
 * Created by vedaprakash on 30/3/15.
 */
class ScreenCapture extends VibrationManager{
    private Activity currentActivity;
    private View rootView;
    private static ScreenCapture instance;
    private JSONObject viewsInAPage;
    private File file;
    private String editSessionId ="SOIEHFUBSIFJPEHPIWPJFHIOW";

    private VibrationManager vibrator;
    static boolean isVibrate = false;




    private ScreenCapture(){


    }
    void initialize(){
        currentActivity = ZeTargetActivityLifecycleCallbacks.currentActivity;
        rootView = currentActivity.getWindow().getDecorView().getRootView();
        String packageName = currentActivity.getPackageName();
        viewsInAPage = new JSONObject();
        try {
            viewsInAPage.put("osName",CommonUtils.replaceWithJSONNull(ZeTarget.deviceDetails.getOSName()));
            viewsInAPage.put("osFamily",CommonUtils.replaceWithJSONNull(ZeTarget.deviceDetails.getOsFamily()));
            viewsInAPage.put("osVersion",CommonUtils.replaceWithJSONNull(ZeTarget.deviceDetails.getOSVersion()));
            viewsInAPage.put("brand",CommonUtils.replaceWithJSONNull(ZeTarget.deviceDetails.getBrand()));
            viewsInAPage.put("manufacturer",CommonUtils.replaceWithJSONNull(ZeTarget.deviceDetails.getManufacturer()));
            viewsInAPage.put("deviceModel",CommonUtils.replaceWithJSONNull(ZeTarget.deviceDetails.getModel()));
            viewsInAPage.put("deviceResolution",CommonUtils.replaceWithJSONNull(ZeTarget.deviceDetails.getScreenResolution()));
            viewsInAPage.put("language",CommonUtils.replaceWithJSONNull(ZeTarget.deviceDetails.getLanguage()));
            viewsInAPage.put("editingSessionId",CommonUtils.replaceWithJSONNull(editSessionId));
            viewsInAPage.put("appName",CommonUtils.replaceWithJSONNull(packageName));
            viewsInAPage.put("screenName",CommonUtils.replaceWithJSONNull(ZeTargetActivityLifecycleCallbacks.currentActivity.getClass().getCanonicalName()));

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    synchronized static ScreenCapture  getInstance(){
        if(instance==null){
           return instance = new ScreenCapture();
        }
        return instance;
    }

    void captureAndSend(){
        try {
            JSONObject screenDetails = new JSONObject();
            screenDetails.put("hierarchyAndProps",buildHierarchy(rootView, -1));
            screenDetails.put("screenshot",retrieveSnapshotOfView(rootView));
            screenDetails.put("screenshotType","png");
            viewsInAPage.put("screenDetails",screenDetails);
            String snap = retrieveSnapshotOfView(rootView);
            PackageManager pm = currentActivity.getApplicationContext().getPackageManager();
            int hasPerm = pm.checkPermission("android.permission.VIBRATE", currentActivity.getApplicationContext().getPackageName());
            if(hasPerm==pm.PERMISSION_GRANTED) {
                vibrator = (VibrationManager) VibrationManager.getManager(currentActivity.getApplicationContext());
                Vibrator v = vibrator.getVibrator();
                v.vibrate(300);
                isVibrate =true;
            }


            File newFile = new File(Environment.getExternalStorageDirectory()+"/newfile1.txt");
            FileWriter fw = new FileWriter(newFile);
            fw.write(snap);
            fw.close();
           byte[] decodedString = Base64.decode(snap.getBytes(), Base64.NO_WRAP);
            //Bitmap decoded = BitmapFactory.decodeByteArray(decodedString,0,decodedString.length);
            File file = new File(Environment.getExternalStorageDirectory()+"/screenshot "+System.currentTimeMillis()+".png");
//            Bitmap bmp  = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory()+"/newfile.txt");
            try {
                FileOutputStream out = new FileOutputStream(file);
                out.write(decodedString);
                //decoded.compress(Bitmap.CompressFormat.JPEG, 600, out);
                out.flush();
                out.close();

            } catch (Exception e) {
                e.printStackTrace();
            }





        } catch (JSONException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //createNewFile();
        //writeToFile(viewsInAPage.toString());
        ZeTarget.sendSnapshot(viewsInAPage);

    }
    private JSONObject buildHierarchy(View view,int index){
        JSONObject viewHierarchy = new JSONObject();
        try {

            viewHierarchy.putOpt("contentDescription",view.getContentDescription());
            viewHierarchy.put("id",view.getId());
            viewHierarchy.put("index",index);
            Class<?> viewClass = view.getClass();
            JSONArray classes = new JSONArray();
            while(viewClass!=Object.class){
                classes.put(viewClass.getCanonicalName());
                viewClass = viewClass.getSuperclass();
            }
            viewHierarchy.put("classes",classes);
            /*viewHierarchy = writeToJSONViewDimAndLoc(view,viewHierarchy);
            viewHierarchy = writeToJSONViewPadding(view,viewHierarchy);*/
            viewHierarchy = writeToJSONVIewAllProps(view,viewHierarchy);
            JSONArray childrenViews = new JSONArray();
            if(view instanceof android.view.ViewGroup){
                ViewGroup vg = (ViewGroup)view;
                int size = (vg).getChildCount();
                for(int i=0; i<size; i++){
                    childrenViews.put(buildHierarchy(vg.getChildAt(i),i));
                }
                viewHierarchy.put("children",childrenViews);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return viewHierarchy;
    }



    private JSONObject writeToJSONVIewAllProps(View v,JSONObject j) throws NoSuchMethodException {

        try{

            JSONArray allViewProps = new JSONArray();
            Method[] methodsOfView = v.getClass().getMethods();
            populateRequiredMethodNames();
            for(Method methodUnderInspection:methodsOfView){
//            for(Method methodUnderInspection:methodList){
            if(methodUnderInspection.getParameterTypes().length==0){
                    if(methodUnderInspection.getReturnType()!=Void.TYPE && methodUnderInspection.getReturnType()!=Void.class){
                        String methodName = methodUnderInspection.getName();
//                        if(isAccessorMethod(methodName)) {
                         if(requiredMethodNames.containsKey(methodName)){
                            JSONObject property = new JSONObject();
                            try {
                                property.put("name",methodName);
                                Object value = methodUnderInspection.invoke(v);
                                if(value instanceof Drawable){
                                    String base64BackgroundDrawable = getBase64ImageOfDrawable((Drawable)((Drawable) value).mutate());
                                    property.put("value",base64BackgroundDrawable);
                                    property.put("type","Base64Bitmap");
                                   // property.put("drawableFeatures",writeToJSONDrawableProps((Drawable)value));
                                }else {
                                    property.put("value", methodUnderInspection.invoke(v));
                                    property.put("type", methodUnderInspection.getReturnType().getCanonicalName());
                                }
                            } catch (InvocationTargetException e) {
                                property.put("name", methodName);
                                property.put("value","Exception Thrown");
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                             allViewProps.put(property);
                        }
                    }
                }
            }
            j.put("props",allViewProps);
        }catch(JSONException e){
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return j;
    }

    static HashMap<String,String> requiredMethodNames= new HashMap<String,String>();

    private void populateRequiredMethodNames() {
        if (requiredMethodNames.size()==0) {
            requiredMethodNames.put("getAlpha","getAlpha");
            requiredMethodNames.put("getBaseLine","getBaseLine");
            requiredMethodNames.put("getBottom","getBottom");
            requiredMethodNames.put("getClass","getClass");
            requiredMethodNames.put("getHeight","getHeight");
            requiredMethodNames.put("getCurrentTextColor","getCurrentTextColor");
            requiredMethodNames.put("getHighlightColor","getHighlightColor");
            requiredMethodNames.put("getHint","getHint");
            requiredMethodNames.put("getId","getId");
            requiredMethodNames.put("getLeft","getLeft");
            requiredMethodNames.put("getRight","getRight");
            requiredMethodNames.put("getLineCount","getLineCount");
            requiredMethodNames.put("getLineHeight","getLineHeight");
            requiredMethodNames.put("getText","getText");
            requiredMethodNames.put("getTextLocale","getTextLocale");
            requiredMethodNames.put("getTextSize","getTextSize");
            requiredMethodNames.put("getVisibility","getVisibility");
            requiredMethodNames.put("getTop","getTop");
            requiredMethodNames.put("getWidth","getWidth");

        }

    }

    private JSONArray writeToJSONDrawableProps(Drawable value) {
        populateRequiredMethodNames();
        JSONArray drawableProps = new JSONArray();
        try{
            Method[] methodsOfView = value.getClass().getMethods();
            for(Method methodUnderInspection:methodsOfView){
                if(methodUnderInspection.getParameterTypes().length==0){
                    if(methodUnderInspection.getReturnType()!=Void.TYPE && methodUnderInspection.getReturnType()!=Void.class){
                        String methodName = methodUnderInspection.getName();
                        if (requiredMethodNames.containsKey(methodName)){
                        //if(isAccessorMethod(methodName)) {
                            JSONObject property = new JSONObject();
                            try {
                                property.put("name",methodName);
                                property.put("value", methodUnderInspection.invoke(value));
                                property.put("type", methodUnderInspection.getReturnType().getCanonicalName());
                            } catch (InvocationTargetException e) {
                                property.put("name", methodName);
                                property.put("value","Exception Thrown");
                            } catch (IllegalAccessException e) {
                                property.put("name", methodName);
                                property.put("value","Exception Thrown");
                            }
                            drawableProps.put(property);
                        }
                    }
                }
            }
        }catch(JSONException e){
            if(ZeTarget.isDebuggingOn()){
                //Log.e("ScreenCapture","Writing Drawable to json",e);
            }
        }
        return drawableProps;
    }


    private String getBase64ImageOfDrawable(Drawable value) throws IOException {
        if(value instanceof BitmapDrawable){
            return base64ScreenshotOf(((BitmapDrawable) value).getBitmap());
        }

        int width = !value.getBounds().isEmpty()? value.copyBounds().width(): value.getIntrinsicWidth();
        int height = !value.getBounds().isEmpty()? value.copyBounds().height():value.getIntrinsicHeight();
        width = (width<=0)? 3 : width;
        height = (height<=0)? 3:height;
        //Log.i("ZeTarget","Base64BmpFromDrawable "+width);
        //Log.i("ZeTarget","Base64BmpFromDrawable "+height);
        Bitmap bitmap = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        value.setBounds(0,0,canvas.getWidth(),canvas.getHeight());
        value.draw(canvas);

        return base64ScreenshotOf(bitmap);
    }

    private String retrieveSnapshotOfView(View v) throws IOException {
        Bitmap rootViewScreenshot= null;
        try {
            Method createSnapshot = View.class.getDeclaredMethod("createSnapshot", Bitmap.Config.class, Integer.TYPE, Boolean.TYPE);
            createSnapshot.setAccessible(true);
            rootViewScreenshot = (Bitmap)createSnapshot.invoke(v,Bitmap.Config.ARGB_8888, Color.WHITE,false);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        if(rootViewScreenshot==null){
            if(v.isDrawingCacheEnabled()){
                rootViewScreenshot = v.getDrawingCache();
            }else{
                v.setDrawingCacheEnabled(true);
                rootViewScreenshot = v.getDrawingCache().copy(Bitmap.Config.ARGB_8888,true);
                v.setDrawingCacheEnabled(false);
            }
        }

        return base64ScreenshotOf(rootViewScreenshot);
    }

    private String base64ScreenshotOf(Bitmap rootViewScreenshot) throws IOException {
        String base64Screenshot = "";

        if(rootViewScreenshot!=null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Base64OutputStream b64os = new Base64OutputStream(baos, Base64.NO_WRAP);
            rootViewScreenshot.compress(Bitmap.CompressFormat.PNG, 100, b64os);

            try {
                b64os.flush();
                b64os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            byte[] bitmapBytes = baos.toByteArray();
            base64Screenshot = Base64.encodeToString(bitmapBytes, Base64.NO_WRAP);

//            File newFile = new File(Environment.getExternalStorageDirectory()+"/newfile2.txt");
//            FileWriter fw = new FileWriter(newFile);
//            fw.write(base64Screenshot);
//            fw.close();
//            writeToFile(baos);
//            File newfile = new File(Environment.getExternalStorageDirectory()+"/123"+System.currentTimeMillis()+".png");
//
//            FileOutputStream bitmapfos = new FileOutputStream(newfile);
//            rootViewScreenshot.compress(Bitmap.CompressFormat.PNG,100,bitmapfos);
//            try {
//                bitmapfos.flush();
//                bitmapfos.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            /*int screenShotDensity = rootViewScreenshot.getDensity();
//            Float scale = (float)(ZeTarget.deviceDetails.getScreenDensity()/screenShotDensity);*/
//        }
//        Log.d("BASE64",base64Screenshot);
        }
        return base64Screenshot;
    }


//    public void writeToFile(ByteArrayOutputStream base64) throws IOException {
//        //byte[] decoded = Base64.decode(base64.getBytes(),Base64.DEFAULT);
////
////        File newFile = new File(Environment.getExternalStorageDirectory()+"/newfile3.txt");
////        FileWriter fw = new FileWriter(newFile);
////        fw.write(base64);
////        fw.close();
//      //  ByteArrayInputStream bais = new ByteArrayInputStream(base64.toByteArray());
//        //byte[] decoded = Base64.decode(base64,0);
//        String base = base64.toString();
//        byte[] decoded = Base64.decode(base, 0);
//        Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
//
//        File file = new File(Environment.getExternalStorageDirectory()+"/456"+System.currentTimeMillis()+".png");
//
//
//
//        try {
//            FileOutputStream out = new FileOutputStream(file);
//            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
//
//               out.flush();
//               out.close();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }
    private boolean isAccessorMethod(String methodName){
        if(methodName.matches("has[A-Z].*")){
            return true;
        }else if (methodName.matches("get[A-Z].*")){
            return true;
        }else if(methodName.matches("is[A-Z].*")){
            return true;
        }else if(methodName.matches("should[A-Z].*")){
            return true;
        }else if(methodName.matches("will[A-Z].*")){
            return true;
        }else if(methodName.matches("can[A-Z].*")){
            return true;
        }
        else
            return false;
    }

    /*void writeToFile(String data){
        Log.i("writeFile:","In the method");
        try {
            // FileOutputStream fOut = new FileOutputStream(file);
            FileWriter myOutWriter;
            myOutWriter = new FileWriter(file,true);
            myOutWriter.write(data);
            myOutWriter.flush();
            myOutWriter.close();
            Log.d("WRITE","SUCCESS");
        } catch (Exception e) {
            Log.d("WRITE","ERROR"+e.toString());
        }
    }*/

    /*void createNewFile(){
        try {
            file = new File("/sdcard/ZeTarget/fetchPromoJSONReceived.txt");
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/
}
