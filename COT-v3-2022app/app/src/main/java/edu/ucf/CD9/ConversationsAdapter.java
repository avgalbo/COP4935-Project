package edu.ucf.CD9;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.fragment.app.FragmentActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberMatch;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;

public class ConversationsAdapter extends RecyclerView.Adapter<ConversationsAdapter.ViewHolder>{

    private ArrayList<SummaryData> summaryData;
    private FragmentActivity parentActivity;

    ConversationsAdapter(FragmentActivity parentActivity, ArrayList<SummaryData> summaryData){

        this.summaryData = summaryData;
        this.parentActivity = parentActivity;
    }

    static class ViewHolder extends RecyclerView.ViewHolder{

        CardView cardView;
        ImageView imgProfilePic;
        TextView txtContactName;
        TextView txtContactNumber;
        TextView txtSentCount;
        TextView txtRecdCount;
        TextView txtLastActivity;
        TextView txtUnreadCount;
        TextView txtFlagCountRecd;
        TextView txtFlagCountSent;
        ImageView imgTrustShield;
        LinearLayout llConv;
        ImageView ivFlagSent, ivFlagRecd;

        ViewHolder(View view){

            super(view);

            cardView = (CardView) view.findViewById(R.id.conv_card_view);
            imgProfilePic = (ImageView) view.findViewById(R.id.imgProfilePic);
            imgProfilePic.setImageDrawable(null);
            txtContactName = (TextView) view.findViewById(R.id.txt_contact_name);
            txtContactNumber = (TextView) view.findViewById(R.id.txt_contact_number);
            txtSentCount = (TextView) view.findViewById(R.id.txtSentCount);
            txtRecdCount = (TextView) view.findViewById(R.id.txtReceivedCount);
            txtLastActivity = (TextView) view.findViewById(R.id.txtLastActivity);
            txtUnreadCount = (TextView) view.findViewById(R.id.txtUnreadCount);
            txtFlagCountRecd = (TextView) view.findViewById(R.id.txtFlagsRecd);
            txtFlagCountSent = (TextView) view.findViewById(R.id.txtFlagsSent);
            imgTrustShield = (ImageView) view.findViewById(R.id.imgTrustShield);
            llConv = (LinearLayout) view.findViewById(R.id.llconv);
            ivFlagSent = (ImageView) view.findViewById(R.id.ivFlagSent);
            ivFlagRecd = (ImageView) view.findViewById(R.id.ivFlagRecd);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){

        View view = LayoutInflater.from(parentActivity).inflate(R.layout.conversation_cardlayout, parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position){

        final String name = summaryData.get(position).name;
        final String number = summaryData.get(position).number;
        final byte[] pic = summaryData.get(position).pic;
        final int sent = summaryData.get(position).sent;
        final int recd = summaryData.get(position).received;
        final long lastActivity = summaryData.get(position).time;
        final int unread_count = summaryData.get(position).unreadCount;
        final int flag_count_sent = summaryData.get(position).flaggedSent;
        final int flag_count_recd = summaryData.get(position).flaggedRecd;
        final int trusted = summaryData.get(position).trusted;
        viewHolder.txtContactNumber.setVisibility(View.VISIBLE);

        if(name.equals("null")){
            viewHolder.txtContactName.setText(number);
            viewHolder.txtContactNumber.setVisibility(View.GONE);
            //viewHolder.txtContactNumber.setText(number);
        }else{
            viewHolder.txtContactName.setText(name);
            viewHolder.txtContactNumber.setText(number);
        }
        viewHolder.txtSentCount.setText(""+sent);
        viewHolder.txtRecdCount.setText(""+recd);
        viewHolder.txtFlagCountRecd.setText(""+flag_count_recd);
        viewHolder.txtFlagCountSent.setText(""+flag_count_sent);
        switch (trusted){
            case 0: viewHolder.imgTrustShield.setImageResource(R.drawable.untrusted_shield);
            break;
            case 2: viewHolder.imgTrustShield.setImageResource(R.drawable.trust_shield);
            break;
            default: viewHolder.imgTrustShield.setImageResource(R.drawable.shield_pending);
        }
        if(pic==null){
            viewHolder.imgProfilePic.setImageDrawable(parentActivity.getResources().getDrawable(R.drawable.unknown_contact));
        }else {
            byte[] contactPic = Base64.decode(pic, Base64.DEFAULT);
            Bitmap decodedImage = BitmapFactory.decodeByteArray(contactPic, 0, contactPic.length);
            viewHolder.imgProfilePic.setImageBitmap(decodedImage);
        }
        viewHolder.txtLastActivity.setText(getTime(lastActivity));

        if(flag_count_recd > 0 || flag_count_sent > 0){
            viewHolder.llConv.setBackgroundColor(0x55ffaaaa);
        }
        else{
            viewHolder.llConv.setBackgroundColor(0xffffffff);
        }

        if(flag_count_recd == 0 && flag_count_sent == 0){
            viewHolder.ivFlagRecd.setVisibility(View.INVISIBLE);
            viewHolder.txtFlagCountRecd.setVisibility(View.INVISIBLE);
            viewHolder.ivFlagSent.setVisibility(View.INVISIBLE);
            viewHolder.txtFlagCountSent.setVisibility(View.INVISIBLE);
        }
        else{
            if(flag_count_recd > 0){
                viewHolder.ivFlagRecd.setVisibility(View.VISIBLE);
                viewHolder.txtFlagCountRecd.setVisibility(View.VISIBLE);
            }else{
                viewHolder.ivFlagRecd.setVisibility(View.INVISIBLE);
                viewHolder.txtFlagCountRecd.setVisibility(View.INVISIBLE);
            }

            if(flag_count_sent > 0){
                viewHolder.ivFlagSent.setVisibility(View.VISIBLE);
                viewHolder.txtFlagCountSent.setVisibility(View.VISIBLE);
            } else {
                viewHolder.ivFlagSent.setVisibility(View.INVISIBLE);
                viewHolder.txtFlagCountSent.setVisibility(View.INVISIBLE);
            }
        }


        if(AppStatus.account!=null){
            //show counts only to parent
            if(unread_count>0){
                //show unread counter
                if(unread_count<100)
                    viewHolder.txtUnreadCount.setText(""+unread_count);
                else
                    viewHolder.txtUnreadCount.setText("99+");
                viewHolder.txtUnreadCount.setVisibility(View.VISIBLE);

            }else{
                //hide unread counter
                viewHolder.txtUnreadCount.setVisibility(View.INVISIBLE);
            }
        }

        //Adding click listener on CardView to open clicked application directly from here .
        viewHolder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String clickedContactNumber = number;
                PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                Iterator<PhoneNumberMatch> phoneNo = phoneUtil.findNumbers(clickedContactNumber, "US").iterator();
                Phonenumber.PhoneNumber number = null;
                if(phoneNo.hasNext())
                    number  = phoneNo.next().number();
                else{
                    try {
                        number = phoneUtil.parse(clickedContactNumber, "US");
                    } catch (NumberParseException e){
                        return;
                    }
                }

                final String address = phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);

                String contactName;
                if(name.equals("null"))
                    contactName=null;
                else
                    contactName=name;

                final String contact = contactName;
//                MainActivity ma = (MainActivity) parentActivity;
//                ma.showBackButton(true);
//                android.support.v4.app.FragmentManager fragmentManager = parentActivity.getSupportFragmentManager();
//                TextsFragment textsFragment = TextsFragment.newInstance(address, contact, pic, false);
////                textsFragment.setArguments(bundle);
//                fragmentManager.beginTransaction().replace(R.id.rlContentMain, textsFragment, "TextDetails").addToBackStack("TextSummary").commit();
                Intent intent = new Intent(parentActivity, AnalysisActivity.class);
                intent.putExtra("number", address);
                intent.putExtra("contact", contact);
                intent.putExtra("pic", pic);
                parentActivity.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return summaryData.size();
    }

    private String getTime(long time){
        time = time * 1000;
        Calendar now = Calendar.getInstance();
        Calendar timeToCheck = Calendar.getInstance();
        timeToCheck.setTimeInMillis(time);
        String format = "MMM dd";
        if(now.get(Calendar.YEAR) != timeToCheck.get(Calendar.YEAR)) {
            format = "MMM dd yyyy";
        }else {
            if(now.get(Calendar.DAY_OF_YEAR) == timeToCheck.get(Calendar.DAY_OF_YEAR)) {
                format = "hh:mm a";
            }else{
                if(now.get(Calendar.WEEK_OF_YEAR) == timeToCheck.get(Calendar.WEEK_OF_YEAR)) {
                    format = "EEE";
                }
            }
        }
        return android.text.format.DateFormat.format(format, time).toString();
    }
}