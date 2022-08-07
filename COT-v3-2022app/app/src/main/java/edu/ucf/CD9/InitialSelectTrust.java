package edu.ucf.CD9;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.snackbar.Snackbar;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberMatch;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class InitialSelectTrust extends AppCompatActivity
        implements AppsFragment.OnFragmentInteractionListener{
    RecyclerView initialcontacttrust;
    ArrayList<String> numbers = new ArrayList<>();
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    ProgressBar pb;
    RequestQueue queue;
    RecyclerView recyclerView;
    RecyclerView.Adapter adapter;
    RecyclerView.LayoutManager recyclerViewLayoutManager;
    EditText input;
    ImageView enter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pref = getApplicationContext().getSharedPreferences("MyPref", 0);
        editor = pref.edit();
        setContentView(R.layout.activity_main2);
        // hide the toolbar since we are reusing the main activity xml
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().hide();

        initiate(true);
    }

    void initiate(boolean isParent){
        InitialContactFragment contactsFragment = new InitialContactFragment(this);
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.rlContentMain, contactsFragment, contactsFragment.getTag()).commit();
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent("refresh-text-counts"));
    }

    void addTrusted(){
        InitialContactTrustedFragment contactsFragment = new InitialContactTrustedFragment(this);
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.rlContentMain, contactsFragment, contactsFragment.getTag()).commit();
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent("refresh-text-counts"));
    }

    public ArrayList<String> getListNumbers(){
        return numbers;
    }

    public boolean addNumber(String number){
        if(!numbers.contains(number)) {
            numbers.add(number);
            return true;
            //Toast.makeText(getApplicationContext(), "added" + number, Toast.LENGTH_SHORT).show();
        }else{
            numbers.remove(number);
            return false;
            //Toast.makeText(getApplicationContext(), "removed" + number, Toast.LENGTH_SHORT).show();
        }
    }

    private void launchHomeScreen() {
        editor.putBoolean("setup_needed", false).apply();
        startActivity(new Intent(InitialSelectTrust.this, MainActivity.class));
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(this).registerReceiver(initialCotComplete,
                new IntentFilter("initial-cot-success"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(initialCotComplete);
    }

    private BroadcastReceiver initialCotComplete= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // intent can contain anydata
            if(intent.getAction().equals("initial-cot-success")){
                launchHomeScreen();
            }
        }
    };

    @Override
    public void onFragmentInteraction(String data){
        //required but we don't need it
    }

    @Override
    public void onBackPressed() {
        // do nothing
    }

}










/*
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        queue = Volley.newRequestQueue(InitialSelectTrust.this);
        setContentView(R.layout.contact_list_initialtrust);


        initialcontacttrust = findViewById(R.id.contactsuggestedinitial);
        input = findViewById(R.id.input);
        enter = findViewById(R.id.add);












    /////////////////////////////////////////////////////////

        try{
            pb = this.findViewById(R.id.progressBar);
        }catch (NullPointerException e){
            //ignore
        }

        String url = AppStatus.serverURL+"/retrieve-texts.php";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if(response==null)
                            return;

                        Log.e("response", "server sent: "+response);
                        if(response.equals("failed")){
                            //MainActivity ma2 = (MainActivity) getActivity();
                            //ma2.signin();
                            return;
                        }
                        if(response.equals("no data")){
                            // 9/27/2021 made separate responses for child and parent
                            if(AppStatus.account == null)
                                Toast.makeText(getApplicationContext(), "Failed to retrieve data. Try again later...", Toast.LENGTH_LONG).show();
                            else
                                Toast.makeText(getApplicationContext(), "No texting activity has been detected yet on your teen's device. You will be notified when texting occurs.", Toast.LENGTH_LONG).show();
                            return;
                        }
                        try{
                            int code = Integer.parseInt(response);
                            //got pending pairing code, show setup
                            SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
                            SharedPreferences.Editor editor = pref.edit();
                            editor.putBoolean("setup_needed", true).apply();
                            startActivity(new Intent(getApplicationContext(), WelcomeActivityParent.class).putExtra("showCode", true));
                            //if(isAdded()) getActivity().finish();
                            return;
                        } catch (NumberFormatException e){
                            //all good
                        }

                        try{
                            String[] texts = response.split("\\r\\n");
                            boolean dev_admin_revoked = false;
                            if(texts[0].equals("DEV_ADMIN_DISABLED")){
                                //device admin is disabled on teen device, notify parent
                                //txtError.setText("Check "+prefs.getString("teen-name", "teen")+"'s device and make sure Child Monitoring App is installed and configured properly.");
                                //txtError.setVisibility(View.VISIBLE);
                                dev_admin_revoked = true;
                            }
                            for (String text: texts){
                                if(dev_admin_revoked){
                                    dev_admin_revoked = false;
                                    continue;
                                }
                                String[] vals = text.split("\n");
                                byte[] pic = null;
                                if(!vals[2].equals("null"))
                                    pic = Base64.decode(vals[2], Base64.DEFAULT);
                                SummaryData data = new SummaryData(vals[0], vals[1], pic, Integer.parseInt(vals[3]),
                                        Integer.parseInt(vals[4]), Long.parseLong(vals[5]), Integer.parseInt(vals[6]),
                                        Integer.parseInt(vals[7]), Integer.parseInt(vals[8]), Integer.parseInt(vals[9]));
                                conversationsUnits.add(data);
                                adapter.notifyDataSetChanged();
                            }
                        } catch (Exception e){
                            e.printStackTrace();
                            Toast.makeText(getApplicationContext(), "Failed to retrieve data. Try again later...", Toast.LENGTH_LONG).show();
                        }
                        if(pb!=null)
                            pb.setVisibility(View.GONE);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //TODO backoff an retry
                error.printStackTrace();
                //if(isAdded() && AppStatus.isMainActivityForeground())
                //    Toast.makeText(getContext(), "Failed to retrieve data. Try again later...", Toast.LENGTH_LONG).show();
            }
        }) {
            //adding parameters to the request
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                //Log.e("hashed ID is",AppStatus.hashedID);
                if(AppStatus.hashedID==null)
                    params.put("auth_token", AppStatus.account.getIdToken());
                else
                    params.put("dev_id", AppStatus.hashedID);
                params.put("counts", "1");
                return params;
            }
        };
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    /////////////////////////////////////////////////////////


        initialcontacttrust.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int i, long l) {
                String contact = contacts.get(i);
                makeToast(contact);
            }
        });

        adapter = new RecyclerView.Adapter(getApplicationContext(), android.R.layout.simple_list_item_1, conversationsUnits);
        initialcontacttrust.setAdapter(adapter);

        enter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = input.getText().toString();
                if (text == null || text.length() == 0) {
                    makeToast("Enter your Bullshit");
                } else {
                    addItem(text);
                    input.setText("");
                    makeToast("Added: " + text);
                }
            }
        });
    }

    public void addItem(String item){
        contacts.add(item);
        initialcontacttrust.setAdapter(adapter);
    }

    Toast t;

    private void makeToast(String s) {
        if (t != null) t.cancel();
        t = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
        t.show();

    }

}*/
