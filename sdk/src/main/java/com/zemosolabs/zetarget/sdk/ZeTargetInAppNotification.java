package com.zemosolabs.zetarget.sdk;

import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import org.json.JSONObject;

/**
 * Created by vedaprakash on 3/3/15.
 */
public abstract class ZeTargetInAppNotification extends DialogFragment {


     /**
     * <p>Customize InAppNotification, providing Context "context",String "campaignId",JSONOjbect "template"
     * as arguments.</p>
     *
     * <p>JSONObject template structure:</p>
     * <p>"template": { </p>
     * <p>     "title": "Season sale! Heavy discounts!!!", </p>
     * <p>     "message": "Buy the stuff now and save a loot of money and use that money to buy some more stuff!!!", </p>
     * <p>     "imageUrl": "http://news.bbcimg.co.uk/media/images/81539000/jpg/_81539447_95ca831d-7a1d-4b02-b3ca-0c9968649937.jpg", </p>
     * <p>     "onClickUrl": "http://www.google.com", </p>
     * <p>     "definition": { </p>
     * <p>         "actionType": "SHARE|LINK|RATE|NONE", </p>
     * <p>         "actionButton": { </p>
     * <p>             "url": "http://www.facebook.com" </p>
     * <p>             "shareText": "This app is awesome!!! Check it out!" </p>
     * <p>             "buttonText": "LIKE US|SHARE|RATE US" </p>
     * <p>          } </p>
     * <p>         "dismissButtonText": "GOT IT|CANCEL|DON'T ASK ME AGAIN", </p>
     * <p>         "remindLaterButtonText: "REMIND ME LATER", </p>
     * <p>      } </p>
     * <p> } </p>
     *
     */
    public abstract void customize(Context context, String campaignId, JSONObject template);

    //Include super.onCreate() if overriding this method in the concrete class.
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Pick a style based on the num.
        int style = DialogFragment.STYLE_NORMAL, theme = 0;
        setStyle(style, theme);
    }
    //use the string fields title and message in the views where title and message need to be displayed.
    @Override
    public abstract View onCreateView(LayoutInflater inflater, ViewGroup container,
                                      Bundle savedInstanceState);

    //Include super.dismiss() if overriding this method in the concrete class.
    @Override
    public void dismiss() {
        super.dismiss();
    }

    //Include super.onPause() if overriding this method in the concrete class.
    @Override
    public void onPause() {
        super.onPause();
    }
}
