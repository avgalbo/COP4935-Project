package edu.ucf.CD9;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.messaging.FirebaseMessaging;

import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;


public class WelcomeActivityChild extends AppCompatActivity {

    private ViewPager viewPager;
    private LinearLayout dotsLayout;
    private int[] layouts;
    private Button btnNext;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    String deviceHash;
    static final int ACTIVATION_REQUEST = 47;
    boolean deviceAdminGranted;
    static String returningTeenCheckUrl = AppStatus.serverURL+"/is-returning-teen.php";
    static String teenPairUrl = AppStatus.serverURL+"/teen-pair.php";
    static String hashedID;
    private boolean uploading;
    // variable to determine whether the back button should be allowed to be pressed
    private boolean paired;

    private final static int REQUEST_PERMISSIONS = 1001;
    RequestPermissionAction onPermissionCallBack;

    private boolean checkReadSMSPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
    }
    private boolean checkReadSMSPermission2() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
    }
    private boolean checkReadSMSPermission3() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }
    private boolean checkPermission4() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    public void getPermissions(RequestPermissionAction onPermissionCallBack) {
        this.onPermissionCallBack = onPermissionCallBack;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(!checkReadSMSPermission() || !checkReadSMSPermission2() || !checkReadSMSPermission3()
                    || !checkPermission4() ){
                requestPermissions(new String[]{Manifest.permission.READ_SMS,
                        Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_CONTACTS,
                        Manifest.permission.READ_PHONE_STATE}, REQUEST_PERMISSIONS);
                return;
            }
        }
        if (onPermissionCallBack != null)
            onPermissionCallBack.permissionGranted();
    }

    public boolean haveAllPermissions(){
        return checkReadSMSPermission() && checkReadSMSPermission2() && checkReadSMSPermission3()
                && checkPermission4();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length==0){
            return;
        }
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (onPermissionCallBack != null)
                onPermissionCallBack.permissionGranted();

        } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
            if (REQUEST_PERMISSIONS == requestCode) {
                final Intent i = new Intent();
                i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                i.addCategory(Intent.CATEGORY_DEFAULT);
                i.setData(Uri.parse("package:" + getPackageName()));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                startActivity(i);
            }
            if (onPermissionCallBack != null)
                onPermissionCallBack.permissionDenied();
        }
    }

    public interface RequestPermissionAction {
        void permissionDenied();
        void permissionGranted();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Checking for first time launch - before calling setContentView()
        if(AppStatus.queue==null)
            AppStatus.queue = Volley.newRequestQueue(getApplicationContext());
        pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        editor = pref.edit();
        if (!pref.getBoolean("setup_needed", true)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

        // Making notification bar transparent
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        paired = false;

        setContentView(R.layout.activity_welcome);

        viewPager = findViewById(R.id.view_pager);
        dotsLayout = findViewById(R.id.layoutDots);
        btnNext = findViewById(R.id.btn_next);

        layouts = new int[]{
                R.layout.slide_screen1_child,
                R.layout.slide_screen2_child,
                R.layout.slide_screen3_child};

        addBottomDots(0);

        // making notification bar transparent
        changeStatusBarColor();

        MyViewPagerAdapter myViewPagerAdapter = new MyViewPagerAdapter();
        viewPager.setAdapter(myViewPagerAdapter);
        viewPager.addOnPageChangeListener(viewPagerPageChangeListener);

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int current = viewPager.getCurrentItem();
                if(current==1){
                    getPermissions(new RequestPermissionAction() {
                        @Override
                        public void permissionDenied() {
                            Toast.makeText(getApplicationContext(),
                                    "All permissions must be granted. Exiting...", Toast.LENGTH_LONG).show();
                            finish();
                        }

                        @Override
                        public void permissionGranted() {
                            //good to go
                            if(haveAllPermissions()){
                                hashedID = getSHA512Hash(getDeviceID(), "TeenDevice");
                                editor.putString("hashedID", hashedID).apply();
                                // Crashlytics.setUserIdentifier(hashedID);
                                viewPager.setCurrentItem(2);
                            }
                        }
                    });
                    return;
                }
                if (current+1 < layouts.length) {
                    // move to next screen
                    viewPager.setCurrentItem(current+1);
                } //else {
                  //  editor.putBoolean("setup_needed", false).apply();
                  //  startActivity(new Intent(WelcomeActivityChild.this,
                  //          MainActivity.class).putExtra("JustSetup", true));
                  //  finish();
                //}
            }
        });

        //enable child monitoring related receivers
        ComponentName component=new ComponentName(this, SmsBroadcastReceiver.class);
        getPackageManager()
                .setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP);
        component=new ComponentName(this, MessageMonitorService.class);
        getPackageManager()
                .setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP);
        component=new ComponentName(this, PackageMonitor.class);
        getPackageManager()
                .setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP);
        component=new ComponentName(this, BootBroadcastReceived.class);
        getPackageManager()
                .setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP);

    }

    private void addBottomDots(int currentPage) {
        TextView[] dots = new TextView[layouts.length];

        int[] colorsActive = getResources().getIntArray(R.array.array_dot_active);
        int[] colorsInactive = getResources().getIntArray(R.array.array_dot_inactive);

        dotsLayout.removeAllViews();
        for (int i = 0; i < dots.length; i++) {
            dots[i] = new TextView(this);
            dots[i].setText(Html.fromHtml("&#8226;"));
            dots[i].setTextSize(35);
            dots[i].setTextColor(colorsInactive[currentPage]);
            dotsLayout.addView(dots[i]);
        }

        if (dots.length > 0)
            dots[currentPage].setTextColor(colorsActive[currentPage]);
    }

    //  viewpager change listener
    ViewPager.OnPageChangeListener viewPagerPageChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageSelected(int position) {
            addBottomDots(position);

            if(uploading){
                viewPager.setCurrentItem(2);
                return;
            }

            if (position == layouts.length - 1) {
                // last page
                btnNext.setVisibility(View.GONE);
            } else {
                // still pages are left
                btnNext.setVisibility(View.VISIBLE);
                btnNext.setText(getString(R.string.next));
            }

            if (position == 2) {
                if(!deviceAdminGranted){
                    //ask for device admin
                    ComponentName demoDeviceAdmin = new ComponentName(getApplicationContext(),
                            MyDeviceAdminReceiver.class);
                    Intent intent = new Intent(
                            DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                            demoDeviceAdmin);
                    intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            getString(R.string.device_admin_requestinfo));
                    startActivityForResult(intent, ACTIVATION_REQUEST);
                }

                if(!haveAllPermissions() || !deviceAdminGranted){
                    viewPager.setCurrentItem(1);
                    return;
                }
            }
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {

        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if(uploading)
                return;

            if(state== ViewPager.SCROLL_STATE_IDLE){
                if(viewPager.getCurrentItem()==2){
                    // Request a string response from the provided URL.
                    StringRequest stringRequest = new StringRequest(Request.Method.POST, returningTeenCheckUrl,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    if(response.contains("yes")){
                                        String last_time = response.substring(response.indexOf("|")+1);
//                                    editor.putString("devID", hashedID).apply();
                                        btnNext.performClick();
                                        LinearLayout llCode = findViewById(R.id.llCode);
                                        llCode.setVisibility(View.GONE);
                                        LinearLayout llWait = findViewById(R.id.llWait);
                                        llWait.setVisibility(View.VISIBLE);
                                        TextView tvProgress = findViewById(R.id.txtSetupProgress);
                                        uploading = true;
                                        editor.putString("devID", hashedID).apply();
                                        getSHA512Hash(getDeviceID(), "TeenDevice");
                                        UploadTextsTask uploadTask = new UploadTextsTask(WelcomeActivityChild.this, tvProgress);
                                        try{
                                            uploadTask.last_time = Long.parseLong(last_time);
                                        } catch (Exception e){
                                            uploadTask.last_time = 0;
                                        }
                                        uploadTask.execute();
                                        launchHomeScreen();
                                    }
                                }
                            }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(getApplicationContext(), "Network error. Try again..",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }) {
                        //adding parameters to the request
                        @Override
                        protected Map<String, String> getParams() throws AuthFailureError {
                            Map<String, String> params = new HashMap<>();
                            params.put("dev_id", hashedID);
                            return params;
                        }
                    };
                    // Add the request to the RequestQueue.
                    AppStatus.queue.add(stringRequest);


                    final EditText etCode = findViewById(R.id.etCode);
                    etCode.addTextChangedListener(new TextValidator(etCode) {
                        @Override
                        public void validate(TextView textView, final String text) {
                            if (text.length() == 6) {
                                etCode.setEnabled(false);

                                FirebaseMessaging.getInstance().getToken()
                                    .addOnCompleteListener(new OnCompleteListener<String>() {
                                        @Override
                                        public void onComplete(@NonNull Task<String> task) {
                                            if (!task.isSuccessful()) {
                                                Log.w("DEBUG", "Fetching FCM registration token failed", task.getException());
                                                return;
                                            }
                                            String fcmString = task.getResult();

                                            // Request a string response from the provided URL.
                                            StringRequest stringRequest = new StringRequest(Request.Method.POST, teenPairUrl,
                                                    new Response.Listener<String>() {
                                                        @Override
                                                        public void onResponse(String response) {
                                                            if(response.equals("failed")){
                                                                Toast.makeText(getApplicationContext(),
                                                                        "Invalid Code. Try again.", Toast.LENGTH_SHORT).show();
                                                                etCode.setEnabled(true);
                                                            }
                                                            else if(response.equals("success")){
                                                                LinearLayout llCode = findViewById(R.id.llCode);
                                                                llCode.setVisibility(View.GONE);
                                                                LinearLayout llWait = findViewById(R.id.llWait);
                                                                llWait.setVisibility(View.VISIBLE);
                                                                TextView tvProgress = findViewById(R.id.txtSetupProgress);
                                                                uploading = false;
                                                                paired = true;
                                                                editor.putString("devID", hashedID).apply();
                                                                getSHA512Hash(getDeviceID(), "TeenDevice");
                                                                new UploadTextsTask(WelcomeActivityChild.this, tvProgress).execute();
                                                            }
                                                        }
                                                    }, new Response.ErrorListener() {
                                                @Override
                                                public void onErrorResponse(VolleyError error) {
                                                    Toast.makeText(getApplicationContext(),
                                                            "Network error. Try again..", Toast.LENGTH_SHORT).show();
                                                    etCode.setEnabled(true);
                                                }
                                            }) {
                                                @Override
                                                protected Map<String, String> getParams() throws AuthFailureError {
                                                    Map<String, String> params = new HashMap<>();
                                                    params.put("otp_entered", text);
                                                    params.put("dev_id", hashedID);
                                                    params.put("fcm_token", fcmString);

                                                    return params;
                                                }
                                            };
                                            // Add the request to the RequestQueue.
                                            AppStatus.queue.add(stringRequest);
                                        }
                                    });
                            }
                        }
                    });
                }
            }
        }
    };


     //Making notification bar transparent
    private void changeStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    //View pager adapter
    public class MyViewPagerAdapter extends PagerAdapter {
        private LayoutInflater layoutInflater;

        MyViewPagerAdapter() {
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View view = null;
            if (layoutInflater != null) {
                view = layoutInflater.inflate(layouts[position], container, false);
                container.addView(view);
            }
            return view;
        }

        @Override
        public int getCount() {
            return layouts.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object obj) {
            return view == obj;
        }


        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            View view = (View) object;
            container.removeView(view);
        }
    }

    public String getSHA512Hash(String stringToHash, String salt){
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
    private String getDeviceID(){
        if(deviceHash!=null)
            return deviceHash;

        String deviceId = null;
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        if (null != tm) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                deviceId = Settings.Secure.getString(
                        getContentResolver(),
                        Settings.Secure.ANDROID_ID);
            } else {
                if (tm.getDeviceId() != null) {
                    deviceId = tm.getDeviceId();
                } else {
                    deviceId = Settings.Secure.getString(
                            getContentResolver(),
                            Settings.Secure.ANDROID_ID);
                }
            }
        }
        if (null == deviceId || 0 == deviceId.length()) {
            deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        deviceHash = deviceId;
        return deviceHash;
    }

    public abstract class TextValidator implements TextWatcher {
        private final TextView textView;

        TextValidator(TextView textView) {
            this.textView = textView;
        }

        public abstract void validate(TextView textView, String text);

        @Override
        final public void afterTextChanged(Editable s) {
            String text = textView.getText().toString();
            validate(textView, text);
        }

        @Override
        final public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* Do nothing */ }

        @Override
        final public void onTextChanged(CharSequence s, int start, int before, int count) { /* Do nothing */ }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ACTIVATION_REQUEST:
                if (resultCode == Activity.RESULT_OK) {
                    Log.i("Device Admin", "Administration enabled!");
                    deviceAdminGranted = true;
                    btnNext.callOnClick();
                } else {
                    Log.i("Device Admin", "Administration enable FAILED!");
                    deviceAdminGranted = false;
                }
                return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if(!paired) {
            startActivity(new Intent(WelcomeActivityChild.this, ParentOrChildActivity.class));
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(this).registerReceiver(onOtpPaired,
                new IntentFilter("otp-pairing-success"));
    }

    private void launchHomeScreen() {
        editor.putBoolean("setup_needed", false).apply();

        Intent intent = new Intent(this, MessageMonitorService.class);
        startService(intent);

        startActivity(new Intent(WelcomeActivityChild.this, MainActivity.class));
        finish();
    }

    private void launchTutorial() {
        startActivity(new Intent(WelcomeActivityChild.this, ChildTutorial.class));
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onOtpPaired);
    }

    private BroadcastReceiver onOtpPaired= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("otp-pairing-success")){
                //Toast.makeText(context, "Pairing Successful!", Toast.LENGTH_SHORT).show();
                launchTutorial();
            }
        }
    };

    private static class UploadTextsTask extends AsyncTask<Void, Void, String> {

        private WeakReference<WelcomeActivityChild> activityReference;
        private WeakReference<TextView> txtProgressReference;
        private long last_time = 0;
        private int count = 0;
        // only retain a weak reference to the activity
        UploadTextsTask(WelcomeActivityChild context, TextView progress) {
            activityReference = new WeakReference<>(context);
            txtProgressReference = new WeakReference<>(progress);
        }

        @Override
        protected String doInBackground(Void... params) {
            Context context = activityReference.get();

            if (context != null) {
                UploadTexts ut = new UploadTexts();
                count = ut.UploadTexts(context, true, last_time, txtProgressReference.get() ,activityReference.get());
                new UploadApps(context);
            }
            return "task finished";
        }

        @Override
        protected void onPostExecute(String result) {
            txtProgressReference.get().setText(R.string.finishing);
            //Toast.makeText(activityReference.get(), "UploadCount: "+count, Toast.LENGTH_LONG).show();
            StringRequest stringRequest = new StringRequest(Request.Method.POST, teenPairUrl,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {

                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {

                        }
            }) {
                //adding parameters to the request
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    Map<String, String> params = new HashMap<>();
                    params.put("dev_id", hashedID);
                    params.put("upload_done", "1");
                    params.put("upload_count", ""+count);
                    return params;
                }
            };
            AppStatus.queue.add(stringRequest);
        }
    }
}