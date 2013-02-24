package com.shinymayhem.radiopresets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

public class PresetButtonsWidgetProvider extends AppWidgetProvider {
	
	public void onEnabled(Context context)
	{
		Log.i("widget", "onEnabled()");
		super.onEnabled(context);
	}
	
	public void onReceive(Context context, Intent intent)
	{
		Log.i("widget", "onReceive()");
		super.onReceive(context, intent);
	}
	
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		Log.i("widget", "onUpdate()");
		final int N = appWidgetIds.length;

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];
            
            
         // Create an Intent to launch ExampleActivity
            Intent intent = new Intent(context, MainActivity.class);
            intent.setAction(Intent.ACTION_RUN);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP| Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, Intent.FLAG_ACTIVITY_NEW_TASK);
            
            // Get the layout for the App Widget and attach an on-click listener
            // to the button
            Intent playIntent = new Intent(context, MainActivity.class);
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.preset_buttons_widget);
            views.setOnClickPendingIntent(R.id.launch_main, pendingIntent);
            int preset = 1;
            playIntent.setAction(RadioPlayer.ACTION_PLAY);
            playIntent.setFlags(0);
            playIntent.setClass(context, RadioPlayer.class);
            playIntent.putExtra(MainActivity.EXTRA_STATION_PRESET, preset);
            Log.i("widget", "widget sending play intent to main activity with preset " + String.valueOf(preset));
            PendingIntent presetIntent = PendingIntent.getService(context, 0, playIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            
            
            views.setOnClickPendingIntent(R.id.preset_1, presetIntent);
            
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
	}

}
