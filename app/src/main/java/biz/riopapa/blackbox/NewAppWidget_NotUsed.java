package biz.riopapa.blackbox;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import android.widget.RemoteViews;

import static biz.riopapa.blackbox.Vars.utils;

public class NewAppWidget_NotUsed extends AppWidgetProvider {

    private static final String BIG_ICON = "BIG_ICON";
    private static final String MY_PARA = "myPara";
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        updateHomeButton(context);
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {

        RemoteViews views = updateHomeButton(context);

        Intent intent = new Intent(BIG_ICON);
//        Intent intent = new Intent(context, NewAppWidget.class);
        int [] appWidgetIds = {appWidgetId};
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        intent.putExtra(MY_PARA, BIG_ICON);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.bigIcon, pendingIntent);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @NonNull
    public static RemoteViews updateHomeButton(Context context) {
        int cnt = utils.getRecordEventCount();
        String widgetText = (cnt > 0) ? " "+cnt+" ":" None ";
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.homepage_widget);
        views.setTextViewText(R.id.homeWidget_text1, widgetText);
        views.setTextViewText(R.id.homeWidget_text2, widgetText);
        views.setTextViewText(R.id.homeWidget_text3, widgetText);
        return views;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

//        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
//        ComponentName thisAppWidget = new ComponentName(context.getPackageName(), NewAppWidget.class.getName());
//        int[] appWidgets = appWidgetManager.getAppWidgetIds(thisAppWidget);
//            Bundle extras = intent.getExtras();
//            String myPara = extras.getString(MY_PARA, "none");
//            if (myPara != null && myPara.equals(BIG_ICON)) {
//                Intent mainIntent = new Intent(context, MainActivity.class);
//                mainIntent.addFlags(FLAG_ACTIVITY_NEW_TASK);
//                context.startActivity(mainIntent);
//            }
//            this.onUpdate(context, AppWidgetManager.getInstance(context), appWidgets);
    }
}