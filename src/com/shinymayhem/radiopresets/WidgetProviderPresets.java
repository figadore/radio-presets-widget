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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import com.shinymayhem.radiopresets.ServiceRadioPlayer.LocalBinder;

public class WidgetProviderPresets extends AppWidgetProvider {
	
	protected ActivityLogger mLogger = new ActivityLogger();
	private final int TALL_WIDGET = 100;
	protected RemoteViews mViews;
	protected Context mContext;
	protected int mPreset = 0;
	private final int MIN_BUTTON_WIDTH = 65; //TODO get from preferences
	private final int FIXED_WIDTH = 294;
	private final int FIXED_HEIGHT = 100;
	private final int[] layoutIds = {R.layout.preset1, R.layout.preset2, R.layout.preset3, R.layout.preset4, R.layout.preset5, R.layout.preset6, R.layout.preset7, R.layout.preset8};
	private final int[] selectedLayoutIds = {R.layout.preset1_selected, R.layout.preset2_selected, R.layout.preset3_selected, R.layout.preset4_selected, R.layout.preset5_selected, R.layout.preset6_selected, R.layout.preset7_selected, R.layout.preset8_selected};
	private final int[] buttonIds = {R.id.widget_preset_1, R.id.widget_preset_2, R.id.widget_preset_3, R.id.widget_preset_4, R.id.widget_preset_5, R.id.widget_preset_6, R.id.widget_preset_7, R.id.widget_preset_8};
	protected ServiceRadioPlayer mService;
	private ServiceConnection mConnection = new ServiceConnection()
	{
		@Override
		public void onServiceConnected(ComponentName className, IBinder service)
		{
			log("service connected", "d");
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService();
			mService.updateDetails();
			mContext.getApplicationContext().unbindService(mConnection);
		}
		@Override
		public void onServiceDisconnected(ComponentName arg0)
		{
			log("service disconnected", "d");
		}
	};
	
	
	public void onEnabled(Context context)
	{
		
		mContext = context;
		log("onEnabled()", "v");
		super.onEnabled(context);
		updateDetails(context);
	}
	
	public void onReceive(Context context, Intent intent)
	{
		
		mContext = context;
		log("onReceive()", "v");
		super.onReceive(context, intent);	
		String action = intent.getAction();
		log(action,"v");
		log(ActivityMain.ACTION_UPDATE_TEXT,"v");
		if (action.equals(ActivityMain.ACTION_UPDATE_TEXT))
		{
			log("update text action", "v");
			Bundle extras = intent.getExtras();
			int preset = extras.getInt(ActivityMain.EXTRA_PRESET);
			String station = extras.getString(ActivityMain.EXTRA_STATION);
			String status = extras.getString(ActivityMain.EXTRA_STATUS);
			String artist = extras.getString(ActivityMain.EXTRA_ARTIST);
			String song = extras.getString(ActivityMain.EXTRA_SONG);
			this.updateText(station, status, artist, song, preset);
		}
		else
		{
			if (intent.getAction() == null)
			{
				log("no action", "v");	
			}
			else
			{
				log("other action:" + String.valueOf(intent.getAction()), "v");	
			}
			
			
		}
		
	}
	
	@SuppressLint("NewApi")
	private void updateText(String station, String status, String artist, String song, int preset)
	{
		log("updating text:" + station + "," + status, "v");
		
		if (status.equals(mContext.getResources().getString(R.string.status_stopped)))
		{
			mPreset = 0;
		}
		else
		{
			mPreset = preset;
		}
		
		//mContext = context;
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);
		ComponentName provider = new ComponentName(mContext, com.shinymayhem.radiopresets.WidgetProviderPresets.class);
		int[] appWidgetIds = appWidgetManager.getAppWidgetIds(provider);
		
		final int N = appWidgetIds.length;
		boolean playing = status.equals(mContext.getResources().getString(R.string.status_playing));
        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];
            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN)
            {
            	Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
                this.getViews(options);	
            }
            else
            {
            	this.getViews();
            }
            
            mViews.setTextViewText(R.id.currently_playing, station);
            if (playing)
            {
            	mViews.setTextViewText(R.id.widget_status, status + ":" + artist + " - " + song);	
            }
            else
            {
            	mViews.setTextViewText(R.id.widget_status, status);
            }
            //appWidgetManager.updateAppWidget(provider, mViews);
            appWidgetManager.updateAppWidget(appWidgetId, mViews);
        }
	}
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
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
		//update widget playing details
		
		updateDetails(context);
		//super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
	}
	
	private void updateDetails(Context context)
	{
		Intent intent = new Intent(context, ServiceRadioPlayer.class);
		intent.setAction(Intent.ACTION_RUN);
		context.getApplicationContext().bindService(intent, mConnection, 0); //should unbind itself once connected and details intent sent
		//mService.updateDetails();
		//context.unbindService(mConnection);
	}
	
	//default for pre-jellybean, not resizeable, stick with 2x4 
	private void getViews()
	{
		Bundle options = new Bundle();
		options.putInt("appWidgetMinWidth", MIN_BUTTON_WIDTH*4);
		this.getPlayerViews(options);
	}
	
	private void getViews(Bundle newOptions)
	{
		//RemoteViews views;
		if (getHeight(newOptions) >= TALL_WIDGET)
        {
			log("tall widget", "v");
        	this.getPlayerViews(newOptions);
        }
        else
        {
        	log("short widget", "v");
        	this.getButtonViews(newOptions);
        }
		//return mViews;
		//mViews = views;
		//return views;
	}

	@SuppressLint("NewApi")
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		mContext = context;
		log("onUpdate()", "v");
		final int N = appWidgetIds.length;
        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];
            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN)
            {
            	Bundle newOptions = appWidgetManager.getAppWidgetOptions(appWidgetId);
                this.getViews(newOptions);	
            }
            else
            {
            	this.getViews();
            }
            appWidgetManager.updateAppWidget(appWidgetId, mViews);
        }
        updateDetails(context);
	}
	
	private Intent getMainIntent()
	{
		return new Intent(mContext, ActivityMain.class);
		
	}
	
	private PendingIntent getLaunchIntent()
	{
		Intent intent = this.getMainIntent()
				.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP)
				.setAction(Intent.ACTION_RUN)
				.setClass(mContext, ActivityMain.class);
        //intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0); //Intent.FLAG_ACTIVITY_NEW_TASK
        return pendingIntent;
	}
	
	private PendingIntent getPreviousIntent()
	{
		Intent intent = getMainIntent()
				.setFlags(0)
				.setAction(ServiceRadioPlayer.ACTION_PREVIOUS)
				.setClass(mContext, ServiceRadioPlayer.class);
		return PendingIntent.getService(mContext, 0, intent, 0);
			 
	}
	
	private PendingIntent getStopIntent()
	{
		Intent intent = getMainIntent()
				.setFlags(0)
				.setAction(ServiceRadioPlayer.ACTION_STOP)
				.setClass(mContext, ServiceRadioPlayer.class);
		return PendingIntent.getService(mContext, 0, intent, 0);
			 
	}
	
	private PendingIntent getNextIntent()
	{
		Intent intent = getMainIntent()
				.setFlags(0)
				.setAction(ServiceRadioPlayer.ACTION_NEXT)
				.setClass(mContext, ServiceRadioPlayer.class);
		return PendingIntent.getService(mContext, 0, intent, 0);
			 
	}
	
	
	private PendingIntent getPresetIntent(int preset)
	{
		Intent playIntent = getMainIntent()
				.setAction(ServiceRadioPlayer.ACTION_PLAY)
				.setFlags(0)
				.setClass(mContext, ServiceRadioPlayer.class);
        playIntent.putExtra(ActivityMain.EXTRA_STATION_PRESET, preset);
        PendingIntent presetIntent = PendingIntent.getService(mContext, preset, playIntent, PendingIntent.FLAG_UPDATE_CURRENT); //cancel current sometimes fails. test PendingIntent.FLAG_UPDATE_CURRENT if 0 doesn't work 
        return presetIntent;
	}
	
	
	
	private void getButtonViews(Bundle options)
	{
		mViews = new RemoteViews(mContext.getPackageName(), R.layout.preset_buttons_widget);
		 
		PendingIntent launchIntent = this.getLaunchIntent();
		mViews.setOnClickPendingIntent(R.id.launch_main, launchIntent);
		
		PendingIntent stopIntent = this.getStopIntent();
		mViews.setOnClickPendingIntent(R.id.widget_stop, stopIntent);
		
		this.setPresets(options);
        
        //return views;
	}
	
	private void getPlayerViews(Bundle options)
	{
		mViews = new RemoteViews(mContext.getPackageName(), R.layout.preset_player_widget);
		
		PendingIntent launchIntent = this.getLaunchIntent();
		mViews.setOnClickPendingIntent(R.id.launch_main, launchIntent);
		
		PendingIntent previousIntent = this.getPreviousIntent();
		mViews.setOnClickPendingIntent(R.id.widget_previous, previousIntent);
		
		PendingIntent stopIntent = this.getStopIntent();
		mViews.setOnClickPendingIntent(R.id.widget_stop, stopIntent);
		
		PendingIntent nextIntent = this.getNextIntent();
		mViews.setOnClickPendingIntent(R.id.widget_next, nextIntent);
		
		setPresets(options);
        
        //return views;
        
	}
	
	private void setPresets(Bundle options)
	{
		log("setPresets()", "v");
		mViews.removeAllViews(R.id.preset_buttons);
		
		RemoteViews presetButton;
		int layoutId;
		int buttonId;
		PendingIntent presetIntent;
		
		int maxButtons = 1;

		Uri uri = ContentProviderRadio.CONTENT_URI_STATIONS;
		String[] projection = {DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER};  
		String selection = null;
		String[] selectionArgs = null;
		String sortOrder = DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER;
		Cursor cursor = mContext.getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
		
		int stationsCount = cursor.getCount();
		
		int widgetWidth = getWidth(options);
		if (stationsCount*MIN_BUTTON_WIDTH > widgetWidth)
		{
			maxButtons = widgetWidth/MIN_BUTTON_WIDTH;
		}
		else
		{
			maxButtons = stationsCount;
		}
		//int playingPreset = 2;

		for (int preset=1; preset <= maxButtons; preset++) {
			if (mPreset == preset)
			{
				layoutId = selectedLayoutIds[preset-1];
			}
			else
			{
				layoutId = layoutIds[preset-1];
			}
			
			buttonId = buttonIds[preset-1];
			presetButton = new RemoteViews(mContext.getPackageName(), layoutId);
			//int viewId = presetButton.getLayoutId();
			
			presetButton.setTextViewText(buttonId, String.valueOf(preset));
			presetIntent = this.getPresetIntent(preset);
			mViews.addView(R.id.preset_buttons, presetButton);
			mViews.setOnClickPendingIntent(buttonId, presetIntent);
			
			
		}
		cursor.close();
	}

/*
	private int getWidth(Bundle newOptions)
	{
		int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
		return minWidth;
	}
	*/
	@SuppressLint("InlinedApi")
	private int getWidth(Bundle newOptions)
	{
		if (newOptions != null)
		{
			//in portrait
			int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
			//in landscape
			//int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);

			return minWidth;
		}
		return FIXED_WIDTH;

	}

	
	@SuppressLint("InlinedApi")
	private int getHeight(Bundle newOptions)
	{
		if (newOptions != null)
		{
			//in landscape
			int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
			//in portrait
			//int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
			return minHeight;
		}
		return FIXED_HEIGHT;
	}

	
	private void log(String text, String level)
	{
		//mLogger.log(mContext, text, level);
		/*String callerClass = "WidgetProvider";
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
		}*/
	}
	
}
