package com.zemosolabs.zetarget.sdk;

import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.DisplayMetrics;
import android.util.Log;

/**
 * Created by sudhanshu on 9/10/15.
 */
public class ZResources extends Resources {

    // The reference to the original Resources object that is created by the Android system.

    private final String TAG ="ZeTarget.ZResources";
    private Resources origResource;
    private String currentActivityName;

    public ZResources(AssetManager assets, DisplayMetrics metrics, Configuration config,Resources origResources) {
        super(assets, metrics, config);
        origResource = origResources;
    }

    public ZResources(Resources origResources) {
        super(origResources.getAssets(), origResources.getDisplayMetrics(), origResources.getConfiguration());
        origResource=origResources;
    }

    void setCurrentActivityName(String activityName){
        currentActivityName = activityName;
    }

    private String getCurrentActivityName(){
        return currentActivityName;
    }

    @Override
    public CharSequence getText(int id) throws NotFoundException {
        if(ZeTarget.isDebuggingOn()) {
            Log.d(TAG, "ResourceName: " + getResourceName(id));
        }
        String key = ZeTarget.getKeyFromResourceName(origResource, id);
        if(key != null){
            String replacement = ZeTarget.getInAppTexts().get(key);
            if(replacement != null){
                return replacement;
            }
        }
        return origResource.getText(id);
    }

    @Override
    public XmlResourceParser getLayout(int id) throws NotFoundException {
        ZeTarget.parseLayout(origResource, id,currentActivityName);
        return origResource.getLayout(id);
    }
}
