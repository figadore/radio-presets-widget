/*
 * Copyright (C) 2013 Reese Wilson | Shiny Mayhem

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.shinymayhem.radiopresets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

public class PresetButtonsWidgetProvider extends AppWidgetProvider {
	
	protected Logger mLogger = new Logger();
	private final int TALL_WIDGET = 100;
	
	public void onEnabled(Context context)
	{
		log("onEnabled()", "i");
		super.onEnabled(context);
	}
	
	public void onReceive(Context context, Intent intent)
	{
		log("onReceive()", "i");
		super.onReceive(context, intent);
	}
	
	public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions)
	{
		log("onAppWidgetOptionsChanged()", "v");
		Log.i("widget", "onAppWidgetOptionsChanged()");
		/*int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
		int maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
		int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
		int maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
		Log.i("widget", "new min height:" + String.valueOf(minHeight));
		Log.i("widget", "new max height:" + String.valueOf(maxHeight));
		Log.i("widget", "new min width:" + String.valueOf(minWidth));
		Log.i("widget", "new min width:" + String.valueOf(maxWidth));*/
		appWidgetManager.updateAppWidget(appWidgetId, getViews(context, newOptions));
		super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
	}
	
	private RemoteViews getViews(Context context, Bundle newOptions)
	{
		RemoteViews views;
		if (getHeight(newOptions) >= TALL_WIDGET)
        {
        	views = this.getPlayerViews(context);
        }
        else
        {
        	views = this.getButtonViews(context);
        }
		return views;
	}

	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		log("onUpdate()", "v");
		final int N = appWidgetIds.length;
		
        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];
            Bundle newOptions = appWidgetManager.getAppWidgetOptions(appWidgetId);
            appWidgetManager.updateAppWidget(appWidgetId, getViews(context, newOptions));
        }
	}
	
	private Intent getMainIntent(Context context)
	{
		return new Intent(context, MainActivity.class);
		
	}
	
	private PendingIntent getLaunchIntent(Context context)
	{
		Intent intent = this.getMainIntent(context);
		intent.setAction(Intent.ACTION_RUN);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, Intent.FLAG_ACTIVITY_NEW_TASK);
        return pendingIntent;
	}
	
	private PendingIntent getStopIntent(Context context)
	{
		Intent intent = getMainIntent(context)
				.setFlags(0)
				.setAction(RadioPlayer.ACTION_STOP)
				.setClass(context, RadioPlayer.class);
		return PendingIntent.getService(context, 0, intent, 0);
			 
	}
	
	private PendingIntent getPresetIntent(Context context, int preset)
	{
		Intent playIntent = getMainIntent(context);
		playIntent.setAction(RadioPlayer.ACTION_PLAY);
        playIntent.setFlags(0);
        playIntent.setClass(context, RadioPlayer.class);
        playIntent.putExtra(MainActivity.EXTRA_STATION_PRESET, preset);
        PendingIntent presetIntent = PendingIntent.getService(context, 0, playIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        return presetIntent;
	}
	
	
	
	private RemoteViews getButtonViews(Context context)
	{
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.preset_buttons_widget);
		
		//TODO iterate from query
		int preset = 1;
		{
		PendingIntent presetIntent = this.getPresetIntent(context, preset);
        views.setOnClickPendingIntent(R.id.preset_1, presetIntent);
		}
        
        return views;
	}
	
	private RemoteViews getPlayerViews(Context context)
	{
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.preset_player_widget);
		
		PendingIntent launchIntent = this.getLaunchIntent(context);
		views.setOnClickPendingIntent(R.id.launch_main, launchIntent);
		
		PendingIntent stopIntent = this.getStopIntent(context);
		views.setOnClickPendingIntent(R.id.widget_stop, stopIntent);
		
		//TODO iterate from query
		int preset = 1;
		{
		PendingIntent presetIntent = this.getPresetIntent(context, preset);
	    views.setOnClickPendingIntent(R.id.preset_1, presetIntent);
		}
        
        return views;
        
	}

/*
	private int getWidth(Bundle newOptions)
	{
		int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
		return minWidth;
	}
	*/
	private int getHeight(Bundle newOptions)
	{
		int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
		return minHeight;
	}

	
	private void log(String text, String level)
	{
		//mLogger.log(this.get, text, level);
		String callerClass = "WidgetProvider";
		String str = text;
		if (level == "v")
		{
			str = "VERBOSE:\t\t" + str;
			Log.v(callerClass, str);
		}
		else if (level == "d")
		{
			str = "DEBUG:\t\t" + str;
			Log.d(callerClass, str);
		}
		else if (level == "i")
		{
			str = "INFO:\t\t" + str;
			Log.i(callerClass, str);
		}
		else if (level == "w")
		{
			str = "WARN:\t\t" + str;
			Log.w(callerClass, str);
		}
		else if (level == "e")
		{
			str = "ERROR:\t\t" + str;
			Log.e(callerClass, str);
		}
	}
	
}
