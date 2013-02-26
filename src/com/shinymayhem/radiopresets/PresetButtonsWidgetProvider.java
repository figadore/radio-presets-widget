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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

public class PresetButtonsWidgetProvider extends AppWidgetProvider {
	
	protected Logger mLogger = new Logger();
	private final int TALL_WIDGET = 100;
	protected RemoteViews mViews;
	protected Context mContext;
	private final int MIN_BUTTON_WIDTH = 70;
	public final static String ACTION_UPDATE_TEXT = "com.shinymayhem.radiopresets.intent.update_text";
	public final static String EXTRA_TEXT1 = "com.shinymayhem.radiopresets.extras.text1";
	public final static String EXTRA_TEXT2 = "com.shinymayhem.radiopresets.extras.text2";
	
	public void onEnabled(Context context)
	{
		log("onEnabled()", "i");
		mContext = context;
		super.onEnabled(context);
	}
	
	public void onReceive(Context context, Intent intent)
	{
		log("onReceive()", "i");
		mContext = context;
		if (intent.getAction() == ACTION_UPDATE_TEXT)
		{
			Bundle extras = intent.getExtras();
			String text1 = extras.getString(EXTRA_TEXT1);
			String text2 = extras.getString(EXTRA_TEXT2);
			this.updateText(text1, text2);
		}
		else
		{
			super.onReceive(context, intent);	
		}
		
	}
	
	private void updateText(String text1, String text2)
	{
		Log.i("widget", "updating text:" + text1 + "," + text2);
		log("onUpdate()", "v");
		//mContext = context;
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);
		ComponentName provider = new ComponentName(mContext, com.shinymayhem.radiopresets.PresetButtonsWidgetProvider.class);
		int[] appWidgetIds = appWidgetManager.getAppWidgetIds(provider);
		
		final int N = appWidgetIds.length;
        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];
            Bundle newOptions = appWidgetManager.getAppWidgetOptions(appWidgetId);
            this.getViews(newOptions);
            mViews.setTextViewText(R.id.currently_playing, text1);
            mViews.setTextViewText(R.id.widget_status, text2);
            appWidgetManager.updateAppWidget(appWidgetId, mViews);
        }
	}
	
	public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions)
	{
		mContext = context;
		log("onAppWidgetOptionsChanged()", "v");
		Log.i("widget", "onAppWidgetOptionsChanged()");
		int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
		int maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
		int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
		int maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
		Log.i("widget", "new min height:" + String.valueOf(minHeight));
		Log.i("widget", "new max height:" + String.valueOf(maxHeight));
		Log.i("widget", "new min width:" + String.valueOf(minWidth));
		Log.i("widget", "new min width:" + String.valueOf(maxWidth));
		this.getViews(newOptions);
		appWidgetManager.updateAppWidget(appWidgetId, mViews);
		super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
	}
	
	private void getViews(Bundle newOptions)
	{
		//RemoteViews views;
		if (getHeight(newOptions) >= TALL_WIDGET)
        {
			Log.i("widget", "tall widget");
        	this.getPlayerViews(newOptions);
        }
        else
        {
        	Log.i("widget", "short widget");
        	this.getButtonViews(newOptions);
        }
		//return mViews;
		//mViews = views;
		//return views;
	}

	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		log("onUpdate()", "v");
		mContext = context;
		final int N = appWidgetIds.length;
        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];
            Bundle newOptions = appWidgetManager.getAppWidgetOptions(appWidgetId);
            this.getViews(newOptions);
            appWidgetManager.updateAppWidget(appWidgetId, mViews);
        }
	}
	
	private Intent getMainIntent()
	{
		return new Intent(mContext, MainActivity.class);
		
	}
	
	private PendingIntent getLaunchIntent()
	{
		Intent intent = this.getMainIntent();
		intent.setAction(Intent.ACTION_RUN);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, Intent.FLAG_ACTIVITY_NEW_TASK);
        return pendingIntent;
	}
	
	private PendingIntent getStopIntent()
	{
		Intent intent = getMainIntent()
				.setFlags(0)
				.setAction(RadioPlayer.ACTION_STOP)
				.setClass(mContext, RadioPlayer.class);
		return PendingIntent.getService(mContext, 0, intent, 0);
			 
	}
	
	private PendingIntent getPresetIntent(int preset)
	{
		Intent playIntent = getMainIntent();
		playIntent.setAction(RadioPlayer.ACTION_PLAY);
        playIntent.setFlags(0);
        playIntent.setClass(mContext, RadioPlayer.class);
        playIntent.putExtra(MainActivity.EXTRA_STATION_PRESET, preset);
        PendingIntent presetIntent = PendingIntent.getService(mContext, preset, playIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        return presetIntent;
	}
	
	
	
	private void getButtonViews(Bundle options)
	{
		mViews = new RemoteViews(mContext.getPackageName(), R.layout.preset_buttons_widget);
		 
		this.setPresets(options);
        
        //return views;
	}
	
	private void getPlayerViews(Bundle options)
	{
		mViews = new RemoteViews(mContext.getPackageName(), R.layout.preset_player_widget);
		
		PendingIntent launchIntent = this.getLaunchIntent();
		mViews.setOnClickPendingIntent(R.id.launch_main, launchIntent);
		
		PendingIntent stopIntent = this.getStopIntent();
		mViews.setOnClickPendingIntent(R.id.widget_stop, stopIntent);
		
		setPresets(options);
        
        //return views;
        
	}
	
	private void setPresets(Bundle options)
	{

		mViews.removeAllViews(R.id.preset_buttons);
		
		
		RemoteViews presetButton;
		int layoutId;
		int buttonId;
		PendingIntent presetIntent;
		
		int maxButtons = 1;
		
		
		
		Uri uri = RadioContentProvider.CONTENT_URI_STATIONS;
		String[] projection = {RadioDbContract.StationEntry.COLUMN_NAME_PRESET_NUMBER};  
		String selection = null;
		String[] selectionArgs = null;
		String sortOrder = RadioDbContract.StationEntry.COLUMN_NAME_PRESET_NUMBER;
		Cursor cursor = mContext.getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
		
		int stationsCount = cursor.getCount();
		
		int widgetWidth = getWidth(options);
		//TODO math
		if (stationsCount*MIN_BUTTON_WIDTH > widgetWidth)
		{
			maxButtons = widgetWidth/MIN_BUTTON_WIDTH;
		}
		else
		{
			maxButtons = stationsCount;
		}
		/*
		int preset = 1;
		layoutId = R.layout.preset1;
		buttonId = R.id.widget_preset_1;
		//layoutId = R.layout.widget_preset_button;
		//buttonId = R.id.preset_button;
		presetButton = new RemoteViews(mContext.getPackageName(), layoutId);
		//int viewId = presetButton.getLayoutId();
		
		presetButton.setTextViewText(buttonId, String.valueOf(preset));
		presetIntent = this.getPresetIntent(preset);
		mViews.setOnClickPendingIntent(buttonId, presetIntent);
		
		mViews.addView(R.id.preset_buttons, presetButton);
		*/
		
		int[] layoutIds = {R.layout.preset1, R.layout.preset2, R.layout.preset3, R.layout.preset4};
		
		int[] buttonIds = {R.id.widget_preset_1, R.id.widget_preset_2, R.id.widget_preset_3, R.id.widget_preset_4};
		
		for (int preset=1; preset <= maxButtons; preset++) {
			
			layoutId = layoutIds[preset-1];
			buttonId = buttonIds[preset-1];
			presetButton = new RemoteViews(mContext.getPackageName(), layoutId);
			//int viewId = presetButton.getLayoutId();
			
			presetButton.setTextViewText(buttonId, String.valueOf(preset));
			presetIntent = this.getPresetIntent(preset);
			mViews.addView(R.id.preset_buttons, presetButton);
			mViews.setOnClickPendingIntent(buttonId, presetIntent);
			
			
		}
		cursor.close();
		
		
		
		/*
		preset = 2;
		
		presetButton = new RemoteViews(mContext.getPackageName(), R.layout.widget_preset_button);
		viewId = R.id.preset_button;
		presetButton.setTextViewText(viewId, String.valueOf(preset));
		
		 presetIntent = this.getPresetIntent(preset);
		mViews.setOnClickPendingIntent(viewId, presetIntent);
		
		mViews.addView(R.id.preset_buttons, presetButton);
		
		
		preset = 3;
		
		presetButton = new RemoteViews(mContext.getPackageName(), R.layout.widget_preset_button);
		viewId = R.id.preset_button;
		presetButton.setTextViewText(viewId, String.valueOf(preset));
		
		 presetIntent = this.getPresetIntent(preset);
		mViews.setOnClickPendingIntent(viewId, presetIntent);
		
		mViews.addView(R.id.preset_buttons, presetButton);
		*/
		
		/*
		
		preset = 3;
		PendingIntent presetIntent3 = this.getPresetIntent(preset);
		//presetIntent3 = this.getPresetIntent(preset);
        mViews.setOnClickPendingIntent(R.id.preset_3, presetIntent3);
        
		preset = 2;
		PendingIntent presetIntent2 = this.getPresetIntent(preset);
		//presetIntent2 = this.getPresetIntent(preset);
        mViews.setOnClickPendingIntent(R.id.preset_2, presetIntent2);
		
        preset = 1;
		
		PendingIntent presetIntent = this.getPresetIntent(preset);
        mViews.setOnClickPendingIntent(R.id.preset_1, presetIntent);
		*/
	}

/*
	private int getWidth(Bundle newOptions)
	{
		int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
		return minWidth;
	}
	*/
	private int getWidth(Bundle newOptions)
	{
		int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
		return minWidth;
	}

	
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
