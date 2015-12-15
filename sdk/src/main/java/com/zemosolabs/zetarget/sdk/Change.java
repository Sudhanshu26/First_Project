package com.zemosolabs.zetarget.sdk;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import static android.util.Log.*;

/**
 * Created by vedaprakash on 17/4/15.
 */
public class Change {
    View viewOfConcern;
    private JSONObject change;
    private ArrayList<ViewElement> Hierarchy = new ArrayList<ViewElement>();
    private Activity currentActivity;
    boolean valid=false;
    private int position;
    private String viewName;
    Change(JSONObject change,Activity activity){
        this.change = change;
        this.currentActivity=activity;
    }
    boolean prepare(){
        position=0;
        valid = true;
        View rootView = currentActivity.getWindow().getDecorView().getRootView();
        updateHierarchy();
        ViewElement rootElement = Hierarchy.remove(0);
        position++;
        viewName = rootElement.viewClassName;
        if(!(rootView instanceof ViewGroup)){
            valid=false;
        }
        find(rootElement, (ViewGroup) rootView,rootElement.index,position);
        if(viewOfConcern==null){
            valid=false;
        }
        if(!pathValid()){
            //Log.i("Path invalid",this.viewName);
            viewOfConcern=null;
            valid=false;
        }

        return valid;
    }

    private boolean pathValid() {
        if(Hierarchy.size()!=0){
            if(!(viewOfConcern instanceof ViewGroup)){
                return false;
            }else{
                ViewElement vElement = Hierarchy.remove(0);
                position++;
                viewName = vElement.viewClassName;
                int pos = vElement.position;
                if(vElement.index>=((ViewGroup)viewOfConcern).getChildCount()){
                    return false;
                }
                viewOfConcern = ((ViewGroup)viewOfConcern).getChildAt(vElement.index);


                if(matches(vElement,viewOfConcern,vElement.index,pos)) {
                    pathValid();
                }else{
                    //Log.i("Path Not Valid","Look in Matches");
                    return false;
                }
            }
        }
        return true;
    }

    void make() {
        if(viewOfConcern==null){
            //Log.i("make","view of concern is null. returning without making this change");
            return;
        }
        try {
            String methodName = change.getJSONObject("property").getJSONObject("set").getString("methodName");
            ArrayList<Class<?>> paramTypes = new ArrayList<Class<?>>();
            Class<?>[] contents = new Class<?>[1];
            JSONArray params = change.getJSONObject("changeInProperty").getJSONArray("parameters");
            for(int i=0;i<params.length();i++){
                String paramType = params.getJSONObject(i).getString("type");
                Class<?> type;
                switch(paramType){
                    case "float":
                        type = Float.TYPE;
                        break;
                    case "double":
                        type = Double.TYPE;
                        break;
                    case "int":
                        type = Integer.TYPE;
                        break;
                    case "long":
                        type = Long.TYPE;
                        break;
                    case "short":
                        type = Short.TYPE;
                        break;
                    default:
                        type = Class.forName(paramType);
                }
                paramTypes.add(type);
            }
            ArrayList<Object> paramValues = new ArrayList<>();
            Object[] paramVs = new Object[1];
            for(int i=0;i<params.length();i++){
                Object value = params.getJSONObject(i).get("value");
                paramValues.add(value);
            }
            Class<?> viewClass = Class.forName(viewName);
            i("changeMethodCall", viewOfConcern.getClass().getCanonicalName() + ":" + methodName + "(" + paramValues.toString() + ")");
            Method method = viewClass.getMethod(methodName, paramTypes.toArray(contents));
            method.invoke(viewOfConcern,paramValues.toArray(paramVs));
        } catch (JSONException e) {
            e("Change Make", e.toString());
        } catch (ClassNotFoundException e) {
            e("Change Make", e.toString());
        } catch (NoSuchMethodException e) {
            e("Change Make", e.toString());
        } catch (InvocationTargetException e) {
            e("Change Make", e.toString());
        } catch (IllegalAccessException e) {
            e("Change Make", e.getMessage());
        }
    }

    private void find(ViewElement rootElement, ViewGroup currentView,int index,int pos){
        if(matches(rootElement,currentView,index,pos)){
            viewOfConcern = currentView;
        }
        else{
            for(int i=0;i<currentView.getChildCount();i++) {
                View v =currentView.getChildAt(i);
                if(matches(rootElement,v,i,pos)){
                    viewOfConcern = v;
                }else{
                    if (v instanceof ViewGroup) {
                        find(rootElement,(ViewGroup) v,i,pos);
                    }else {
                        viewOfConcern = null;
                    }
                }
            }
        }
    }
    private boolean matches(ViewElement element, View view,int index,int pos){
        if(!view.getClass().getCanonicalName().equalsIgnoreCase(element.viewClassName)){
            i("matchingNames",element.viewClassName+","+view.getClass().getCanonicalName());
            return false;
        }
        if(view.getId()!=element.viewId){
            i("matchingId",element.viewId+","+view.getId());
            return false;
        }
        if(index!=element.index){
            i("matchingIndex",element.index+","+index);
            return false;
        }
        if(pos!=position){
            i("matchingPosition","JSON integrity check failed");
        }
        return true;
    }

    private void updateHierarchy() {
        try {
            JSONArray hierarchy = change.getJSONArray("hierarchy");
            for(int i=0;i<hierarchy.length();i++){
                JSONObject jO = (JSONObject)hierarchy.get(i);
                String className = jO.getString("viewClassName");
                int id = jO.getInt("viewId");
                int ix = jO.getInt("index");
                int pos = jO.getInt("position");
                ViewElement viewElement = new ViewElement(className,id,ix,pos);
                Hierarchy.add(viewElement);
            }
            i("Change", "Hierarchy Updated");
        } catch (JSONException e) {
            e("Change UpdateHier", e.getMessage());
        }
    }

    @Override
    public String toString() {
        String details="Activity: "+currentActivity.getClass().getCanonicalName()+"\nChangeJSON: "+change;
        return super.toString()+"\n"+details;
    }

    private class ViewElement {
        String viewClassName;
        int viewId,index,position;

        ViewElement(String className, int id, int indX, int pos){
            viewClassName = className;
            viewId = id;
            index = indX;
            position = pos;
        }
    }
}
