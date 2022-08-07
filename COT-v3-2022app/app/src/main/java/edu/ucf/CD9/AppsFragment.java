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

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AppsFragment extends Fragment {
    ArrayList<String[]> tableItems=new ArrayList<String[]>();
    RecyclerView recyclerView;
    RecyclerView.Adapter adapter;
    RecyclerView.LayoutManager recyclerViewLayoutManager;
    ProgressBar pb;

    private OnFragmentInteractionListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(!isAdded())
            return null;
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_apps, container, false);

        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);

        // Passing the column number 1 to show online one column in each row.
        recyclerViewLayoutManager = new NpaGridLayoutManager(getActivity(), 1);

        recyclerView.setLayoutManager(recyclerViewLayoutManager);

        adapter = new AppsAdapter(getActivity(), tableItems);

        recyclerView.setAdapter(adapter);

        SharedPreferences prefs = getContext().getApplicationContext().getSharedPreferences("MyPref", 0);
        TextView txtteenName = view.findViewById(R.id.txtTeenName);
        if(prefs.contains("teen-name")){
            txtteenName.setText("Apps installed on "+prefs.getString("teen-name", "teen") + "'s device.");
        }
        else
            txtteenName.setVisibility(View.GONE);

        try {
            pb = getActivity().findViewById(R.id.progressBar);
        }catch (NullPointerException e){
            //ignore
        }

        displayApps();

        return view;
    }

    void displayApps(){
        //Fetch data
        if(!isAdded())
            return;

        tableItems.clear();

        SharedPreferences prefs = getContext().getApplicationContext().getSharedPreferences("MyPref", 0);

//        if(prefs.contains("teen-name")){
//            txtteenName.setText(prefs.getString("teen-name", "teen") + "'s Texting Activity");
//        }
//        else
//            txtteenName.setVisibility(View.GONE);

        String url = AppStatus.serverURL+"/retrieve-apps.php";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //Log.e("response", response);

                        try {
                            JSONArray apps = new JSONArray(response);

                            for (int i = 0; i < apps.length(); i++) {
                                JSONObject c = apps.getJSONObject(i);

                                String appname = c.getString("name");
                                String apppackage = c.getString("package");
                                String appicon = c.getString("icon");
                                String status = c.getString("status");
                                String time = c.getString("time");

                                Log.i("json data", "name: "+appname + ", package: "+apppackage + ", status: "+status + ", time: " +time);

                                byte[] apppic = null;
                                apppic = Base64.decode(appicon, Base64.DEFAULT);
                                appicon = new String(apppic);

                                String[] appinfo =  {appname, apppackage, appicon, status, time};

                                tableItems.add(appinfo);
                                adapter.notifyDataSetChanged();
                                if(pb!=null)
                                    pb.setVisibility(View.GONE);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //TODO backoff an retry
                error.printStackTrace();
            }
        }) {
            //adding parameters to the request
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                if(AppStatus.hashedID==null)
                    params.put("auth_token", AppStatus.account.getIdToken());
                else
                    params.put("dev_id", AppStatus.hashedID);
                return params;
            }
        };
        // Add the request to the RequestQueue.
        AppStatus.queue.add(stringRequest);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(String data) {
        if (mListener != null) {
            mListener.onFragmentInteraction(data);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(String data);
    }

    private BroadcastReceiver refreshTextCount= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(isAdded()){
                displayApps();
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
                    new IntentFilter("refresh-apps"));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(isAdded())
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(refreshTextCount);
    }


}
