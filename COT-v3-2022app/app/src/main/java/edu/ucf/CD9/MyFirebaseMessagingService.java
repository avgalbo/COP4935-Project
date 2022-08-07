package edu.ucf.CD9;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.installations.FirebaseInstallations;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Created by abhiditya
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // ...
        String TAG = "firebase";
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

//            if (/* Check if data needs to be processed by long running job */ true) {
//                // For long-running tasks (10 seconds or more) use Firebase Job Dispatcher.
//                scheduleJob();
//            } else {
//                // Handle message within 10 seconds
//                handleNow();
            if(remoteMessage.getData().containsKey("pairing")){
                if(remoteMessage.getData().containsValue("success")){
                    Log.d(TAG, "pairing success");
                    if(AppStatus.isWelcomeActivityForeground()){
                        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("otp-pairing-success"));
                    } else {
                        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0);
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putBoolean("setup_needed", false).apply();
                        startActivity(new Intent(getApplicationContext(), MainActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    }
                }
            }

            if(remoteMessage.getData().containsKey("initcot")){
                if(remoteMessage.getData().containsValue("success")){
                    if(AppStatus.isInitialSelectTrustForeground()){
                        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("initial-cot-success"));
                    }
                    else if(AppStatus.isChildTutorialForeground()){
                        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("initial-cot-success"));
                    }
                }
            }
//            }
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            String tag = remoteMessage.getNotification().getTag();
            if(AppStatus.isMainActivityForeground()){
                if(remoteMessage.getNotification().getTitle()!=null && remoteMessage.getNotification().getTitle().contains("Texting Activity on ")){
                    LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent
                            ("refresh-text-counts").putExtra("body", remoteMessage.getNotification().getBody()));
                } else if(remoteMessage.getData().containsValue("cot")){
                    LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent
                            ("refresh-cot").putExtra("body", remoteMessage.getNotification().getBody()));
                    LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent
                            ("refresh-text-counts").putExtra("body", remoteMessage.getNotification().getBody()));
                }
                else {
                    LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent
                            ("refresh-apps").putExtra("body", remoteMessage.getNotification().getBody()));
                }
                sendNotification(title, body, tag);
            } else if(AppStatus.isAnalysisActivityForeground()){
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent
                        ("refresh-cot").putExtra("body", remoteMessage.getNotification().getBody()));
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent
                        ("refresh-text-counts").putExtra("body", remoteMessage.getNotification().getBody()));
            }
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }
    @Override
    public void onNewToken(String token){
        super.onNewToken(token);
        Log.d("NEW_TOKEN",token);
    }

    public static String getToken(Context context) {
        return context.getSharedPreferences("_", MODE_PRIVATE).getString("fcm_token", "empty");
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private void sendNotification(String messageTitle, String messageBody, String messageTag) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 , intent,
                PendingIntent.FLAG_ONE_SHOT);

        String channelId = getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(messageTitle)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Parent Dashboard",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(1000 , notificationBuilder.build());
    }
}