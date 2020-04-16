package com.urrecliner.blackbox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Timer;
import java.util.TimerTask;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final Context ctx = context;
        String model = Build.MODEL;
        if (model.equals("Nexus 6P")) {
            new Timer().schedule(new TimerTask() {
                public void run() {
                    Intent i = new Intent(ctx, MainActivity.class);
                    ctx.startActivity(i);
                }
            }, 5000);
        }
    }
}
