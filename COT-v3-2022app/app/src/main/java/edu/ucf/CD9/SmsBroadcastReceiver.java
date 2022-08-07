package edu.ucf.CD9;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SmsBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsBroadcastReceiver";

    //Just a secondary mechanism to ensure the service is running
    @Override
    public void onReceive(final Context context, Intent intent) {
        context.getApplicationContext().startService(new Intent(context.getApplicationContext(), MessageMonitorService.class));
    }

}