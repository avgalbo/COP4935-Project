package edu.ucf.CD9;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;

/**
 * Created by abhiditya
 */

public class MessageListAdapter extends RecyclerView.Adapter {

    private Context mContext;
    private List<MessageData> mMessageList;

    MessageListAdapter(Context context, List<MessageData> messageList) {
        mMessageList = messageList;
        mContext = context;
    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }

    // Determines the appropriate ViewType, sent or received
    @Override
    public int getItemViewType(int position) {
        return position;
    }

    // Inflates the appropriate layout according to the ViewType.
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int position) {
        View view;
        MessageData message = mMessageList.get(position);

        if (message.isSent()) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageHolder(view);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageHolder(view);
        }
    }

    // Passes the message object to a ViewHolder so that the contents can be bound to UI.
    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {

        MessageData message = mMessageList.get(position);

        if (message.isSent()) {
            ((SentMessageHolder) holder).bind(message, position);
        } else {
            ((ReceivedMessageHolder) holder).bind(message, position);
        }
    }

    private class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText;
        ImageView imgView, imgFlagText, imgFlagMedia;

        SentMessageHolder(View itemView) {
            super(itemView);

            messageText = itemView.findViewById(R.id.text_message_body);
            timeText = itemView.findViewById(R.id.text_message_time);
            imgView = itemView.findViewById(R.id.imageView);
            imgFlagText = itemView.findViewById(R.id.imgMsgFlagText);
            imgFlagMedia = itemView.findViewById(R.id.imgMsgFlagMedia);
            imgView.setImageDrawable(null);
            imgView.setVisibility(View.GONE);
            imgView.setOnClickListener(null);
        }

        void bind(final MessageData message, int position) {
            String text = message.getSentiments();
            final String time = getTime(message.getTime());
            final boolean showFlag = message.isShowFlag();

            if(message.isAnalysis() && !text.equals("null")){
                String[] texts = text.split(",");
                if(message.isTextFlagged())
                    text = "FLAGGED TEXT\n";
                else
                    text = "";

                int positive = Math.round(Float.parseFloat(texts[7])*100);
                int negative = Math.round(Float.parseFloat(texts[3])*100);
                int neutral = Math.round(Float.parseFloat(texts[5])*100);
                int mixed = Math.round(Float.parseFloat(texts[1])*100);

                if(positive>30)
                    text = text + (text.equals("")?"":"\n") + "\uD83D\uDE42 Positive: "+ positive + "%";
                if(negative>30)
                    text = text + (text.equals("")?"":"\n") + "\uD83D\uDE41 Negative: " + negative + "%";
                if(neutral>30)
                    text = text + (text.equals("")?"":"\n") + "\uD83D\uDE10 Neutral: "+ neutral + "%";
                if(mixed>30)
                    text = text + (text.equals("")?"":"\n") + "\uD83D\uDE36 Mixed: " + mixed + "%";
            }

            if(message.isAnalysis()){
                if(text.equals("null")){
                    text = "";
                }
                //Log.e("IMGMODERATION", message.getImgModeratoin());
                if(message.getMimetype().contains("image")){
                    if(!message.getImgModeratoin().equals("null")){
                        StringBuilder out = new StringBuilder();
                        String[] moderationFlags = message.getImgModeratoin().split(",");
                        HashSet<String> tags = new HashSet<>();
                        ArrayList<String> flag = new ArrayList<>();
                        ArrayList<Float> chances = new ArrayList<>();
                        for(String p : moderationFlags){
                            if(p.equals("")) continue;
                            try {
                                chances.add(Float.parseFloat(p));
                            } catch (Exception e){
                                if(!tags.contains(p)){
                                    tags.add(p);
                                    flag.add(p);
                                }

                            }
                        }
                        for(int i=0; i<tags.size(); i++){
                            out.append(flag.get(i));
                            out.append(": ");
                            out.append(Math.round(chances.get(i)));
                            out.append("%");
                            if(i<tags.size()-1)
                                out.append("\n");
                        }
                        text = text + (text.equals("")?"":"\n\n") + "FLAGGED IMAGE:\n\n"+out.toString();
                    }
                    else {
                        //show image entities
                        StringBuilder out = new StringBuilder();
                        String[] moderationFlags = message.getMediaLabels().split(",");
                        HashSet<String> tags = new HashSet<>();
                        ArrayList<String> flag = new ArrayList<>();
                        ArrayList<Float> chances = new ArrayList<>();
                        for(String p : moderationFlags){
                            if(p.equals("")) continue;
                            try {
                                chances.add(Float.parseFloat(p));
                            } catch (Exception e){
                                if(!tags.contains(p)){
                                    tags.add(p);
                                    flag.add(p);
                                }

                            }
                        }
                        for(int i=0; i<flag.size(); i++){
                            if(i!=0)
                                out.append(", ");

                            out.append(flag.get(i));
                        }

                        text = text + (text.equals("")?"":"\n") +"Image Sent\n\nEntities: "+out.toString();
                    }
                }
                else if(message.getMimetype().contains("video")){
                    text = text + (text.equals("")?"":"\n") + "Video Sent\n";
                }
                else if(message.getMimetype().contains("audio")){
                    text = text + (text.equals("")?"":"\n") + "Audio Sent\n";
                }

                if(showFlag){
                    displayMedia(message, imgView, messageText, position, 1);
                }
                //text = text +"\n";
            }
            else
                displayMedia(message, imgView, messageText, position, 1);

            if(!message.getMessage().equals(""))
                text = text + (message.isAnalysis()?"\n\nActual Text:\n":"") + message.getMessage();

            final String msg = text;

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    //UI thread
                    try{
                        messageText.setText(msg);
                        timeText.setText(time);

                        if(showFlag){
                            if(message.getMimetype().equals("null")){
                                imgFlagText.setVisibility(View.VISIBLE);
                                imgFlagMedia.setVisibility(View.GONE);
                            }else{
                                imgFlagMedia.setVisibility(View.VISIBLE);
                                imgFlagText.setVisibility(View.GONE);
                            }
                        }
                        else{
                            imgFlagMedia.setVisibility(View.GONE);
                            imgFlagText.setVisibility(View.GONE);
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText, nameText;
        ImageView imgView, imgProfilePic, imgFlagText, imgFlagMedia;

        ReceivedMessageHolder(View itemView) {
            super(itemView);

            messageText = itemView.findViewById(R.id.text_message_body);
            timeText = itemView.findViewById(R.id.text_message_time);
            nameText = itemView.findViewById(R.id.text_message_name);
            imgView = itemView.findViewById(R.id.imageView);
            imgFlagText = itemView.findViewById(R.id.imgMsgFlagText);
            imgFlagMedia = itemView.findViewById(R.id.imgMsgFlagMedia);
            imgProfilePic = itemView.findViewById(R.id.img_profile_pic);
            if(TextsFragment.profilePic!=null)
                imgProfilePic.setImageBitmap(TextsFragment.profilePic);
            imgView.setImageDrawable(null);
            imgView.setVisibility(View.GONE);
            imgView.setOnClickListener(null);
        }

        void bind(final MessageData message, int position) {
            String text = message.getSentiments();
            final String time = getTime(message.getTime());
            final boolean showFlag = message.isShowFlag();

            if(message.isAnalysis() && !text.equals("null")){
                String[] texts = text.split(",");
                if(message.isTextFlagged())
                    text = "FLAGGED TEXT\n";
                else
                    text = "";

                int positive = Math.round(Float.parseFloat(texts[7])*100);
                int negative = Math.round(Float.parseFloat(texts[3])*100);
                int neutral = Math.round(Float.parseFloat(texts[5])*100);
                int mixed = Math.round(Float.parseFloat(texts[1])*100);

                if(positive>30)
                    text = text + (text.equals("")?"":"\n") + "\uD83D\uDE42 Positive: "+ positive + "%";
                if(negative>30)
                    text = text + (text.equals("")?"":"\n") + "\uD83D\uDE41 Negative: " + negative + "%";
                if(neutral>30)
                    text = text + (text.equals("")?"":"\n") + "\uD83D\uDE10 Neutral: "+ neutral + "%";
                if(mixed>30)
                    text = text + (text.equals("")?"":"\n") + "\uD83D\uDE36 Mixed: " + mixed + "%";

                //text = text + "Positive: "+ Math.round(Float.parseFloat(texts[7])) + "\nNegative: "+ Math.round(Float.parseFloat(texts[3]))
                //        + "\nNeutral: "+Math.round(Float.parseFloat(texts[5])) + "\nMixed: "+ Math.round(Float.parseFloat(texts[1]));
            }

            if(message.isAnalysis()){
                if(text.equals("null")){
                    text = "";
                }
                if(message.getMimetype().contains("image")){
                    Log.e("IMGMODERATION", message.getImgModeratoin());
                    if(!message.getImgModeratoin().equals("null")){
                        StringBuilder out = new StringBuilder();
                        String[] moderationFlags = message.getImgModeratoin().split(",");
                        HashSet<String> tags = new HashSet<>();
                        ArrayList<String> flag = new ArrayList<>();
                        ArrayList<Float> chances = new ArrayList<>();
                        for(String p : moderationFlags){
                            if(p.equals("")) continue;
                            try {
                                chances.add(Float.parseFloat(p));
                            } catch (Exception e){
                                if(!tags.contains(p)){
                                    tags.add(p);
                                    flag.add(p);
                                }

                            }
                        }
                        for(int i=0; i<flag.size(); i++){
                            out.append(flag.get(i));
                            out.append(": ");
                            out.append(Math.round(chances.get(i)));
                            out.append("%");
                            if(i<tags.size()-1)
                                out.append("\n");
                        }
                        text = text + (text.equals("")?"":"\n\n") + "FLAGGED IMAGE:\n\n"+out.toString();
                    }
                    else {
                        //show image entities
                        StringBuilder out = new StringBuilder();
                        String[] moderationFlags = message.getMediaLabels().split(",");
                        HashSet<String> tags = new HashSet<>();
                        ArrayList<String> flag = new ArrayList<>();
                        ArrayList<Float> chances = new ArrayList<>();
                        for(String p : moderationFlags){
                            if(p.equals("")) continue;
                            try {
                                chances.add(Float.parseFloat(p));
                            } catch (Exception e){
                                if(!tags.contains(p)){
                                    tags.add(p);
                                    flag.add(p);
                                }

                            }
                        }
                        for(int i=0; i<flag.size(); i++){
                            if(i!=0)
                                out.append(", ");

                            out.append(flag.get(i));
                        }

                        text = text + (text.equals("")?"":"\n") + "Image Received\n\nEntities: "+out.toString();

                    }
                }
                else if(message.getMimetype().contains("video")){
                    text = text + (text.equals("")?"":"\n") + "Video Received\n";
                }
                else if(message.getMimetype().contains("audio")){
                    text = text + (text.equals("")?"":"\n") + "Audio Received\n";
                }

                if(showFlag){
                    displayMedia(message, imgView, messageText, position, 0);
                }
                //text = text +"\n";
            }
            else
                displayMedia(message, imgView, messageText, position, 0);

            if(!message.getMessage().equals(""))
                text = text + (message.isAnalysis()?"\n\nActual Text:\n":"") + message.getMessage();

            final String msg = text;

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    //UI thread
                    try{
                        messageText.setText(msg);
                        // Format the stored timestamp into a readable String using method.
                        timeText.setText(time);

                        if (message.getName() == null || message.getName().equals("null"))
                            nameText.setText(message.getNumber());
                        else
                            nameText.setText(message.getName());

                        if(showFlag){
                            if(message.getMimetype().equals("null")){
                                imgFlagText.setVisibility(View.VISIBLE);
                                imgFlagMedia.setVisibility(View.GONE);
                            }else{
                                imgFlagMedia.setVisibility(View.VISIBLE);
                                imgFlagText.setVisibility(View.GONE);
                            }
                        }
                        else{
                            imgFlagMedia.setVisibility(View.GONE);
                            imgFlagText.setVisibility(View.GONE);
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });
        }
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

    private void displayMedia(final MessageData message, final ImageView imgView, final TextView tvMsg, final int position, final int sent) {
        if (message.getMedia() != null && !message.getMimetype().equals("null")) {
            if (message.getMimetype().contains("gif")) {
                try {
                    Glide.with(mContext).load(message.getMedia()).into(imgView);
                    imgView.setVisibility(View.VISIBLE);
                    imgView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            openMedia(message, position, sent);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (message.getMimetype().contains("image")) {
                //image media
                Log.d("display mms", "image media found");

                Bitmap decodedImage = BitmapFactory.decodeByteArray(message.getMedia(), 0, message.getMedia().length);
                if(decodedImage==null){
                    Log.i("bitmap", "bitmap was null");
                    return;
                }
                final Drawable drawable = new BitmapDrawable(mContext.getResources(), decodedImage);

                imgView.setImageDrawable(drawable);
                imgView.setVisibility(View.VISIBLE);
                imgView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        openMedia(message, position, sent);
                    }
                });

            } else if (message.getMimetype().contains("video")){
                //video

                    Thread vidThumbThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                final File out = new File(mContext.getExternalFilesDir(null), "vid-"+position + message.getNumber().replace(" ", "") + sent + "." + message.getMimetype().substring(message.getMimetype().indexOf("/") + 1));
                                if (!out.exists()) {
                                    OutputStream os = new FileOutputStream(out);
                                    os.write(message.getMedia());
                                    os.close();
                                }
                                //Log.d("media_path", out.getPath());
                                //final FutureTarget<Bitmap> futureTarget = Glide.with(mContext.getApplicationContext()).asBitmap().load(out).submit();
                                //final Bitmap bmp1 = futureTarget.get();
                                Bitmap bmp1 = ThumbnailUtils.createVideoThumbnail(out.getPath(),MediaStore.Images.Thumbnails.MINI_KIND);

                                if (bmp1 == null) {
                                    Log.i("bitmap", "video bitmap was null");
                                    return;
                                }

                                Bitmap bmp2 = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_play);
                                int bmp1W = bmp1.getWidth();
                                int bmp2W = bmp2.getWidth();
                                int bmp1H = bmp1.getHeight();
                                int bmp2H = bmp2.getHeight();
                                if(bmp1W<bmp2W || bmp1H<bmp2H){
                                    int size = Math.min(bmp1H, bmp1W);
                                    size = (int)(size - size*0.7);
                                    bmp2 = Bitmap.createScaledBitmap(bmp2, size, size, true);
                                }
                                Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
                                Canvas canvas = new Canvas(bmOverlay);
                                canvas.drawBitmap(bmp1, new Matrix(), null);
                                canvas.drawBitmap(bmp2, new Matrix(), null);
                                final Bitmap bitmap = bmOverlay;

                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        //UI thread
                                        try {
//                                            Glide.with(imgView).asBitmap().load(bitmap).listener(new RequestListener<Bitmap>() {
//                                                @Override
//                                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
//                                                    return false;
//                                                }
//
//                                                @Override
//                                                public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
//                                                    return false;
//                                                }
//                                            }).into(imgView);
                                            imgView.setImageBitmap(bitmap);
                                            imgView.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    Intent intent = new Intent();
                                                    Uri videoURI = FileProvider.getUriForFile(mContext.getApplicationContext(), mContext.getApplicationContext().getPackageName() + ".provider", out);
                                                    intent.setAction(Intent.ACTION_VIEW);
                                                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                                    intent.setDataAndType(videoURI, message.getMimetype());
                                                    mContext.startActivity(intent);
                                                }
                                            });
                                            imgView.setVisibility(View.VISIBLE);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }

                                    }
                                });
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    vidThumbThread.start();

            }
            else if (message.getMimetype().contains("audio")){
                //audio
                final Drawable drawable = mContext.getResources().getDrawable(R.drawable.audio_icon);
                imgView.setImageDrawable(drawable);
                imgView.setVisibility(View.VISIBLE);
                imgView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        openMedia(message, position, sent);
                    }
                });
            }
            if(message.getMessage().equals("") && !message.isAnalysis())
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        //UI thread
                        tvMsg.setVisibility(View.GONE);
                    }
                });
        }
    }

    private void openMedia(MessageData message, int position, int sent){
        String fname = "img-"+position+ message.getNumber().replace(" ", "").replace("+", "") + sent + "." + message.getMimetype().substring(message.getMimetype().indexOf("/") + 1);
        final File out = new File(mContext.getExternalFilesDir(null), fname);
        if (!out.exists()) {
            OutputStream os = null;
            try {
                os = new FileOutputStream(out);
                os.write(message.getMedia());
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
        Intent intent = new Intent();
        Uri photoURI = FileProvider.getUriForFile(mContext.getApplicationContext(), mContext.getApplicationContext().getPackageName() + ".provider", out);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(photoURI, message.getMimetype());
        mContext.startActivity(intent);
    }

}