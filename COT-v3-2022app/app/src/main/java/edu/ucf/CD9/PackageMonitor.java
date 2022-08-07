package edu.ucf.CD9;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;


public class PackageMonitor extends BroadcastReceiver {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(final Context context, Intent intent) {

        new UploadAppInfoTask(this, intent, context).execute();

    }

    public static byte[] getFileDataFromDrawable(Context context, Drawable drawable) {
        final Bitmap bmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bmp);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 80, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    private static String getSHA512Hash(String stringToHash){
        String hash = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update("TeenDevice".getBytes("UTF-8"));
            byte[] bytes = md.digest(stringToHash.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte aByte : bytes) {
                sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
            }
            hash = sb.toString();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return hash;
    }






    // replace
    private static String getDeviceID(Context context){

        String deviceUniqueIdentifier = null;
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            do_setup(context);
            return "No Permission";
        }
        if (null != tm) {
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                if(tm.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM)
                    deviceUniqueIdentifier=tm.getImei();
                else
                    deviceUniqueIdentifier=tm.getMeid();
            }
            else
            {
                deviceUniqueIdentifier=tm.getDeviceId();
            }
        }
        if (null == deviceUniqueIdentifier || 0 == deviceUniqueIdentifier.length()) {
            deviceUniqueIdentifier = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        return deviceUniqueIdentifier;
    }

    static  void do_setup(Context context){
        SharedPreferences pref = context.getSharedPreferences("MyPref", 0); // 0 - for private mode
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("setup_needed", true).apply();
        context.startActivity(new Intent(context, WelcomeActivityChild.class));
    }


    private static class UploadAppInfoTask extends AsyncTask<Void, Void, Void> {

        private WeakReference<PackageMonitor> broadcastReceiverReference;
        private WeakReference<Intent> intentWeakReference;
        private WeakReference<Context> contextWeakReference;
        private Intent intent;
        private Context context;
        String url = AppStatus.serverURL+"/upload-apps.php";
        private static int retryms = 5000;

        // only retain a weak reference to the activity
        UploadAppInfoTask(PackageMonitor packageMonitor, Intent intent, Context context) {
            broadcastReceiverReference = new WeakReference<>(packageMonitor);
            intentWeakReference = new WeakReference<Intent>(intent);
            contextWeakReference = new WeakReference<Context>(context);
            this.intent = intentWeakReference.get();
            this.context = contextWeakReference.get().getApplicationContext();
            if(AppStatus.queue==null)
                AppStatus.queue = Volley.newRequestQueue(this.context.getApplicationContext());
        }

        @Override
        protected Void doInBackground(Void... params) {
            if(intent==null)
                return null;

            if(context==null)
                return null;

            boolean uninstall = false;

            SharedPreferences pref = context.getSharedPreferences("MyPref", 0); // 0 - for private mode
            SharedPreferences.Editor editor = pref.edit();

            final String packageName = intent.getData().getEncodedSchemeSpecificPart();

            if(pref.getString("LUP", "").equals(packageName)){
                //install broadcast for updating app, ignore
                editor.remove("LUP").apply();
                return null;
            }

            // when package removed
            if (intent.getAction().equals("android.intent.action.PACKAGE_REMOVED")) {
                if(intent.getBooleanExtra(Intent.EXTRA_DATA_REMOVED, true)){
                    //app is being uninstalled
                    Log.e(" BroadcastReceiver ", "onReceive called "
                            + " PACKAGE_REMOVED ");
                    uninstall = true;
                }else {
                    //app is being updated
                    Log.e(" BroadcastReceiver ", "onReceive called "
                            + " PACKAGE BEING UPDATED ");
                    editor.putString("LUP", packageName).apply(); //save package name to filter imminent install broadcast
                    return null;
                }
            }

            // when package installed
            else if (intent.getAction().equals("android.intent.action.PACKAGE_ADDED")) {
                Log.e(" BroadcastReceiver ", "onReceive called " + "PACKAGE_ADDED");
            }

            final boolean uninstalled = uninstall;

            final PackageManager pm = context.getPackageManager();

//            if (pm.getLaunchIntentForPackage(packageName) == null)
//                return null;

            String appName = "";
            Drawable icon = null;
            if(!uninstalled){
                //get app name
                ApplicationInfo ai;
                try {
                    ai = pm.getApplicationInfo(packageName, 0);
                } catch (final PackageManager.NameNotFoundException e) {
                    ai = null;
                }
                appName = (String) (ai != null ? pm.getApplicationLabel(ai) : "(unknown)");

                //get icon
                try{
                    icon = pm.getApplicationIcon(packageName);
                }
                catch (PackageManager.NameNotFoundException e){
                    e.printStackTrace();
                    icon = ContextCompat.getDrawable(context, R.mipmap.ic_launcher);
                }
            }
            final String applicationName = appName;
            final Drawable appicon = icon;

            VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(Request.Method.POST, url, new Response.Listener<NetworkResponse>() {
                @Override
                public void onResponse(NetworkResponse response) {
                    String resultResponse = new String(response.data);
                    // parse success output
                    Log.e("RESPONSE", resultResponse);
                    if(resultResponse.equals("do setup")){
                        do_setup(context);
                    }
                    retryms = 5000;
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    error.printStackTrace();
                    try {
                        Thread.sleep(retryms<600000?retryms+=retryms*0.1:600000);
                    }catch (Exception e){
                        //do nothing
                    }
                    doInBackground();
                }
            }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("dev_id", getSHA512Hash(getDeviceID(context)));
                    params.put("package", packageName);
                    params.put("install-time", System.currentTimeMillis()/1000 + "");

                    if(uninstalled){
                        params.put("uninstalled", "1");
                    } else {
                        params.put("name", applicationName);
                    }
                    return params;
                }

                @Override
                protected Map<String, DataPart> getByteData() {
                    Map<String, DataPart> params = new HashMap<>();
                    if(!uninstalled)
                        params.put("icon", new DataPart("icon.png", getFileDataFromDrawable(context, appicon), "image/png"));
                    return params;
                }
            };

            multipartRequest.setRetryPolicy(new DefaultRetryPolicy(
                    DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
                    Integer.MAX_VALUE,  // max retries
                    Float.valueOf("1.1") //backoff multiplier
            ));

            AppStatus.queue.add(multipartRequest);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Log.e("AsyncTask", "execution completed");
        }
    }
}