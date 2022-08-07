package edu.ucf.CD9;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.Settings;
import androidx.annotation.NonNull;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberMatch;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by abhiditya
 */

class UploadTexts {
    private boolean initial, re_setup;
    private static boolean stop;
    private Context context;
    private static String url = AppStatus.serverURL+"/upload-texts.php";
    private static int retryms = 5000;
    private HashSet<String> contactNos;
    String deviceHash;
    private static String id;
    private int uploadCount = 0;
    private TextView tvProgress;
    private Activity activity;

    int UploadTexts(Context context, boolean initial, long last_time, final TextView tvProgress, Activity activity){

        /* This is how you can toast from background threads such as this one.
        activity.runOnUiThread(new Runnable() {
            public void run()
            {
                Toast.makeText(context, "am i going to work???", Toast.LENGTH_SHORT).show();
            }
        });
        */

        this.initial = initial;
        this.re_setup = re_setup;
        this.context = context;
        this.activity = activity;
        this.tvProgress = tvProgress;
        contactNos = new HashSet<String>();
        stop = false;

        long cutOffTime = 0;
        if(last_time == 0)
            cutOffTime = (System.currentTimeMillis() / 1000L) - /*86400*/ 1209600;
        else
            cutOffTime = last_time;

        if(AppStatus.queue==null)
            AppStatus.queue = Volley.newRequestQueue(context.getApplicationContext());

        if(initial){
            initialSMSUpload(cutOffTime);
            initialMMSUpload(cutOffTime);
            if(AppStatus.isWelcomeActivityForeground()){
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        tvProgress.setText("Processing...");
                    }
                });
            }
            return uploadCount;
        }

        String[] projection = new String[]{"_id", "date"};
        Uri mainUri;

        mainUri = Uri.parse("content://mms-sms/conversations?simple=true");
        final Cursor cursor = context.getContentResolver().
                query(mainUri, projection,
                        null, null, initial?"date ASC":"date DESC");

        if(cursor == null){
            return uploadCount;
        } else if(!cursor.moveToFirst()){
            cursor.close();
            return uploadCount;
        }

        if(initial){
            //skip conversations older than cutoff
            String date = cursor.getString(cursor.getColumnIndex("date"));
            if(date!=null){
                long threadDate = Long.parseLong(date);
                while (threadDate <= cutOffTime){
                    if(!cursor.moveToNext()){
                        cursor.close();
                        return uploadCount;
                    }
                    //Log.e("cursor", "skipped thread with date"+threadDate);
                    threadDate = Long.parseLong(cursor.getString(cursor.getColumnIndex("date")));
                }
            }
        }

        do{
            final String threadID = cursor.getString(cursor.getColumnIndex("_id"));

            //Log.e("New Conversation", "id "+threadID);

            mainUri = Uri.parse("content://mms-sms/conversations/"+threadID);
            projection = new String[]{"_id","ct_t"};

            final Cursor mainCursor = context.getContentResolver().
                    query(mainUri, projection,
                            null, null, initial?"normalized_date ASC":null);

            if(mainCursor == null){
                continue;
            } else if(mainCursor.getCount()==0){
                mainCursor.close();
                continue;
            } else{
                if(initial)
                    mainCursor.moveToFirst();
                else
                    mainCursor.moveToLast();
            }

            do {
                id = mainCursor.getString(mainCursor.
                        getColumnIndex("_id"));
                //discard duplicate triggers
                if(AppStatus.lastMsgId !=null && AppStatus.lastMsgId.equals(id)){
                    mainCursor.close();
                    cursor.close();
                    return uploadCount;
                }
                else {
                    AppStatus.lastMsgId = id;
                }
                //Toast.makeText(context, "ID "+id, Toast.LENGTH_SHORT).show();

                String msgContentType = mainCursor.getString(mainCursor.
                        getColumnIndex("ct_t"));


                //Log.i("message_type", "" + msgContentType);

                if ("application/vnd.wap.multipart.related".equals(msgContentType)) {
                    // it's MMS
                    //Log.v("Debug", "it's MMS");
                    String selection = "_id=" + id;
                    String[] mmsprojection = new String[]{"msg_box","date"};
                    Cursor mmsCursor = context.getContentResolver().query(Uri.parse("content://mms/"), mmsprojection, selection,
                            null, null);
                    //now we need to find if MMS message is sent or received
                    if(mmsCursor == null){
                        continue;
                    } else if(!mmsCursor.moveToFirst()){
                        mmsCursor.close();
                        continue;
                    }

                    //read time and skip messages older than 14 days
                    long dateVal = Long.parseLong(mmsCursor.getString(mmsCursor.getColumnIndex("date")));
                    if(dateVal <= cutOffTime){
                        mmsCursor.close();
                        continue;
                    }

                    int type = mmsCursor.getInt(mmsCursor.getColumnIndex("msg_box"));
                    mmsCursor.close();

                    if (type == 1) {
                        //it's received MMS
                        //Log.v("Debug", "it's received MMS");
                        getMMSinfo(id, dateVal, 0);
                    } else if (type == 2) {
                        //it's sent MMS
                        //Log.v("Debug", "it's Sent MMS");
                        getMMSinfo(id, dateVal, 1);
                    }

                } else {
                    // it's SMS
                    //Log.v("Debug", "it's SMS");
                    String selection = "_id=" + id;
                    String[] smsprojection = new String[]{"type","date","address","body"};
                    Cursor smsCursor = context.getContentResolver().query(Uri.parse("content://sms/"), smsprojection, selection,
                            null, null);
                    if(smsCursor == null){
                        continue;
                    } else if(!smsCursor.moveToFirst()){
                        smsCursor.close();
                        continue;
                    }

                    //Crashlytics.setInt("Number of cols: ", smsCursor.getCount());
                    int type = smsCursor.getInt(smsCursor.getColumnIndex("type"));

                    long dateVal = Long.parseLong(smsCursor.getString(smsCursor.
                            getColumnIndex("date")))/1000L;
                    if(dateVal <= cutOffTime){
                        smsCursor.close();
                        continue;
                    }

                    String phone = smsCursor.getString(smsCursor.
                            getColumnIndex("address"));
                    String body = smsCursor.getString(smsCursor.
                            getColumnIndex("body"));

                    smsCursor.close();

                    uploadSMS(phone, dateVal, body, type==1?0:1, id);
                }
            }while (initial && mainCursor.moveToNext() && !stop);
            mainCursor.close();
        }
        while(initial && cursor.moveToNext() && !stop);
        cursor.close();

        return uploadCount;
    }

    private void initialSMSUpload(long cutOffTime){
        String[] smsprojection = new String[]{"_id", "date","address","body"};
        //String selection = "date>" + cutOffTime*1000L;
        long cutOff = cutOffTime*1000L;
        String where = "date >= ?";
        String cutoffstr = ""+cutOff;
        String[] args = {cutoffstr};
        Cursor smsCursor = context.getContentResolver().query(Uri.parse("content://sms/inbox"), smsprojection, where,
                args, null);
        if(smsCursor == null){
            return;
        } else if(!smsCursor.moveToFirst()){
            smsCursor.close();
            return;
        }

        final int totalTextsR = smsCursor.getCount();
        int processed = 0;
        if(AppStatus.isWelcomeActivityForeground()){
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    tvProgress.setText("Reading received SMS 0/"+totalTextsR);
                }
            });
        }

        //Crashlytics.setInt("Number of cols: ", smsCursor.getCount());
        //Log.e("Inbox Count ", "Inbox Count "+smsCursor.getCount());

//        while(dateVal <= cutOffTime){
//            if(!smsCursor.moveToNext())
//                return;
//            dateVal = Long.parseLong(smsCursor.getString(smsCursor.
//                    getColumnIndex("date")))/1000L;
//        }
        do{
            long dateVal = Long.parseLong(smsCursor.getString(smsCursor.
                    getColumnIndex("date")))/1000L;
//            if(dateVal <= cutOffTime)
//                continue;
            //int type = smsCursor.getInt(smsCursor.getColumnIndex("type"));
            String msg_id = smsCursor.getString(smsCursor.getColumnIndex("_id"));
            String phone = smsCursor.getString(smsCursor.
                    getColumnIndex("address"));
            String body = smsCursor.getString(smsCursor.
                    getColumnIndex("body"));

            //Log.e("inbox", phone +" " +dateVal+" "+body);
            processed++;
            final int i = processed;
            if(AppStatus.isWelcomeActivityForeground()){
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        tvProgress.setText("Reading received SMS "+ i+"/"+totalTextsR);
                    }
                });
            }
            uploadSMS(phone, dateVal, body, 0, msg_id);
        }
        while (smsCursor.moveToNext());

        smsCursor.close();

        //Crashlytics.setInt("Number of cols: ", smsCursor.getCount());
        //Log.e("Sent Count ", "Sent Count "+smsCursor.getCount());
//        while(dateVal <= cutOffTime){
//            if(!smsCursor.moveToNext())
//                return;
//            dateVal = Long.parseLong(smsCursor.getString(smsCursor.
//                    getColumnIndex("date")))/1000L;
//        }

        Cursor smsCursorS = context.getContentResolver().query(Uri.parse("content://sms/sent"), smsprojection, where,
                args, null);
        if(smsCursorS == null){
            return;
        } else if(!smsCursorS.moveToFirst()){
            smsCursorS.close();
            return;
        }
        processed = 0;
        final int totalTextsS = smsCursorS.getCount();
        if(AppStatus.isWelcomeActivityForeground()){
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    tvProgress.setText("Reading sent SMS 0/"+totalTextsS);
                }
            });
        }

        do{
            long dateVal = Long.parseLong(smsCursorS.getString(smsCursorS.
                    getColumnIndex("date")))/1000L;
//            if(dateVal <= cutOffTime)
//                continue;

            //int type = smsCursor.getInt(smsCursor.getColumnIndex("type"));
            String msg_id = smsCursorS.getString(smsCursorS.getColumnIndex("_id"));
            String phone = smsCursorS.getString(smsCursorS.
                    getColumnIndex("address"));
            String body = smsCursorS.getString(smsCursorS.
                    getColumnIndex("body"));
            //Log.e("sent", phone +" " +dateVal+" "+body);
            final int i = processed;
            if(AppStatus.isWelcomeActivityForeground()){
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        tvProgress.setText("Reading sent SMS "+ i+"/"+totalTextsS);
                    }
                });
            }
            uploadSMS(phone, dateVal, body, 1, msg_id);
        }
        while (smsCursorS.moveToNext());

        smsCursorS.close();
    }

    private void initialMMSUpload(long cutOffTime){
        //String selection = "date>" + cutOffTime;
        String where = "date >= ?";
        String cutoffstr = ""+cutOffTime;
        String[] args = {cutoffstr};
        String[] mmsprojection = new String[]{"_id", "msg_box","date"};
        Cursor mmsCursor = context.getContentResolver().query(Uri.parse("content://mms/"), mmsprojection, where,
                args, null);
        //now we need to find if MMS message is sent or received
        if(mmsCursor == null){
            return;
        } else if(!mmsCursor.moveToFirst()){
            mmsCursor.close();
            return;
        }

        final int totalMMS = mmsCursor.getCount();
        int processed = 0;
        if(AppStatus.isWelcomeActivityForeground()){
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    tvProgress.setText("Reading MMS 0/"+totalMMS);
                }
            });
        }

        do{
            String mms_id = mmsCursor.getString(mmsCursor.getColumnIndex("_id"));
            long dateVal = Long.parseLong(mmsCursor.getString(mmsCursor.getColumnIndex("date")));
            if(dateVal <= cutOffTime)
                continue;
            int type = mmsCursor.getInt(mmsCursor.getColumnIndex("msg_box"));
            processed ++;
            final int i = processed;
            if(AppStatus.isWelcomeActivityForeground()){
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        tvProgress.setText("Reading MMS "+i+"/"+totalMMS);
                    }
                });
            }
            if (type == 1) {
                //it's received MMS
                //Log.v("Debug", "it's received MMS");
                getMMSinfo(mms_id, dateVal, 0);
            } else if (type == 2) {
                //it's sent MMS
                //Log.v("Debug", "it's Sent MMS");
                getMMSinfo(mms_id, dateVal, 1);
            }
        }while(mmsCursor.moveToNext());

        mmsCursor.close();
    }

    // method to read MMS
    private void getMMSinfo(String mms_id, Long dateVal, int sentFlag) {

        String selection = "mid=" + mms_id;
        Uri uri = Uri.parse("content://mms/part");

        Cursor cursor = context.getContentResolver().query(uri,
                null,selection,
                null, null);

        byte[] media = null;
        String body="", mediatype="", phone;

        if(cursor == null){
            return;
        } else if(!cursor.moveToFirst()){
            cursor.close();
            return;
        }

        phone = getMMSAddress(context.getApplicationContext(), mms_id, sentFlag);
        do {
            String partId = cursor.getString(cursor.getColumnIndex("_id"));
            String type = cursor.getString(cursor.getColumnIndex("ct"));
            if ("text/plain".equals(type)) {
                String data = cursor.getString(cursor.getColumnIndex("_data"));
                if (data != null) {
                    // implementation of this method below
                    body = getMmsText(partId);
                } else {
                    body = cursor.getString(cursor.getColumnIndex("text"));
                }
            }
            else {
                Uri partURI = Uri.parse("content://mms/part/" + partId);
                media = loadRaw(partURI);
                mediatype = type;
            }
        } while (cursor.moveToNext());

        cursor.close();

        uploadMMS(phone, dateVal, body, sentFlag, media, mediatype);
    }


    private void uploadSMS(String phNumber, final long time, final String msg, final int sentFlag, String msg_id){
        uploadCount++;
        //Format phone number
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        Iterator<PhoneNumberMatch> phoneNo = phoneUtil.findNumbers(phNumber, "US").iterator();
        Phonenumber.PhoneNumber number;
        if(phoneNo.hasNext())
            number  = phoneNo.next().number();
        else{
            try {
                number = phoneUtil.parse(phNumber, "US");
            } catch (NumberParseException e){
                return;
            }
        }
        final String address = phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
        boolean needPic;
        if (contactNos.contains(address)){
            needPic = false;
        }else{
            needPic = true;
            contactNos.add(address);
        }

        String[] contactInfo = getContactInfo(address, needPic);
        final String finalDisplayName = contactInfo[0];

        //encode text
        String base64;
        try {
            byte[] data;
            data = msg.getBytes("UTF-8");
            base64 = Base64.encodeToString(data, Base64.DEFAULT);
        } catch (UnsupportedEncodingException e) {
            base64 = msg;
        }
        final String base64Msg = base64;
        byte[] pic = null;

        if(needPic && contactInfo[1]!=null) //contactInfo[1] is photoURI
            pic = loadRaw(Uri.parse(contactInfo[1]));

        //get device identifier
        final String hashedID = getSHA512Hash(getDeviceID(), "TeenDevice");
        if(pic!=null && pic.length>1)
            executeRequest(hashedID, address, time, base64Msg, finalDisplayName, pic, sentFlag, initial, context, msg_id);
        else
            executeRequest(hashedID, address, time, base64Msg, finalDisplayName, sentFlag, initial, context, msg_id);
    }

    private static void executeRequest(final String hashedID, final String address, final long time, final String base64Msg,
                                       final String finalDisplayName, final int sentFlag, final boolean initial, final Context context, final String msg_id) {
        //create request to sent
        final StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //Toast.makeText(getApplicationContext(), "Server says: "+response, Toast.LENGTH_SHORT).show();
                        if (response.equals("do setup")) {
                            do_setup(context);
                        }
                        retryms = 5000;
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //Toast.makeText(context, "An error occurred", Toast.LENGTH_LONG).show();
                error.printStackTrace();

                try {
                    Thread.sleep(retryms < 600000 ? retryms += retryms * 0.1 : 600000);
                } catch (Exception e) {
                    //do nothing
                }
                executeRequest(hashedID, address, time, base64Msg, finalDisplayName, sentFlag, initial, context, msg_id);
            }
        }) {
            //adding parameters to the request
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("dev_id", hashedID);
                params.put("number", address);
                params.put("time", ""+time);
                params.put("text", base64Msg);
                params.put("name", finalDisplayName);
                params.put("sent", "" + sentFlag);
                if (initial)
                    params.put("initial-upload", "1");
                params.put("msg_id", msg_id);
                return params;
            }
        };
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
                Integer.MAX_VALUE,  // max retries
                Float.valueOf("1.1") //backoff multiplier
        ));

        //send
        AppStatus.queue.add(stringRequest);
    }

    private static void executeRequest(final String hashedID, final String address, final long time, final String base64Msg,
                                       final String finalDisplayName, final byte[] photo, final int sentFlag,
                                       final boolean initial, final Context context, final String msg_id) {

        VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(Request.Method.POST, url, new Response.Listener<NetworkResponse>() {
                @Override
                public void onResponse(NetworkResponse response) {
                    String resultResponse = new String(response.data);
                    // parse success output
                    //Log.e("RESPONSE", resultResponse);
                    if(Arrays.equals(response.data, "do setup".getBytes())){
                        do_setup(context);
                    }
                    retryms=5000;
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    error.printStackTrace();
                    try {
                        Thread.sleep(retryms<600000?retryms+=retryms*0.1:600000);
                    }catch (Exception e){
                        //do nothing
                    }
                    executeRequest(hashedID, address, time, base64Msg, finalDisplayName, photo, sentFlag, initial, context, msg_id);
                }
            }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("dev_id", hashedID);
                    params.put("number", address);
                    params.put("time", ""+time);
                    params.put("text", base64Msg);
                    params.put("name", finalDisplayName);
                    params.put("sent", ""+sentFlag);
                    if (initial)
                        params.put("initial-upload", "1");
                    params.put("msg_id", msg_id);
                    return params;
                }

                @Override
                protected Map<String, DataPart> getByteData() {
                    Map<String, DataPart> params = new HashMap<>();
                    params.put("contact_pic", new DataPart("pic", photo, "image/jpeg"));
                    return params;
                }
            };

            multipartRequest.setRetryPolicy(new DefaultRetryPolicy(
                    DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
                    Integer.MAX_VALUE,  // max retries
                    Float.valueOf("1.1") //backoff multiplier
        ));

        AppStatus.queue.add(multipartRequest);
    }


    private void uploadMMS(String phNumber, final long time, final String msg, final int sentFlag, final byte[] media, final String type) {
        uploadCount++;
        //Format phone number
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        Iterator<PhoneNumberMatch> phoneNo = phoneUtil.findNumbers(phNumber, "US").iterator();
        Phonenumber.PhoneNumber number;
        if(phoneNo.hasNext())
            number  = phoneNo.next().number();
        else{
            try {
                number = phoneUtil.parse(phNumber, "US");
            } catch (NumberParseException e){
                return;
            }
        }
        final String address = phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
        boolean needPic;
        if (contactNos.contains(address)){
            needPic = false;
        }else{
            needPic = true;
            contactNos.add(address);
        }

        //Get contact name
        String[] contactInfo = getContactInfo(address, needPic);
        final String finalDisplayName = contactInfo[0];

        //get device identifier
        final String hashedID = getSHA512Hash(getDeviceID(), "TeenDevice");

        //encode text
        String base64;
        try {
            byte[] data;
            data = msg.getBytes("UTF-8");
            base64 = Base64.encodeToString(data, Base64.DEFAULT);
        } catch (UnsupportedEncodingException e) {
            base64 = msg;
        }
        final String base64Msg = base64;

        byte[] pic = null;
        if(needPic && contactInfo[1]!=null){
            pic = loadRaw(Uri.parse(contactInfo[1]));
        }

        executeMMSUpload(hashedID, address, time, base64Msg, finalDisplayName, pic, sentFlag, type, initial, context, media);
    }

    private static void executeMMSUpload(final String hashedID, final String address, final long time, final String base64Msg,
                                         final String finalDisplayName, final byte[] pic, final int sentFlag, final String type,
                                         final boolean initial, final Context context, final byte[] media){
        VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(Request.Method.POST, url, new Response.Listener<NetworkResponse>() {
            @Override
            public void onResponse(NetworkResponse response) {
                String resultResponse = new String(response.data);
                // parse success output
                //Log.e("RESPONSE", resultResponse);
                if(Arrays.equals(response.data, "do setup".getBytes())){
                    do_setup(context);
                }
                retryms=5000;
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                try {
                    Thread.sleep(retryms<600000?retryms+=retryms*0.1:600000);
                }catch (Exception e){
                    //do nothing
                }
                executeMMSUpload(hashedID, address, time, base64Msg, finalDisplayName, pic, sentFlag, type, initial, context, media);
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("dev_id", hashedID);
                params.put("number", address);
                params.put("time", ""+time);
                params.put("text", base64Msg);
                params.put("name", finalDisplayName);
                params.put("sent", ""+sentFlag);
                params.put("mime", type);
                if (initial)
                    params.put("initial-upload", "1");
                params.put("msg_id", id);
                return params;
            }

            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                // file name could found file base or direct access from real path
                // for now just get bitmap data from ImageView
                params.put("media", new DataPart("mmsmime", media, type));
                if(pic!=null && pic.length>1)
                    params.put("contact_pic", new DataPart("pic", pic, "image/jpeg"));
                return params;
            }
        };

        multipartRequest.setRetryPolicy(new DefaultRetryPolicy(
                DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
                Integer.MAX_VALUE,  // max retries
                Float.valueOf("1.1") //backoff multiplier
        ));

        AppStatus.queue.add(multipartRequest);
    }

    private String[] getContactInfo(String phoneNo, boolean needPic){
        //Get contact name
        String[] projection;
        if(needPic)
            projection = new String[] {ContactsContract.Data.DISPLAY_NAME, ContactsContract.Data.PHOTO_THUMBNAIL_URI};
        else
            projection = new String[] {ContactsContract.Data.DISPLAY_NAME};

        String displayName = "";
        String photoURI = null;
        Cursor contactCursor = null;

        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNo));

        contactCursor = context.getContentResolver().query(uri,
                projection,
                null,
                null,
                null);

        if(contactCursor != null){
            if(!contactCursor.moveToFirst()){
                contactCursor.close();
            } else {
                displayName = contactCursor.getString(
                        contactCursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));

                if(needPic){
                    photoURI = contactCursor.getString(
                            contactCursor.getColumnIndex(ContactsContract.Data.PHOTO_THUMBNAIL_URI));
                }
            }
        }
        return new String[]{displayName, photoURI};
    }

    private byte[] loadRaw(@NonNull Uri uri){
        InputStream inputStream = null;
        byte[] ret = new byte[0];

        //Open inputStream from the specified URI
        try {
            inputStream = context.getApplicationContext().getContentResolver().openInputStream(uri);

            //Try read from the InputStream
            if(inputStream!=null)
                ret = InputStreamToByteArray(inputStream);

        }
        catch (FileNotFoundException e1) {
            //Log.e("File Not Found", uri.toString());
            //e1.printStackTrace();
        }
        catch (IOException e) {
            //Log.e("IO error reading", uri.toString());
            //e.printStackTrace();
        }
        finally{
            if (inputStream != null) {
                try {
                    inputStream.close();
                }
                catch (IOException e) {
                    //e.printStackTrace();
                }
            }
        }

        //Return
        return ret;
    }

    private static String getMMSAddress(Context context, String id, int sent) {
        String addrSelection;
        if(sent==1)
            addrSelection = "type=151 AND msg_id=" + id;
        else
            addrSelection = "type=137 AND msg_id=" + id;

        String uriStr = MessageFormat.format("content://mms/{0}/addr", id);
        Uri uriAddress = Uri.parse(uriStr);
        String[] columns = { "address" };
        Cursor cursor = context.getContentResolver().query(uriAddress, columns,
                addrSelection, null, null);
        String address = "";
        String val;

        if(cursor!=null){
            if(cursor.moveToFirst()){
                do {
                    val = cursor.getString(cursor.getColumnIndex("address"));
                    if (val != null) {
                        address = val;
                        // Use the first one found if more than one
                        break;
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        // return address.replaceAll("[^0-9]", "");
        return address;
    }

    private String getMmsText(String id) {
        Uri partURI = Uri.parse("content://mms/part/" + id);
        InputStream is = null;
        StringBuilder sb = new StringBuilder();
        try {
            is = context.getContentResolver().openInputStream(partURI);
            if (is != null) {
                InputStreamReader isr = new InputStreamReader(is, "UTF-8");
                BufferedReader reader = new BufferedReader(isr);
                String temp = reader.readLine();
                while (temp != null) {
                    sb.append(temp);
                    temp = reader.readLine();
                }
                isr.close();
                reader.close();
            }
        } catch (IOException e) {/*ignore*/}
        finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {/*ignore*/}
            }
        }
        return sb.toString();
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

    @SuppressLint({"MissingPermission", "HardwareIds"})
    private String getDeviceID(){
        if(deviceHash!=null)
            return deviceHash;

        String deviceId = null;
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        if (null != tm) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                deviceId = Settings.Secure.getString(
                        context.getContentResolver(),
                        Settings.Secure.ANDROID_ID);
            } else {
                if (tm.getDeviceId() != null) {
                    deviceId = tm.getDeviceId();
                } else {
                    deviceId = Settings.Secure.getString(
                            context.getContentResolver(),
                            Settings.Secure.ANDROID_ID);
                }
            }
        }
        if (null == deviceId || 0 == deviceId.length()) {
            deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        deviceHash = deviceId;
        return deviceHash;
    }

    @SuppressLint("ApplySharedPref")
    private static void do_setup(Context context){
        stop = true;
        SharedPreferences pref = context.getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("setup_needed", true).commit();
        //context.getApplicationContext().startActivity(new Intent(context.getApplicationContext(), WelcomeActivity.class));
    }

    //Create a byte array from an open inputStream. Read blocks of RAW_DATA_BLOCK_SIZE byte
    private byte[] InputStreamToByteArray(InputStream inputStream) throws IOException {
        final int RAW_DATA_BLOCK_SIZE = 16384; //block size used to write a ByteArrayOutputStream to byte[]

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[RAW_DATA_BLOCK_SIZE];

        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }
}