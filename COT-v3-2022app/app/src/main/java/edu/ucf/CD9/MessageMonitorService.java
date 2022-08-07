package edu.ucf.CD9;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class MessageMonitorService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        String hashedID = pref.getString("hashedID", null);
        if(hashedID!=null){

        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        String NOTIFICATION_CHANNEL_ID = "edu.ucf.CD9.ch1";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Notifications", NotificationManager.IMPORTANCE_NONE);

            // Configure the notification channel.
            notificationChannel.setDescription("Channel description");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            notificationChannel.enableVibration(true);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(notificationChannel);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);



        notificationBuilder.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                //.setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher_round)
                //.setContentTitle("")
                .setContentText("Parental Monitoring is Active")
                .setContentInfo("Info")
                .setContentIntent(pendingIntent);

        if(Build.VERSION.SDK_INT<16){
            notificationBuilder.setPriority(1);
        }
        else{
            notificationBuilder.setPriority(NotificationManager.IMPORTANCE_HIGH);
        }

        Notification notification = notificationBuilder.build();

        startForeground(1, notification);

        Log.v("Debug", "MMS SMS MONITOR service created.........");
    }

    public void registerObserver() {
        getContentResolver().registerContentObserver(
                Uri.parse("content://mms-sms/conversations/"),
                true, new SMSObserver(new Handler()));
        Log.v("Debug", " in registerObserver method.........");
    }

    //start the service and register observer for lifetime
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v("Debug", "Service has been started..");
        Toast.makeText(getApplicationContext(),
                "Service has been started.. ",
                Toast.LENGTH_SHORT).show();
        registerObserver();

        return START_STICKY;
    }

    class SMSObserver extends ContentObserver {

        SMSObserver(Handler handler) {
            super(handler);
        }

        //will be called when database get change
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Log.v("Debug", "SMS/MMS Activity Detected");

            UploadTexts ut = new UploadTexts();
            ut.UploadTexts(getApplicationContext(), false, 0, null, null);

        }
    }
}