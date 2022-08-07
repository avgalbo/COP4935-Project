package edu.ucf.CD9;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.android.volley.RequestQueue;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

/**
 * Created by abhiditya
 */

public class AppStatus extends Application implements Application.ActivityLifecycleCallbacks {
    static boolean WelcomeActivityForeground;
    static boolean MainActivityForeground;
    static boolean AnalysisActivityForeground;
    static boolean InitialSelectTrustForeground;
    static boolean ChildTutorialForeground;
    static GoogleSignInAccount account;
    static RequestQueue queue;
    static String lastMsgId;
    static String hashedID;
    public static boolean isWelcomeActivityForeground() {
        return WelcomeActivityForeground;
    }
    public static boolean isMainActivityForeground() {
        return MainActivityForeground;
    }
    public static boolean isAnalysisActivityForeground() {
        return AnalysisActivityForeground;
    }
    public static boolean isInitialSelectTrustForeground(){
        return InitialSelectTrustForeground;
    }
    public static boolean isChildTutorialForeground() { return ChildTutorialForeground; }

    static String serverURL = "http://ec2-3-95-69-177.compute-1.amazonaws.com";//"http://18.116.215.171";//"http://10.0.2.2:8888"; //https://parentalcontrolappresearch.tk
//"https://parentalcontrolappresearch.tk/CD9"
    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (activity instanceof WelcomeActivityParent) {
            WelcomeActivityForeground = true;
        }
        else if (activity instanceof MainActivity) {
            MainActivityForeground = true;
        }
        else if(activity instanceof  AnalysisActivity){
            AnalysisActivityForeground = true;
        } else if (activity instanceof WelcomeActivityChild) {
            WelcomeActivityForeground = true;
        } else if(activity instanceof  InitialSelectTrust){
            InitialSelectTrustForeground = true;
        } else if(activity instanceof ChildTutorial){
            ChildTutorialForeground = true;
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
        if (activity instanceof WelcomeActivityParent) {
            WelcomeActivityForeground = false;
        }
        else if (activity instanceof MainActivity) {
            MainActivityForeground = false;
        }
        else if(activity instanceof  AnalysisActivity){
            AnalysisActivityForeground = false;
        } else if (activity instanceof WelcomeActivityChild) {
            WelcomeActivityForeground = false;
        } else if (activity instanceof InitialSelectTrust){
            InitialSelectTrustForeground = false;
        } else if(activity instanceof ChildTutorial){
            ChildTutorialForeground = false;
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

    }

    // onActivityDestroyed Test comment
    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}
