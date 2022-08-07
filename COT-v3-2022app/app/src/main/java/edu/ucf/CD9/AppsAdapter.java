package edu.ucf.CD9;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;

public class AppsAdapter extends RecyclerView.Adapter<AppsAdapter.ViewHolder>{

    private Context context;
    private List<String[]> stringList;

    AppsAdapter(Context context, List<String[]> list){

        this.context = context;

        stringList = list;
    }

    static class ViewHolder extends RecyclerView.ViewHolder{

        CardView cardView;
        ImageView imageView;
        TextView textView_App_Name;
        TextView textView_App_Package_Name;
        TextView txtInstallStatus;
        TextView txtInstallUninstallTime;

        ViewHolder (View view){

            super(view);

            cardView = view.findViewById(R.id.card_view);
            imageView = view.findViewById(R.id.imageview);
            textView_App_Name = view.findViewById(R.id.Apk_Name);
            textView_App_Package_Name = view.findViewById(R.id.Apk_Package_Name);
            txtInstallStatus = view.findViewById(R.id.txtInstallStatus);
            txtInstallUninstallTime = view.findViewById(R.id.txtInstallTime);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){

        View view2 = LayoutInflater.from(context).inflate(R.layout.cardview_apps,parent,false);

        return new ViewHolder(view2);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position){

        final String appPackageName = stringList.get(position)[1];
        final String appLabelName = stringList.get(position)[0];
        byte[] decodedString = Base64.decode(stringList.get(position)[2], Base64.DEFAULT);
        final short appInstallStatus = Short.parseShort(stringList.get(position)[3]);
        final String appInstallTime = stringList.get(position)[4];

        Bitmap decodedImage = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        Drawable drawable = new BitmapDrawable(decodedImage);

        viewHolder.textView_App_Name.setText(appLabelName);
        viewHolder.textView_App_Package_Name.setText(appPackageName);
        viewHolder.imageView.setImageDrawable(drawable);

        if(appInstallStatus==0)
            viewHolder.txtInstallStatus.setText(R.string.uninstalled);
        else
            viewHolder.txtInstallStatus.setText(R.string.installed);

        if(appInstallTime.equals("null"))
            viewHolder.txtInstallUninstallTime.setText("");
        else
            viewHolder.txtInstallUninstallTime.setText(getTime(Long.parseLong(appInstallTime)));

        //Adding click listener on CardView to open clicked application directly from here .
        viewHolder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

//                if(ApplicationPackageName.matches("com.(android|example).*")){
//                    //app is most probably not on play store
//                    Toast.makeText(context, "This app is not listed on Google Play", Toast.LENGTH_SHORT).show();
//                    return;
//                }

                try {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                }
            }
        });
    }

    @Override
    public int getItemCount(){

        return stringList.size();
    }

    private String getTime(long time){
        time = time * 1000;
        Calendar now = Calendar.getInstance();
        Calendar timeToCheck = Calendar.getInstance();
        timeToCheck.setTimeInMillis(time);
        String format = "MMM dd, hh:mm:ss a";
        if(now.get(Calendar.YEAR) != timeToCheck.get(Calendar.YEAR)) {
            format = "MMM dd yyyy, hh:mm:ss a";
        }else {
            if(now.get(Calendar.DAY_OF_YEAR) == timeToCheck.get(Calendar.DAY_OF_YEAR)) {
                format = "hh:mm:ss a";
            }else{
                if(now.get(Calendar.WEEK_OF_YEAR) == timeToCheck.get(Calendar.WEEK_OF_YEAR)) {
                    format = "EEE, hh:mm:ss a";
                }
            }
        }
        return android.text.format.DateFormat.format(format, time).toString();
    }
}