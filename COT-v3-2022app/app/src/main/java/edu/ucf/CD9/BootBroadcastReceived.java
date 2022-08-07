package edu.ucf.CD9;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by abhiditya
 */

public class BootBroadcastReceived extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.getApplicationContext()
                .startService(new Intent(context.getApplicationContext(),
                        MessageMonitorService.class));
    }
}
