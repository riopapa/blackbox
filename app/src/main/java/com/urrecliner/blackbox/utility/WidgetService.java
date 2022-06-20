package com.urrecliner.blackbox.utility;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import com.urrecliner.blackbox.R;

public class WidgetService extends Service {
    final String TAG = WidgetService.class.getSimpleName();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String maxSpeed = "999";
        try {
            maxSpeed = intent.getStringExtra("maxSpeed");
        } catch (Exception e) {
            Log.e("maxSpeed "+maxSpeed,e.toString());
        }
        RemoteViews updateViews = new RemoteViews(this.getPackageName(), R.layout.widget);

        updateViews.setTextViewText(R.id.widget_text, maxSpeed);
//        updateViews.setOnClickPendingIntent(R.id.widget_text, buildStartIntent(this)); // 클릭 펜딩인텐트.

        ComponentName componentname = new ComponentName(this, WidgetProvider.class);
        AppWidgetManager appwidgetmanager = AppWidgetManager.getInstance(this);
        appwidgetmanager.updateAppWidget(componentname, updateViews);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }
}
//    // 클릭 시 앱 실행 펜딩 이벤트.
//    private PendingIntent buildStartIntent(Context context) {
//        Intent i = new Intent(Intent.ACTION_MAIN);
//        i.addCategory(Intent.CATEGORY_LAUNCHER);
//        i.setComponent(new ComponentName(context, SplashActivity.class));
//        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, i, 0);
//        return pendingIntent;
//    }