package edu.ucf.CD9;

import android.annotation.SuppressLint;
import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the component that is responsible for device administration
 */
public class MyDeviceAdminReceiver extends DeviceAdminReceiver {
    static int retryms = 100;

    /** Called when this application is approved to be a device administrator. */
    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        if(AppStatus.queue==null)
            AppStatus.queue = Volley.newRequestQueue(context.getApplicationContext());
        SharedPreferences pref = context.getSharedPreferences("MyPref", 0); // 0 - for private mode
        String id = pref.getString("hashedID", null);
        if(id==null)
            id = getSHA512Hash(getDeviceID(context), "TeenDevice");
        Long ts = System.currentTimeMillis()/1000L;
        String ts_now = ts.toString();
        notifyParent(id, "1", ts_now);
        Log.d("DevAdmin", "onEnabled");
    }

    /** Called when this application is no longer the device administrator. */
    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
//        Toast.makeText(context, "Device Admin Removal Received",
//                Toast.LENGTH_LONG).show();
        if(AppStatus.queue==null)
            AppStatus.queue = Volley.newRequestQueue(context.getApplicationContext());
        SharedPreferences pref = context.getSharedPreferences("MyPref", 0); // 0 - for private mode
        String id = pref.getString("hashedID", null);
        if(id==null)
            id = getSHA512Hash(getDeviceID(context), "TeenDevice");
        Long ts = System.currentTimeMillis()/1000L;
        String ts_now = ts.toString();
        notifyParent(id, "0", ts_now);
        Log.d("DevAdmin", "onDisabled");
    }

    private static void notifyParent(final String hashedID, final String status, final String ts_now) {
        //create request to sent
        String url = AppStatus.serverURL+"/dev-admin-revoked.php";
        final StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        retryms=100;
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //Toast.makeText(context, "An error occurred", Toast.LENGTH_LONG).show();
                error.printStackTrace();

                try {
                    Thread.sleep(retryms<200000?retryms+=retryms*0.001:200000);
                }catch (Exception e){
                    //do nothing
                }
                notifyParent(hashedID, status, ts_now);
            }
        }) {
            //adding parameters to the request
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("dev_id", hashedID);
                params.put("status", status);
                params.put("ts", ts_now);
                return params;
            }
        };
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
                Integer.MAX_VALUE,  // max retries
                Float.valueOf("1.001") //backoff multiplier
        ));

        //send
        AppStatus.queue.add(stringRequest);
    }

    //HELPER FUNCTIONS
    private String getSHA512Hash(String stringToHash, String salt){
        String hash = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(salt.getBytes("UTF-8"));
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


    @SuppressLint({"MissingPermission", "HardwareIds"})
    private String getDeviceID(Context context){

        String deviceUniqueIdentifier = null;
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

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
}