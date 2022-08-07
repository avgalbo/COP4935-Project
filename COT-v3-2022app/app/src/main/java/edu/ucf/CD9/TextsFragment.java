package edu.ucf.CD9;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class TextsFragment extends Fragment {

    private RecyclerView mMessageRecycler;
    private MessageListAdapter mMessageAdapter;
    ArrayList<MessageData> messageList;
    ProgressBar pb;
    private String number, name;
    Fragment fragment;
    static Bitmap profilePic;
    private boolean fetchPic;
    LinearLayoutManager llm;
    boolean analysis;

    public static TextsFragment newInstance(String number, String name, byte[] profilePic, boolean fetchPic, boolean analysis) {
        TextsFragment fragment = new TextsFragment();
        // arguments
        Bundle arguments = new Bundle();
        arguments.putString("number", number);
        arguments.putString("name", name);
        arguments.putByteArray("pic", profilePic);
        arguments.putBoolean("fetchPic", fetchPic);
        arguments.putBoolean("analysis", analysis);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        number = getArguments().getString("number", null);
        name = getArguments().getString("name", null);
        fetchPic = getArguments().getBoolean("fetchPic", false);
        byte[] pic = getArguments().getByteArray("pic");
        analysis = getArguments().getBoolean("analysis", false);
        if (pic == null) {
            profilePic = null;
        } else {
            profilePic = BitmapFactory.decodeByteArray(pic, 0, pic.length);
        }
        fragment = this;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_texts, container, false);

        if (number == null)
            return view;

        messageList = new ArrayList<>();
        mMessageRecycler = view.findViewById(R.id.reyclerview_message_list);
        mMessageAdapter = new MessageListAdapter(getContext(), messageList);
        llm = new LinearLayoutManager(view.getContext());
        //llm.setStackFromEnd(true);
        //llm.setReverseLayout(true);
        mMessageRecycler.setLayoutManager(llm);
        mMessageRecycler.setAdapter(mMessageAdapter);

        try {
            if (name == null)
                getActivity().setTitle("" + number);
            else
                getActivity().setTitle(name);

            pb = view.findViewById(R.id.textLoadProgressBar);
        } catch (NullPointerException e) {
            //ignore, no problem
        }
        //displayTexts();
        return view;
    }

    void displayTexts() {
        RequestQueue queue = Volley.newRequestQueue(getActivity());
        String url;
        messageList.clear();
        if(analysis)
            url = AppStatus.serverURL+"/retrieve-texts-analysis.php";
        else
            url = AppStatus.serverURL+"/retrieve-texts.php";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(final String response) {
                        // Display the response string.
                        Log.d("response", response);
                        new AsyncTask<Void, Void, Void>() {
                            boolean wait = false;

                            @Override
                            protected Void doInBackground( final Void ... params ) {
                                if(response.equals("failed")){
                                    //attempt silent login
                                    //silent login
                                    wait = true;
                                    GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                            .requestEmail()
                                            .requestProfile()
                                            .requestId()
                                            .requestIdToken(getString(R.string.server_client_id))
                                            .build();

                                    final GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(getActivity().getApplicationContext(), gso);

                                    mGoogleSignInClient.silentSignIn()
                                            .addOnCompleteListener(getActivity(), new OnCompleteListener<GoogleSignInAccount>() {
                                                @Override
                                                public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                                                    try {
                                                        AppStatus.account = task.getResult(ApiException.class);
                                                    } catch (ApiException e) {
                                                        e.printStackTrace();
                                                    }
                                                    if(AppStatus.account == null){
                                                        getActivity().finish();
                                                    }else {
                                                        wait = false;
                                                    }

                                                }
                                            });
                                }
                                while (wait){
                                    try {
                                        Thread.sleep(10);
                                    } catch (InterruptedException e) {
                                        //ignore
                                    }
                                }
                                try {
                                    JSONArray texts = new JSONArray(response);
                                    int i = 0;

                                    if(fetchPic){
                                        JSONObject picJSON = texts.getJSONObject(0);
                                        if(picJSON.has("contact_pic")){
                                            String picstr = picJSON.getString("contact_pic");
                                            if(!picstr.equals("null")){
                                                byte[] pic  = Base64.decode(picstr, Base64.DEFAULT);
                                                profilePic = BitmapFactory.decodeByteArray(pic, 0, pic.length);
                                            } else{
                                                profilePic = null;
                                            }
                                            i = 1;
                                        }
                                    }

                                    for (; i < texts.length(); i++) {
                                        JSONObject c = texts.getJSONObject(i);

                                        String name = c.getString("name");
                                        String number = c.getString("number");
                                        String time = c.getString("time");
                                        String sent = c.getString("sent");
                                        String mime = c.getString("mime");

                                        String textSentiments = "";
                                        String text = "";
                                        byte[] mmsdata = null;
                                        String imgModeration = null;
                                        String textFlagged = "";
                                        String mediaLabels = "";
                                        boolean showFlag = c.getBoolean("flag");

                                        if(analysis){
                                            textSentiments = c.getString("text-sentiments");
                                            imgModeration = c.getString("img-moderation");
                                            textFlagged = c.getString("text-flagged");
                                            mediaLabels = c.getString("media-labels");

                                        }
                                        if(!analysis || showFlag){
                                            text = c.getString("text");
                                            String mmsmedia = c.getString("media");

                                            //decode base64 text and media
                                            byte[] data = Base64.decode(text, Base64.DEFAULT);
                                            try {
                                                text = new String(data, "UTF-8");
                                            } catch (UnsupportedEncodingException e) {
                                                e.printStackTrace();
                                            }

                                            mmsdata = Base64.decode(mmsmedia, Base64.DEFAULT);
                                        }

                                        MessageData mdata = new MessageData(analysis, name, number, Long.parseLong(time),
                                                text, textSentiments, Boolean.parseBoolean(sent.equals("1")?"true":"false"),
                                                mmsdata, mime, imgModeration, mediaLabels, Boolean.parseBoolean(textFlagged.equals("1")?"true":"false"), showFlag);
                                        //Log.d("mdata", mdata.getName() + ", " + mdata.getNumber()+ ", " +mdata.getTime()+ ", " +mdata.getMessage()+ ", " +mdata.isSent()+ ", " +mdata.getMimetype() );

                                        messageList.add(mdata);


                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mMessageAdapter.notifyDataSetChanged();
                                        mMessageRecycler.scrollToPosition(messageList.size()-1);
                                    }
                                });
                                try {
                                    Thread.sleep(200);
                                } catch (InterruptedException e) {
                                    //ignore
                                }
                                //mMessageRecycler.smoothScrollToPosition(0);

                                return null;
                            }

                            @Override
                            protected void onPostExecute( final Void result ) {
                                if(isAdded() && pb!=null && AppStatus.isAnalysisActivityForeground())
                                    pb.setVisibility(View.GONE);

                                //llm.setStackFromEnd(true);
                                if(isAdded() && mMessageRecycler != null && AppStatus.isAnalysisActivityForeground())
                                    mMessageRecycler.smoothScrollToPosition(messageList.size()-1);
                            }
                        }.execute();

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(isAdded() && AppStatus.isAnalysisActivityForeground())
                    Toast.makeText(getContext(), "An error occurred", Toast.LENGTH_LONG ).show();
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
                params.put("number", number);
                if(fetchPic)
                    params.put("send_pic", "1");
                return params;
            }
        };
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private BroadcastReceiver refreshTextCount = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (isAdded()) {
                displayTexts();
                //Toast.makeText(getContext(), "Broadcast recd", Toast.LENGTH_SHORT).show();
                //Snackbar.make(getView(), intent.getStringExtra("body"), Snackbar.LENGTH_LONG).show();
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded()) {
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(refreshTextCount,
                    new IntentFilter("refresh-text-counts"));
        }
        //Toast.makeText(getContext(), "Resume", Toast.LENGTH_SHORT).show();
        displayTexts();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isAdded())
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(refreshTextCount);
    }
}