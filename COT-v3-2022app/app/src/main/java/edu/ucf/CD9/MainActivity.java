package edu.ucf.CD9;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.NonNull;
import com.google.android.material.navigation.NavigationView;
import android.graphics.Color;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberMatch;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.security.MessageDigest;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements AppsFragment.OnFragmentInteractionListener, NavigationView.OnNavigationItemSelectedListener {
    long foreground_time = 0;
    String analytics_url = AppStatus.serverURL+"/analytics.php";
    String deviceHash;
    ActionBarDrawerToggle mDrawerToggle;
    private boolean mToolBarNavigationListenerIsRegistered = false;

    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);
        if(AppStatus.queue==null)
            AppStatus.queue = Volley.newRequestQueue(getApplicationContext());

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();
        showBackButton(false);
        setTitle("Text Monitoring");

        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        if(pref.getString("ParentOrTeen", "").equals("Parent")) {
            signin();
        }
        else if (pref.getString("ParentOrTeen", "").equals("Child")) {
            String device_id = getDeviceID();
            if(device_id == null)
                return;
            AppStatus.hashedID = getSHA512Hash(device_id, "TeenDevice");

            initiate(false);
        }

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setCheckedItem(R.id.nav_texts);
    }

    void showBackButton(boolean show) {

        if(show) {
            // Remove hamburger
            mDrawerToggle.setDrawerIndicatorEnabled(false);
            // Show back button
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            if(!mToolBarNavigationListenerIsRegistered) {
                mDrawerToggle.setToolbarNavigationClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Doesn't have to be onBackPressed
                        onBackPressed();
                    }
                });

                mToolBarNavigationListenerIsRegistered = true;
            }

        } else {
            // Remove back button
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            // Show hamburger
            mDrawerToggle.setDrawerIndicatorEnabled(true);
            mDrawerToggle.getDrawerArrowDrawable().setColor(getResources().getColor(R.color.white));
            // Remove the/any drawer toggle listener
            mDrawerToggle.setToolbarNavigationClickListener(null);
            mToolBarNavigationListenerIsRegistered = false;
        }

    }

    public void signin(){
        //silent login
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.server_client_id))
                .requestEmail()
                .build();


        mGoogleSignInClient = GoogleSignIn.getClient(MainActivity.this, gso);

        mGoogleSignInClient.silentSignIn()
                .addOnCompleteListener(this, new OnCompleteListener<GoogleSignInAccount>() {
                    @Override
                    public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                        // check if account is null, jic
                        if(AppStatus.account == null){
                            try {
                                AppStatus.account = task.getResult(ApiException.class);
                            } catch (ApiException e) {
                                signOut();
                                e.printStackTrace();
                            }
                        }
                        // check if account is null after trying to receive account
                        if(AppStatus.account == null){

                            //initiate fresh signin
                            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                            startActivityForResult(signInIntent, 53943);
                        } // continue if account is signed in
                        else {
                            initiate(true);
                        }
                    }
                });
    }

    void initiate(boolean isParent){
        final NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View header=navigationView.getHeaderView(0);
        TextView name = header.findViewById(R.id.txtUsername);
        TextView email = header.findViewById(R.id.txtUserEmail);
        LinearLayout buttonContainer = header.findViewById(R.id.buttonCon);
        LinearLayout buttonContainer2 = header.findViewById(R.id.buttonCon2);
        Button settingsbtn = header.findViewById(R.id.button3);
        final ImageView profilPic = header.findViewById(R.id.imageViewUserProfilePic);

        settingsbtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                settings();
            }

        });

        if(isParent){
            // Show parent account information and a Sign-out button
            name.setText(AppStatus.account.getDisplayName());
            email.setText(AppStatus.account.getEmail());
            buttonContainer.setVisibility(View.VISIBLE);

            //buttonContainer2.setVisibility(View.VISIBLE);
            buttonContainer2.setVisibility(View.GONE);

            ImageRequest imgrequest = new ImageRequest(
                    AppStatus.account.getPhotoUrl().toString(), new Response.Listener<Bitmap>() {
                @Override
                public void onResponse(Bitmap bitmap) {
                    profilPic.setImageBitmap(bitmap);
                }
            }, 0,
                    0, null, Bitmap.Config.RGB_565, new Response.ErrorListener() {
                public void onErrorResponse(VolleyError error) {
                    profilPic.setImageResource(R.mipmap.ic_launcher_round);
                }
            });

            AppStatus.queue.add(imgrequest);

//            buttonContainer2.setOnClickListener(new View.OnClickListener(){
//                @Override
//                public void onClick(View view){
//                    startActivity(new Intent(MainActivity.this, Settings.class));
//                }
//
//        });

        } else{
            // Don't show the sign-out button
            buttonContainer.setVisibility(View.GONE);
            buttonContainer2.setVisibility(View.GONE);
            name.setText("Child App");
            email.setText("");
        }

        TextSummaryFragment textFragment = new TextSummaryFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.rlContentMain, textFragment, textFragment.getTag()).commit();
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent("refresh-text-counts"));

        //if started from notification, navigate to appropriate screen
        Bundle b = getIntent().getExtras();
        String action = "";
        if(b!=null){
            action = b.getString("action", "");
            Log.e("intent action", action);

            if(action.equals("apps")) {
                //android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
                ProgressBar pb = findViewById(R.id.progressBar);
                pb.setVisibility(View.VISIBLE);
                AppsFragment appsFragment = new AppsFragment();
                if (fragmentManager.getBackStackEntryCount() > 0)
                    fragmentManager.popBackStack();
                setTitle("App Monitoring");
                fragmentManager.beginTransaction().replace(R.id.rlContentMain, appsFragment, "AppsScreen").commit();
            }else if(action.equals("text") || action.equals("cot")){
                String contactNumber = b.getString("number", null);
                if(contactNumber == null)
                    return;

                PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                Iterator<PhoneNumberMatch> phoneNo = phoneUtil.findNumbers(contactNumber, "US").iterator();
                Phonenumber.PhoneNumber number = null;
                if(phoneNo.hasNext())
                    number  = phoneNo.next().number();
                else{
                    try {
                        number = phoneUtil.parse(contactNumber, "US");
                    } catch (NumberParseException e){
                        return;
                    }
                }

                final String address = phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);

                String contactName = b.getString("name", "");
                if(contactName.equals(""))
                    contactName=null;
                //String p = b.getString("pic", null);
                //byte[] pic = Base64.decode(p, Base64.DEFAULT);

                final String contact = contactName;
                //showBackButton(true);
//                                    android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
                boolean trustedContact = Boolean.parseBoolean(b.getString("trusted", "false"));

                //TextsFragment textsFragment = TextsFragment.newInstance(address, contact, null, true, trustedContact);
//                textsFragment.setArguments(bundle);
                Intent intent = new Intent(this, AnalysisActivity.class);
                intent.putExtra("number", address);
                intent.putExtra("contact", contact);
                intent.putExtra("fetchPic", true);
                intent.putExtra("cot", trustedContact);
                if(!action.equals("cot")){
                    intent.putExtra("showDetails", true);
                }
                startActivity(intent);
                //fragmentManager.beginTransaction().replace(R.id.rlContentMain, textsFragment, "TextDetails").addToBackStack("TextSummary").commit();
            }
        }
    }

    @Override
    public void onBackPressed() {
        setTitle("Text Monitoring");
        showBackButton(false);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main_activity2, menu);
        return true;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        FragmentManager fragmentManager = getSupportFragmentManager();
        int id = item.getItemId();
        ProgressBar pb = findViewById(R.id.progressBar);
        if (id == R.id.nav_texts) {
            pb.setVisibility(View.VISIBLE);
            TextSummaryFragment textFragment = new TextSummaryFragment();
            setTitle("Text Monitoring");
            fragmentManager.beginTransaction().replace(R.id.rlContentMain, textFragment, "TextSummary").commit();

        } else if (id == R.id.nav_apps) {
            pb.setVisibility(View.VISIBLE);
            AppsFragment appsFragment = new AppsFragment();
            if(fragmentManager.getBackStackEntryCount()>0)
                fragmentManager.popBackStack();
            setTitle("App Monitoring");
            fragmentManager.beginTransaction().replace(R.id.rlContentMain, appsFragment, "AppsScreen").commit();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void signOutUser(View view){

        // Build a GoogleSignInClient with the options specified by gso.
        //final GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        AppStatus.account = null;
                        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putBoolean("setup_needed", true).apply();
                        startActivity(new Intent(MainActivity.this, Welcome.class));
                    }
                });
    }

    public void settings() {
        Intent settingsIntent = new Intent(this, Settings.class);
        startActivity(settingsIntent);
    }

    private void signOut() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .requestId()
                .requestIdToken(getString(R.string.server_client_id))
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                        startActivityForResult(signInIntent, 53943);}
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 53943) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        try {
            AppStatus.account = completedTask.getResult(ApiException.class);
            // CRASHLYTICS
            crashlytics.setUserId(AppStatus.account.getId());

            final String idToken = AppStatus.account.getIdToken();

            String url = AppStatus.serverURL+"/gtoken-signin.php";

            // Request a string response from the provided URL.
            StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            if(response.equals("failed") || !response.equals(AppStatus.account.getId())){
                                Toast.makeText(getApplicationContext(), "Sign-in Failed. Please login again.", Toast.LENGTH_SHORT).show();
                                signOut();
                            }
                            else{
                                if(AppStatus.isMainActivityForeground())
                                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent("refresh-text-counts"));
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(getApplicationContext(), "Network error. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }) {
                //adding parameters to the request
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    Map<String, String> params = new HashMap<>();
                    params.put("auth_token", idToken);
                    params.put("verify", "1");
                    return params;
                }
            };
            // Add the request to the RequestQueue.
            AppStatus.queue.add(stringRequest);

        } catch (ApiException e) {
            Log.w("GoogleSignIn", "signInResult:failed code=" + e.getStatusCode());
            // CRASHLYTICS LOG
        }
    }

    @Override
    public void onFragmentInteraction(String data){
        //required but we don't need it
    }

    @Override
    protected void onPause(){
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0);
        final long used_time = Calendar.getInstance().getTimeInMillis() - foreground_time
                + pref.getLong("pendingtime", 0);
        final int launch = 1 + pref.getInt("pendinglaunch", 0);
        SharedPreferences.Editor editor = pref.edit();
        editor  .putLong("pendingtime", 0)
                .putInt("pendinglaunch", 0)
                .apply();

        if(used_time>0){
            //upload use time to server
            StringRequest stringRequest = new StringRequest(Request.Method.POST, analytics_url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(final String response) {
                            Log.d("response", response);
                            //Toast.makeText(getBaseContext(), "response "+response, Toast.LENGTH_LONG ).show();;
                            if(!response.matches("OK")){
                                saveUseTimeForLater(used_time, launch);
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    saveUseTimeForLater(used_time, launch);
                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    Map<String, String> params = new HashMap<>();
                    if(AppStatus.hashedID==null)
                        params.put("auth_token", AppStatus.account.getIdToken());
                    else
                        params.put("dev_id", AppStatus.hashedID);
                    params.put("add_time", ""+used_time/1000);
                    params.put("launch", ""+launch);
                    return params;
                }
            };
            AppStatus.queue.add(stringRequest);
        }
        super.onPause();
    }

    @Override
    protected void onResume(){
        foreground_time = Calendar.getInstance().getTimeInMillis();
        super.onResume();
    }

    void saveUseTimeForLater(long time, int launch){
        //saves usage time to upload next time
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0);
        SharedPreferences.Editor editor = pref.edit();
        editor  .putLong("pendingtime", time)
                .putInt("pendinglaunch", launch)
                .apply();
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

    // 9/27/2021 Updated how the device ID is retrieved - did the same in child app
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
}