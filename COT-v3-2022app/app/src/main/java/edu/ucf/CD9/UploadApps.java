package edu.ucf.CD9;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by abhiditya
 */

class UploadApps {
    private Context context;
    private boolean stop;
    String url = AppStatus.serverURL+"/upload-apps.php";

    UploadApps(final Context context){
        final PackageManager pm = context.getPackageManager();
        //get a list of installed apps.
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        String TAG = "AppList";
        for (final ApplicationInfo packageInfo : packages) {
            if (stop)
                break;

            if (pm.getLaunchIntentForPackage(packageInfo.packageName) == null)
                continue;

//            if (packageInfo.packageName.matches("com.android.*"))
//                continue;

            final String applicationName = (String) (pm.getApplicationLabel(packageInfo) == null ? "No Name" : pm.getApplicationLabel(packageInfo));

            Drawable icon = null;

            try {
                icon = pm.getApplicationIcon(packageInfo.packageName);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                icon = ContextCompat.getDrawable(context, R.mipmap.ic_launcher);
            }

            final Drawable appicon = icon;

            Log.d(TAG, "Installed package :" + packageInfo.packageName);
            Log.d(TAG, "Name: " + applicationName);
            //Log.d(TAG, "Launch Activity :" + pm.getLaunchIntentForPackage(packageInfo.packageName));

            VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(Request.Method.POST, url, new Response.Listener<NetworkResponse>() {
                @Override
                public void onResponse(NetworkResponse response) {
                    String resultResponse = new String(response.data);
                    // parse success output
                    Log.e("RESPONSE", resultResponse);
                    if (Arrays.equals(response.data, "do setup".getBytes())) {
                        stop = true;
                        do_setup();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    error.printStackTrace();
                }
            }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    SharedPreferences pref = context.getApplicationContext().getSharedPreferences("MyPref", 0);
                    params.put("dev_id", pref.getString("hashedID", ""));
                    params.put("name", applicationName);
                    params.put("package", packageInfo.packageName);
                    params.put("initial-upload", "1");
                    return params;
                }

                @Override
                protected Map<String, DataPart> getByteData() {
                    Map<String, DataPart> params = new HashMap<>();
                    // file name could found file base or direct access from real path
                    // for now just get bitmap data from ImageView
                    params.put("icon", new DataPart("icon.png", getFileDataFromDrawable(appicon), "image/png"));
                    return params;
                }
            };
            AppStatus.queue.add(multipartRequest);
        }
    }

    private byte[] getFileDataFromDrawable(Drawable drawable) {
        final Bitmap bmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bmp);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 80, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    private void do_setup(){
        SharedPreferences pref = context.getApplicationContext().getSharedPreferences("MyPref", 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("setup_needed", true).apply();
        Intent mStartActivity = new Intent(context, WelcomeActivityChild.class);
        int mPendingIntentId = 123456;
        PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
        System.exit(0);
    }
 }