package edu.ucf.CD9;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Html;
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
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import org.w3c.dom.Text;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;


public class WelcomeActivityParent extends AppCompatActivity {

    private ViewPager viewPager;
    private LinearLayout dotsLayout;
    private int[] layouts;
    private Button btnNext;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    int pairingCode;
    boolean nameSaved;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pref = getApplicationContext().getSharedPreferences("MyPref", 0);
        editor = pref.edit();

        AppStatus.account = null;

        if(!pref.getString("ParentOrTeen", "").equals("Parent")){

            startActivity(new Intent(WelcomeActivityParent.this,  ParentOrChildActivity.class));
        }

        // Making notification bar transparent
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        setContentView(R.layout.activity_welcome);

        viewPager = (ViewPager) findViewById(R.id.view_pager);
        dotsLayout = (LinearLayout) findViewById(R.id.layoutDots);
        btnNext = (Button) findViewById(R.id.btn_next);


        // layouts of all welcome sliders
        layouts = new int[]{
                R.layout.slide_screen1_parent,
                R.layout.slide_screen2_parent,
                R.layout.slide_screen3_parent,
                R.layout.slide_screen4_parent};

        // adding bottom dots
        addBottomDots(0);

        // making notification bar transparent
        changeStatusBarColor();

        MyViewPagerAdapter myViewPagerAdapter = new MyViewPagerAdapter();
        viewPager.setAdapter(myViewPagerAdapter);
        viewPager.addOnPageChangeListener(viewPagerPageChangeListener);

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // checking for last page
                // if last page home screen will be launched
                int current = viewPager.getCurrentItem();
                // request child app
                if(current == 2){
                    EditText etName = findViewById(R.id.etName);
                    final String name = etName.getText().toString();

                    if(name.length()<1){
                        etName.setError("Required");
                        return;
                    }
                    else{

                        RequestQueue queue = Volley.newRequestQueue(WelcomeActivityParent.this);
                        String url = AppStatus.serverURL+"/otp-name.php";
                        // Request a string response from the provided URL.
                        if(AppStatus.account == null){
                            viewPager.setCurrentItem(1);
                            return;
                        }
                        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                                new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        if(response.equals("auth error")){
                                            Toast.makeText(getApplicationContext(), "Authentication Error. Try again.", Toast.LENGTH_SHORT).show();
                                            signOut();
                                            if(AppStatus.isWelcomeActivityForeground())
                                                viewPager.setCurrentItem(1);
                                        }
                                        if(response.equals("success")){
                                            if(AppStatus.isWelcomeActivityForeground())
                                                nameSaved=true;
                                                viewPager.setCurrentItem(3);
                                                editor.putString("teen-name", name).apply();
                                        }
                                    }
                                }, new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        if(AppStatus.isWelcomeActivityForeground())
                                            viewPager.setCurrentItem(2);
                                        Toast.makeText(getApplicationContext(), "Network error, try again.", Toast.LENGTH_SHORT).show();
                                }
                        }) {
                            //adding parameters to the request
                            @Override
                            protected Map<String, String> getParams() throws AuthFailureError {
                                Map<String, String> params = new HashMap<>();
                                params.put("auth_token", AppStatus.account.getIdToken());
                                params.put("name", name);
                                return params;
                            }
                        };
                        // Add the request to the RequestQueue.
                        queue.add(stringRequest);
                    }
                    return;
                }
                if (current+1 < layouts.length) {
                    // move to next screen
                    viewPager.setCurrentItem(current+1);
                } else {
                    launchHomeScreen();
                }
            }
        });

        if(getIntent().getBooleanExtra("showCode", false)){
            //show pending code
            viewPager.setCurrentItem(3);
        }

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

    private void launchHomeScreen() {
        editor.putBoolean("setup_needed", false).apply();
        startActivity(new Intent(WelcomeActivityParent.this, MainActivity.class));
        finish();
    }

    private void launchInitialTrustSelection(){

        startActivity(new Intent(WelcomeActivityParent.this, InitialSelectTrust.class));
        finish();

    }

    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(this).registerReceiver(onOtpPaired,
                new IntentFilter("otp-pairing-success"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onOtpPaired);
    }

    private BroadcastReceiver onOtpPaired= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // intent can contain anydata
            if(intent.getAction().equals("otp-pairing-success")){
                Toast.makeText(context, "Pairing Successful!", Toast.LENGTH_SHORT).show();
                //launchHomeScreen();
                launchInitialTrustSelection();
            }
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == 53943) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {

        try {
            // Signed in successfully
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            AppStatus.account = account;
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            Log.w("GoogleSignIn", "signInResult:failed code=" + e.getStatusCode());

            return;
        }

        final String idToken = AppStatus.account.getIdToken();

        if(idToken==null) {
            new WaitForFCMToken(WelcomeActivityParent.this).execute();
        }else {
            //Toast.makeText(getApplicationContext(), idToken, Toast.LENGTH_SHORT).show();
            uploadUserInfo(idToken);
        }
    }

    private void uploadUserInfo(final String idToken){
        //            editor.putString("accountName", AppStatus.account.getDisplayName()).apply();
        RequestQueue queue = Volley.newRequestQueue(WelcomeActivityParent.this);
        String url = AppStatus.serverURL+"/gtoken-signin.php";

        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(new OnCompleteListener<String>() {
                @Override
                public void onComplete(@NonNull Task<String> task) {
                    if (!task.isSuccessful()) {
                        Log.w("DEBUG", "Fetching FCM registration token failed", task.getException());
                        uploadUserInfo(idToken);
                        return;
                    }
                    String fcmString = task.getResult();

                    // Request a string response from the provided URL.
                    StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    if(response.equals("failed")){
                                        Toast.makeText(getApplicationContext(), "Sign-in Failed. Please login again.", Toast.LENGTH_SHORT).show();
                                        signOut();
                                        return;
                                    }
                                    if(response.contains("already_setup")) {
                                        //user reinstalled app or cleared app data
                                        Toast.makeText(getApplicationContext(), "Welcome Back!", Toast.LENGTH_SHORT).show();
                                        launchHomeScreen();
                                        String teenName = response.substring(response.indexOf("|") + 1);
                                        editor.putString("teen-name", teenName).apply();
                                        return;
                                    }
                                    try{
                                        // perform button click once a response is received from server
                                        btnNext.performClick();

                                        //previously left setup at otp screen, show otp
                                        pairingCode = Integer.parseInt(response);
                                        TextView pairCode = (TextView) findViewById(R.id.flamingo);
                                        pairCode.setText(""+pairingCode);
                                    } catch (NumberFormatException e){
                                        Toast.makeText(getApplicationContext(), "An error occurred. Please login again.", Toast.LENGTH_SHORT).show();
                                        signOut();
                                    }

                                }
                            }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e("DEBUG", "in Error");
                            error.printStackTrace();
                            Toast.makeText(getApplicationContext(), "Network error. Try again...", Toast.LENGTH_SHORT).show();
                        }
                    }) {
                        //adding parameters to the request
                        @Override
                        protected Map<String, String> getParams() throws AuthFailureError {
                            Map<String, String> params = new HashMap<>();
                            params.put("auth_token", idToken);
                            params.put("fcm_token", fcmString);
                            params.put("setup", "1");
                            return params;
                        }
                    };
                    // Add the request to the RequestQueue.
                    queue.add(stringRequest);

                    Log.e("DEBUG", "Outside Listener");


                    //btnNext.performClick();

                    //**********************************
                }
            } );
    }

    private void signOut() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .requestId()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        final GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        mGoogleSignInClient.signOut();
//                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
//                    @Override
//                    public void onComplete(@NonNull Task<Void> task) {
//                        Intent i = new Intent(getApplicationContext(), Login_Activity.class);
//                        startActivityForResult(i, 10303);                    }
//                });
        editor.remove("setup_needed")
               .apply();

    }

    //  viewpager change listener
    ViewPager.OnPageChangeListener viewPagerPageChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageSelected(int position) {
            // changing the next button text 'NEXT' / 'GOT IT'
            if (position == layouts.length - 1) {
                // last page. make button text to GOT IT
                btnNext.setVisibility(View.GONE);
            } else if(position == layouts.length - 3 && AppStatus.account == null){
                   btnNext.setVisibility(View.GONE);
            }else {
                // still pages are left
                btnNext.setVisibility(View.VISIBLE);
                btnNext.setText(getString(R.string.next));
            }

            if(position==1){
                //btnNext.setVisibility(View.GONE);
                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.server_client_id))
                        .requestEmail()
                        .build();

                final GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(WelcomeActivityParent.this, gso);

                findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                        startActivityForResult(signInIntent, 53943);
                    }
                });
            }

            //prevent skipping of critical steps
            if(position > 1 && AppStatus.account==null){
                viewPager.setCurrentItem(1);
                position = 1;
            }
            else if(getIntent().getBooleanExtra("showCode", false)){
                viewPager.setCurrentItem(3);
            }

            if(position==3){
                if(!nameSaved)
                    viewPager.setCurrentItem(2);
            }

            addBottomDots(position);
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {

        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if(state==ViewPager.SCROLL_STATE_IDLE){
                if(viewPager.getCurrentItem()==3){
                    TextView txtCode = findViewById(R.id.flamingo);
                    txtCode.setText(""+pairingCode);
                }
            }

        }
    };
    /**
     * Making notification bar transparent
     */
    private void changeStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    @Override
    public void onBackPressed() {
        if(AppStatus.account == null){
            startActivity(new Intent(WelcomeActivityParent.this, ParentOrChildActivity.class));
            finish();
        }
    }

    /**
     * View pager adapter
     */
    public class MyViewPagerAdapter extends PagerAdapter {
        private LayoutInflater layoutInflater;

        MyViewPagerAdapter() {
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View view = layoutInflater.inflate(layouts[position], container, false);
            container.addView(view);

            return view;
        }

        @Override
        public int getCount() {
            return layouts.length;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object obj) {
            return view == obj;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            View view = (View) object;
            container.removeView(view);
        }
    }

    private class WaitForFCMToken extends AsyncTask<Void, Void, String> {
        private WeakReference<WelcomeActivityParent> activityReference;

        // only retain a weak reference to the activity
        private WaitForFCMToken(WelcomeActivityParent context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected String doInBackground(Void... voids) {
            String idToken = AppStatus.account.getIdToken();
            while(idToken==null || idToken.equals("")){
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    //ignore
                }
                idToken = AppStatus.account.getIdToken();
            }
            return idToken;
        }

        @Override
        protected void onPostExecute(String idToken) {
            uploadUserInfo(idToken);
        }
    }

}
