package edu.ucf.CD9;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TextSummaryFragment extends Fragment {

    RequestQueue queue;
    TextView txtteenName, txtError;
    ProgressBar pb;
    RecyclerView recyclerView;
    RecyclerView.Adapter adapter;
    RecyclerView.LayoutManager recyclerViewLayoutManager;
    ArrayList<SummaryData> conversationsUnits = new ArrayList<>();

    public static AppsFragment newInstance() {
        return new AppsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        queue = Volley.newRequestQueue(getActivity());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_text_summary, container, false);

        if(AppStatus.account == null){
            //child
            //removed
            TextView tvSentLegend = view.findViewById(R.id.txtSentLegend);
            TextView tvRecdLegend = view.findViewById(R.id.txtRecdLegend);
            tvRecdLegend.setText(R.string.texts_received_by_you);
            tvSentLegend.setText(R.string.texts_sent_by_you);
        }

        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerview_msg_summary);

        // Passing the column number 1 to show online one column in each row.
        recyclerViewLayoutManager = new NpaGridLayoutManager(getActivity(), 1);

        recyclerView.setLayoutManager(recyclerViewLayoutManager);

        adapter = new ConversationsAdapter(getActivity(), conversationsUnits);

        recyclerView.setAdapter(adapter);

        txtteenName = view.findViewById(R.id.txtTeenName);

        txtError = view.findViewById(R.id.txtError);

        //displayTextCounts();

        try{
            if(isAdded())
                pb = getActivity().findViewById(R.id.progressBar);
        }catch (NullPointerException e){
            //ignore
        }

        return view;
    }

    void displayTextCounts(){
        //Fetch data
        if(!isAdded())
            return;

        conversationsUnits.clear();
        final SharedPreferences prefs = getContext().getApplicationContext().getSharedPreferences("MyPref", 0);

        if(prefs.contains("teen-name")){
            txtteenName.setText(prefs.getString("teen-name", "Child") + "'s Texting Activity");
        }
        else
            txtteenName.setVisibility(View.GONE);

        String url = AppStatus.serverURL+"/retrieve-texts.php";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if(response==null)
                            return;

                        Log.e("response", "server sent: "+response);
                        if(response.equals("failed")){
                            MainActivity ma2 = (MainActivity) getActivity();
                            ma2.signin();
                            return;
                        }
                        if(response.equals("no data")){
                            // 9/27/2021 made separate responses for child and parent
                            if(AppStatus.account == null)
                                Toast.makeText(getContext(), "Failed to retrieve data. Try again later...", Toast.LENGTH_LONG).show();
                            else
                                Toast.makeText(getContext(), "No texting activity has been detected yet on your teen's device. You will be notified when texting occurs.", Toast.LENGTH_LONG).show();

                            return;
                        }
                        try{
                            int code = Integer.parseInt(response);
                            //got pending pairing code, show setup
                            SharedPreferences pref = getContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
                            SharedPreferences.Editor editor = pref.edit();
                            editor.putBoolean("setup_needed", true).apply();
                            startActivity(new Intent(getContext(), WelcomeActivityParent.class).putExtra("showCode", true));
                            if(isAdded()) getActivity().finish();
                            return;
                        } catch (NumberFormatException e){
                            //all good
                        }

                        try{
                            String[] texts = response.split("\\r\\n");
                            boolean dev_admin_revoked = false;
                            if(texts[0].equals("DEV_ADMIN_DISABLED")){
                                //device admin is disabled on teen device, notify parent
                                txtError.setText("Check "+prefs.getString("teen-name", "teen")+"'s device and make sure Child Monitoring App is installed and configured properly.");
                                txtError.setVisibility(View.VISIBLE);
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
                            Toast.makeText(getContext(), "Failed to retrieve data. Try again later...", Toast.LENGTH_LONG).show();
                        }
                        if(pb!=null)
                            pb.setVisibility(View.GONE);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //TODO backoff an retry
                error.printStackTrace();
                if(isAdded() && AppStatus.isMainActivityForeground())
                Toast.makeText(getContext(), "Failed to retrieve data. Try again later...", Toast.LENGTH_LONG).show();
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
    }

    private BroadcastReceiver refreshTextCount= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(isAdded()){
                displayTextCounts();
                //Toast.makeText(getContext(), "Broadcast recd", Toast.LENGTH_SHORT).show();
                Snackbar.make(getView(), intent.getStringExtra("body"), Snackbar.LENGTH_LONG).show();
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        if(isAdded()){
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(refreshTextCount,
                    new IntentFilter("refresh-text-counts"));
        }
        //Toast.makeText(getContext(), "Resume", Toast.LENGTH_SHORT).show();
        displayTextCounts();
    }

    @Override
    public void onPause() {
        super.onPause();
        if(isAdded())
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(refreshTextCount);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            File root = getContext().getExternalFilesDir(null);
            File[] files = root.listFiles();
            if(files != null) {
                int j;
                for(j = 0; j < files.length; j++) {
                    System.out.println(files[j].getPath());
                    System.out.println(files[j].delete());
                }
            }
        }catch (NullPointerException e){
            //ignore
            e.printStackTrace();
        }

    }
}
