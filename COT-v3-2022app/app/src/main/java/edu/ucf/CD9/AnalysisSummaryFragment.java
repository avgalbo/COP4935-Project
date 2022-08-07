package edu.ucf.CD9;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.MPPointF;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.Fragment;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.cardview.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import android.util.Base64;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AnalysisSummaryFragment extends Fragment {
    ArrayList<String[]> tableItems=new ArrayList<String[]>();
    ProgressBar pb;
    TextView tvPositive, tvNegative, tvNeutral, tvMixed, tvFlagged, tvFlaggedTexts, tvFlaggedImgs,
            tvTotalTexts, tvTotalImgs, tvTotalVids, tvCotMsg, tvWordCloudHeader;
    Switch cotSwitch;
    View view;
    CardView cotCV, cotActionCV;
    String name, number;
    Button btnAccept, btnReject;
    String cotStatus;
    ImageView ivWordCloud;
    PieChart pieChart;
    //BarChart barChart;
    static boolean trusted;

    private OnFragmentInteractionListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppStatus.queue = Volley.newRequestQueue(getActivity());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(!isAdded())
            return null;
        Log.e("Check", "SEE if it gets here");
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_conv_summary, container, false);
        view.setVisibility(View.INVISIBLE);
       /* tvPositive = view.findViewById(R.id.txtPositive);
        tvNegative = view.findViewById(R.id.txtNegative);
        tvNeutral = view.findViewById(R.id.txtNeutral);
        tvMixed = view.findViewById(R.id.txtMixed);*/
        tvFlagged = view.findViewById(R.id.txtFlaggedTotal);
        tvFlaggedTexts = view.findViewById(R.id.txtTextsFlaggedCount);
        tvFlaggedImgs = view.findViewById(R.id.txtImagesFlaggedCount);
        tvTotalTexts = view.findViewById(R.id.txtTextsCount);
        tvTotalImgs = view.findViewById(R.id.txtImagesCount);
        tvTotalVids = view.findViewById(R.id.txtVideosCount);
        tvCotMsg = view.findViewById(R.id.tvCotMsg);
        cotCV = view.findViewById(R.id.cot_card_view);
        cotActionCV = view.findViewById(R.id.cot_action_card_view);
        btnAccept = view.findViewById(R.id.btnCotAccept);
        btnReject = view.findViewById(R.id.btnCotReject);

        // commented out might need to undo
        ivWordCloud = view.findViewById(R.id.imgWordCloud);
        tvWordCloudHeader = view.findViewById(R.id.txtWcHeader);

        //Creating Pie chart for sentiment analysis
        pieChart = view.findViewById(R.id.pieChart);

        //pieChart.getLegend().setEnabled(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawSliceText(false);
        Legend legend = pieChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.VERTICAL);
        legend.setDrawInside(false);

        LegendEntry legendEntryA = new LegendEntry();
        legendEntryA.label = "Positive";
        legendEntryA.formColor = Color.rgb(87, 204, 130);

        LegendEntry legendEntryB = new LegendEntry();
        legendEntryB.label = "Negative";
        legendEntryB.formColor = Color.rgb(235, 87, 92);
        LegendEntry legendEntryC = new LegendEntry();
        legendEntryC.label = "Neutral";
        legendEntryC.formColor = Color.rgb(243, 204, 123);;
        LegendEntry legendEntryD = new LegendEntry();
        legendEntryD.label = "Mixed";
        legendEntryD.formColor = Color.rgb(86, 86, 85);;



        legend.setCustom(Arrays.asList(legendEntryA, legendEntryB, legendEntryC, legendEntryD));

        //legend.setTextSize(16f);


        //Creating Bar chart for ??
       /* barChart = view.findViewById(R.id.barChart);

        barChart.setFitBars(true);*/

        if(getActivity()!=null) {
            pb = getActivity().findViewById(R.id.progressBar);
        }

        // displayAnalysisSummary();

        number = getActivity().getIntent().getStringExtra("number");
        name = getActivity().getIntent().getStringExtra("contact");

        if(name==null){
            getActivity().setTitle("Summary ("+number+")");
            name = number;
        }
        else
            getActivity().setTitle("Summary ("+name+")");

        cotSwitch = view.findViewById(R.id.cotSwitch);
        cotSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cotSwitch.setEnabled(false);
                uploadCOT(cotSwitch.isChecked(), -1);

            }
        });
        btnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cotBtnClicked(true);
            }
        });
        btnReject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cotBtnClicked(false);
            }
        });

        return view;
    }

    void cotBtnClicked(boolean accepted){
        if(cotStatus==null)
            return;

        if(!isAdded())
            return;

        if(AppStatus.hashedID==null) {
            //parent
            if(cotStatus.equals("1")){
                if(accepted){
                    //cot add request approved
                    uploadCOT(false, 2);
                }else {
                    //cot add request denied
                    uploadCOT(false, 0);
                }
            }
        }
        else if(AppStatus.account==null){
            //child
            if(cotStatus.equals("3")){
                if(accepted){
                    //cot removal request approved
                    uploadCOT(false, 0);
                }
                else {
                    //cot removal request denied
                    uploadCOT(false, 2);
                }
            }
        }
    }

    void uploadCOT(final boolean value, final int state){
        Log.e("cot changed", ""+value);
        String url = AppStatus.serverURL+"/cot-manager.php";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //Log.e("cot response", response);
                        cotSwitch.setEnabled(true);
                        updateDisplayForCot(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(isAdded())
                    cotSwitch.setChecked(!cotSwitch.isChecked());
                error.printStackTrace();
                Log.e("cot", "error");
            }
        }) {
            //adding parameters to the request
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                String topVal, bottomVal;
                if(AppStatus.hashedID==null){
                    params.put("from", "parent");
                    params.put("auth_token", AppStatus.account.getIdToken());
                    topVal="2";
                    bottomVal="3";
                }
                else if (AppStatus.account==null){
                    params.put("from", "teen");
                    params.put("dev_id", AppStatus.hashedID);
                    topVal="1";
                    bottomVal="0";
                }
                else{
                    return null;
                }
                params.put("name", name);
                params.put("number", number);
                if(state==-1)
                    params.put("cot", value?topVal:bottomVal);
                else {
                    params.put("cot", "" + state);
                    params.put("response", "1");
                }
                return params;
            }
        };
        // Add the request to the RequestQueue.
        AppStatus.queue.add(stringRequest);
    }

    void updateDisplayForCot(String cot){
        if(!isAdded())
            return;

        cotStatus = cot;
        if(AppStatus.hashedID==null){
            //parent
            final SharedPreferences prefs = getContext().getApplicationContext().getSharedPreferences("MyPref", 0);
            String teenName;
            if(prefs.contains("teen-name")){
                teenName = prefs.getString("teen-name", "Teen");
            }
            else{
                teenName = "Your Teen";
            }

            switch (cot){
                case "0": cotSwitch.setChecked(false);
                    trusted = false;
                    cotSwitch.setEnabled(true);
                    cotCV.setVisibility(View.VISIBLE);
                    cotActionCV.setVisibility(View.GONE);
                    break;
                case "1": cotSwitch.setChecked(true);
                    cotSwitch.setEnabled(false);
                    cotCV.setVisibility(View.GONE);
                    tvCotMsg.setText(teenName+" requested to add "+name+" to Circle of Trust");
                    btnAccept.setVisibility(View.VISIBLE);
                    btnReject.setVisibility(View.VISIBLE);
                    cotActionCV.setVisibility(View.VISIBLE);
                    break;
                case "2": cotSwitch.setChecked(true);
                    trusted = true;
                    cotSwitch.setEnabled(true);
                    cotCV.setVisibility(View.VISIBLE);
                    cotActionCV.setVisibility(View.GONE);
                    break;
                case "3": cotSwitch.setChecked(false);
                    cotSwitch.setEnabled(false);
                    tvCotMsg.setText("Your request to remove "+name+" from Circle of Trust is pending with "+teenName);
                    btnAccept.setVisibility(View.GONE);
                    btnReject.setVisibility(View.GONE);
                    cotCV.setVisibility(View.GONE);
                    cotActionCV.setVisibility(View.VISIBLE);
                    break;
                default: cotSwitch.setChecked(false);
            }
        } else if(AppStatus.account==null){
            //child
            switch (cot){
                case "0": cotSwitch.setChecked(false);
                    trusted = false;
                    cotSwitch.setEnabled(true);
                    cotCV.setVisibility(View.VISIBLE);
                    cotActionCV.setVisibility(View.GONE);
                    break;
                case "1": cotSwitch.setChecked(true);
                    cotSwitch.setEnabled(false);
                    tvCotMsg.setText("Your request to add "+name+" to Circle of Trust is pending with your parent");
                    btnAccept.setVisibility(View.GONE);
                    btnReject.setVisibility(View.GONE);
                    cotCV.setVisibility(View.GONE);
                    cotActionCV.setVisibility(View.VISIBLE);
                    break;
                case "2": cotSwitch.setChecked(true);
                    trusted = true;
                    cotSwitch.setEnabled(true);
                    cotCV.setVisibility(View.VISIBLE);
                    cotActionCV.setVisibility(View.GONE);
                    break;
                case "3": cotSwitch.setChecked(false);
                    cotSwitch.setEnabled(false);
                    tvCotMsg.setText("Your parent requested to remove "+name+" from Circle of Trust");
                    btnAccept.setVisibility(View.VISIBLE);
                    btnReject.setVisibility(View.VISIBLE);
                    cotCV.setVisibility(View.GONE);
                    cotActionCV.setVisibility(View.VISIBLE);
                    break;
                default: cotSwitch.setChecked(false);
            }
        }
    }

    void displayAnalysisSummary(){
        //Fetch data
        if(!isAdded())
            return;

        tableItems.clear();

        //SharedPreferences prefs = getContext().getApplicationContext().getSharedPreferences("MyPref", 0);

//        if(prefs.contains("teen-name")){
//            txtteenName.setText(prefs.getString("teen-name", "teen") + "'s Texting Activity");
//        }
//        else
//            txtteenName.setVisibility(View.GONE);

        String url = AppStatus.serverURL+"/retrieve-analysis.php";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.e("analysis response", response);

                        try {
                            JSONArray apps = new JSONArray(response);

                            for (int i = 0; i < apps.length(); i++) {
                                JSONObject c = apps.getJSONObject(i);

                                String positive = c.getString("positive");
                                String negative = c.getString("negative");
                                String neutral = c.getString("neutral");
                                String mixed = c.getString("mixed");
                                String totalFlagged = c.getString("total-flagged");
                                String textsFlagged = c.getString("texts-flagged");
                                String imagesFlagged = c.getString("images-flagged");
                                String totalTexts = c.getString("total-texts");
                                String totalImgs = c.getString("total-images");
                                //   String totalVids = c.getString("total-videos");
                                String cot = c.getString("cot");
                                final byte[] wordCloud = Base64.decode(c.getString("wc"), Base64.DEFAULT);
                                Bitmap decodedImage = BitmapFactory.decodeByteArray(wordCloud, 0, wordCloud.length);

                                //testing
                                //Bitmap decodedImage = null;

                                int positive1 = Math.round(Float.parseFloat(positive));
                                int negative1 = Math.round(Float.parseFloat(negative));
                                int neutral1 = Math.round(Float.parseFloat(neutral));
                                int mixed1 = Math.round(Float.parseFloat(mixed));

                                // Values for testing
                                double cat1 = 5.0;
                                double cat2 = 4.0;
                                double cat3 =  6.0;

                                if(isAdded()){

                                    boolean pos_flag;
                                    boolean neg_flag;
                                    boolean neu_flag;
                                    boolean mix_flag;


                                    ArrayList<PieEntry> entries = new ArrayList<>();

                                    //int [] color= new int[4];

                                    ArrayList<Integer> colors = new ArrayList<Integer>();

                                    /*
                                    // Negative
                                    color[0] = Color.rgb(235, 87, 92);
                                    // Neutral
                                    color[1] = Color.rgb(243, 204, 123);;
                                    // Positive i thinks
                                    color[2] = Color.rgb(87, 204, 130);
                                    // mixed i thinks
                                    color[3] = Color.rgb(86, 86, 85);
                                    */


//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



                                    if(positive1 > 0 )
                                    {
                                        entries.add(new PieEntry(positive1, "Positive"));
                                        colors.add(Color.rgb(87, 204, 130));
                                    }
                                    if(negative1 > 0 )
                                    {
                                        entries.add(new PieEntry(negative1,"Negative"));
                                        colors.add(Color.rgb(235, 87, 92));
                                    }
                                    if(neutral1 > 0 )
                                    {
                                        entries.add(new PieEntry(neutral1,"Neutral"));
                                        colors.add(Color.rgb(243, 204, 123));
                                    }
                                    if(mixed1 > 0 )
                                    {
                                        entries.add(new PieEntry(mixed1, "Mixed"));
                                        colors.add(Color.rgb(86, 86, 85));
                                    }

                                    int[] color = new int[colors.size()];
                                    int k = 0;
                                    for(Integer col : colors){
                                        color[k] = col.intValue();
                                        k++;
                                    }



                                    PieDataSet setPie = new PieDataSet(entries,"");


                                    //setPie.setColors(R.color.positive, R.color.negative, R.color.neutral, R.color.mixed);

                                    setPie.setColors(color);

                                    PieData data = new PieData(setPie);
                                    data.setDrawValues(true);
                                    data.setValueFormatter(new PercentFormatter(pieChart));
                                    data.setValueTextSize(10f);
                                    data.setValueTextColor(Color.BLACK);
                                    pieChart.setUsePercentValues(true);

                                    pieChart.setData(data);
                                    pieChart.invalidate();


                                   /* tvPositive.setText(positive1+"%");
                                    tvNegative.setText(negative1+"%");
                                    tvNeutral.setText(neutral1+"%");
                                    tvMixed.setText(mixed1+"%");*/
                                    tvFlagged.setText(totalFlagged);
                                    tvFlaggedTexts.setText(textsFlagged);
                                    tvFlaggedImgs.setText(imagesFlagged);
                                    tvTotalTexts.setText(totalTexts);
                                    tvTotalImgs.setText(totalImgs);
                                    //   tvTotalVids.setText(totalVids);
                                    if(decodedImage==null){

                                       /* ArrayList<BarEntry> entries2 = new ArrayList<>();
                                        entries2.add(new BarEntry(1f, 2));
                                        entries2.add(new BarEntry(3f, 4));
                                        entries2.add(new BarEntry(5f, 6));

                                        BarDataSet setBar = new BarDataSet(entries2,"Category Analysis");
                                        setBar.setColors(color);

                                        BarData data2 = new BarData(setBar);
                                        barChart.setData(data2);*/



                                        ivWordCloud.setImageResource(R.drawable.no_word_cloud);
                                        ivWordCloud.setImageDrawable(null);
                                        ivWordCloud.setVisibility(View.GONE);
                                        tvWordCloudHeader.setText(R.string.no_images_exchanged);
                                    }
                                    else{
                                        tvWordCloudHeader.setText(R.string.word_cloud_header);
                                        final Drawable drawable = new BitmapDrawable(getResources(), decodedImage);
                                        ivWordCloud.setImageDrawable(drawable);
                                        ivWordCloud.setVisibility(View.VISIBLE);
                                        ivWordCloud.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                String fname = "imgWC-"+number.replace(" ", "").replace("+", "");
                                                File temp;
                                                try{
                                                    temp = new File(getContext().getExternalFilesDir(null), fname);
                                                } catch (Exception e){
                                                    return;
                                                }
                                                final File out = temp;
                                                if (!out.exists()) {
                                                    OutputStream os = null;
                                                    try {
                                                        os = new FileOutputStream(out);
                                                        os.write(wordCloud);
                                                        os.close();
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                        return;
                                                    }
                                                }
                                                Intent intent = new Intent();
                                                Uri photoURI = FileProvider.getUriForFile(getContext().getApplicationContext(), getContext().getApplicationContext().getPackageName() + ".provider", out);
                                                intent.setAction(Intent.ACTION_VIEW);
                                                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                                intent.setDataAndType(photoURI, "image/png");
                                                startActivity(intent);
                                            }
                                        });
                                    }
                                    updateDisplayForCot(cot);
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if(pb!=null)
                            pb.setVisibility(View.GONE);
                        view.setVisibility(View.VISIBLE);
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
                params.put("number", number);
                params.put("type", "2");
                return params;
            }
        };
        // Add the request to the RequestQueue.
        AppStatus.queue.add(stringRequest);
    }

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
        public void onFragmentInteraction(String data);
    }

    private BroadcastReceiver refreshTextCount= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(isAdded()){
                //displayTextAnalysis();
                //Toast.makeText(getContext(), "Broadcast recd", Toast.LENGTH_SHORT).show();
                Snackbar.make(getView(), intent.getStringExtra("body"), Snackbar.LENGTH_LONG).show();
            }
        }
    };
    private BroadcastReceiver refreshCotScreen= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(isAdded()){
                displayAnalysisSummary();
                Snackbar.make(getView(), intent.getStringExtra("body"), Snackbar.LENGTH_LONG).show();
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        displayAnalysisSummary();
        if(isAdded()){
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(refreshTextCount,
                    new IntentFilter("refresh-text-counts"));
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(refreshCotScreen,
                    new IntentFilter("refresh-cot"));
        }

        //delete temp files
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

    @Override
    public void onPause() {
        super.onPause();
        if(isAdded())
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(refreshTextCount);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(refreshCotScreen);
    }

}