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



import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteException;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.shinymayhem.radiopresets.DbContractRadio.DbHelperRadio;
import com.shinymayhem.radiopresets.DialogFragmentAdd.ListenerAddDialog;
import com.shinymayhem.radiopresets.DialogFragmentEvent.ListenerEventDialog;
import com.shinymayhem.radiopresets.FragmentPlayer.PlayerListener;
import com.shinymayhem.radiopresets.FragmentStations.PresetListener;
import com.shinymayhem.radiopresets.ServiceRadioPlayer.LocalBinder;

public class ActivityMain extends FragmentActivity implements ListenerAddDialog, ListenerEventDialog, PresetListener, PlayerListener {

	//string-extra key for intent
	//public final static String URL = "com.shinymayhem.radiopresets.URL";
	public final static String EXTRA_STATION_PRESET = "com.shinymayhem.radiopresets.STATION_ID";
	
	public final static String ACTION_UPDATE_TEXT = "com.shinymayhem.radiopresets.mainactivity.ACTION_UPDATE_TEXT";
	public final static String EXTRA_STATION = "com.shinymayhem.radiopresets.mainactivity.EXTRA_STATION";
	public final static String EXTRA_STATUS = "com.shinymayhem.radiopresets.mainactivity.EXTRA_STATUS";
	public final static String EXTRA_ARTIST = "com.shinymayhem.radiopresets.mainactivity.EXTRA_ARTIST";
	public final static String EXTRA_SONG = "com.shinymayhem.radiopresets.mainactivity.EXTRA_SONG";
	public final static String EXTRA_PRESET = "com.shinymayhem.radiopresets.mainactivity.EXTRA_PRESET";
	
	//public static final int BUTTON_LIMIT = 25;
	public static final int LOADER_STATIONS = 0;
	public final static String LOG_FILENAME = "log.txt";
	
	protected boolean mBound = false;
	//protected DbHelperRadio mDbHelper;
	protected ServiceRadioPlayer mService;
	protected DbHelperRadio mDbHelper;
	protected ActivityLogger mLogger = new ActivityLogger();
	protected ReceiverDetails mDetailsReceiver;
	
	private ServiceConnection mConnection = new ServiceConnection()
	{
		@Override
		public void onServiceConnected(ComponentName className, IBinder service)
		{
			log("service connected", "d");
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService();
			mBound = true;
			updateDetails();
		}
		@Override
		public void onServiceDisconnected(ComponentName arg0)
		{
			log("service disconnected", "d");
			mBound = false;
		}
	};
	
	
	protected Context getContext()
	{
		return this;
	}
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		log("creating main activity", "v");
		
		//set content view first so findViewById works
		setContentView(R.layout.activity_main);
		
		if (findViewById(R.id.stations_fragment_container) != null) {
			if (savedInstanceState != null) { //don't create overlapping fragments
                return;
            }
			
			FragmentPlayer playerFragment = new FragmentPlayer();
			FragmentStations stationsFragment = new FragmentStations();
			playerFragment.setArguments(getIntent().getExtras());
			stationsFragment.setArguments(getIntent().getExtras());
			
			FragmentManager fragmentManager = getSupportFragmentManager();
			fragmentManager.beginTransaction()
					.add(R.id.stations_fragment_container, stationsFragment, FragmentStations.FRAGMENT_TAG)
					.add(R.id.player_fragment_container, playerFragment, FragmentPlayer.FRAGMENT_TAG)
					.commit();
		}
		
		
	}
	
	@Override
	protected void onStart()
	{
		super.onStart();
		log("starting main activity", "v");
		bindRadioPlayer();
	}
	
	protected void bindRadioPlayer()
	{
		log("binding radio player", "d");
		Intent intent = new Intent(this, ServiceRadioPlayer.class);
		intent.setAction(Intent.ACTION_RUN);
		startService(intent);
		
		//don't call service's onStartCommand, just connect to it so play(url) and other functions are available through ui
		//bind_above_client so ui might be killed before service, in case of low memory 
		//bindService(intent, mConnection, Context.BIND_AUTO_CREATE|Context.BIND_ABOVE_CLIENT);
		//bindService(intent, mConnection, Context.BIND_ABOVE_CLIENT); 
		bindService(intent, mConnection, 0);
	}
	
	protected void onRestart()
	{
		super.onRestart();
		log("restarting main activity", "v");
	}
	
	protected void onResume()
	{
		super.onResume();
		log("resuming main activity", "v");
		//while app is visible, volume buttons should adjust music stream volume
		log("setting volume control stream", "v");
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		
		registerDetailsReceiver();
		
		//update player details
		//if (mService == null)
		//{
//			log("Service not bound yet", "e");
	//	}
		//mService.updateDetails();
	}
	
	//register for details updates
	private void registerDetailsReceiver()
	{
		//register network receiver
		IntentFilter filter = new IntentFilter(ActivityMain.ACTION_UPDATE_TEXT);
		if (mDetailsReceiver != null)
		{
			log("------------------------------------------", "v");
			log("details receiver already registered, find out why", "w"); 
			log("------------------------------------------", "v");
			//widget doesn't accept localbroadcasts
			//LocalBroadcastManager.getInstance(this).unregisterReceiver(mDetailsReceiver);
			this.unregisterReceiver(mDetailsReceiver);
			mDetailsReceiver = null;
		}
		mDetailsReceiver = new ReceiverDetails();
        log("registering details broadcast receiver", "i");
        //widget doesn't accept localbroadcasts
        //LocalBroadcastManager.getInstance(this).registerReceiver(mDetailsReceiver, filter); 
		this.registerReceiver(mDetailsReceiver, filter);
	}
	
	private void unregisterDetailsReceiver()
	{
		if (mDetailsReceiver == null)
		{
			log("mDetailsReceiver null, probably already unregistered", "v");
		}
		else
		{
			try
			{
				//widget doesn't accept localbroadcasts
				//LocalBroadcastManager.getInstance(this).unregisterReceiver(mDetailsReceiver);
				this.unregisterReceiver(mDetailsReceiver);
				log("unregistering details receiver", "v");
			}
			catch (IllegalArgumentException e)
			{
				log("details receiver already unregistered", "w");
			}
			mDetailsReceiver = null;	
		}
	}
	
	@Override
	protected void onStop()
	{
		log("stopping main activity", "v");
		
		if (mBound)
		{
			unbindService(mConnection);
			mBound = false;
		}
		super.onStop();
	}
	
	
	
	@Override
	public void onPause()
	{
		log("pausing main activity", "v");
		super.onPause();
		unregisterDetailsReceiver();
		
	}
	
	public void onDestroy()
	{
		log("onDestroy()", "v");
		super.onDestroy();
	}

	
	@Override
    public void onDialogPositiveClick(View view) {
        // User touched the dialog's positive button
		log("add station confirmed", "i");
		EditText titleView = (EditText)view.findViewById(R.id.station_title);
		EditText urlView = (EditText)view.findViewById(R.id.station_url);
		
		//int preset = 1; 
		String title = titleView.getText().toString().trim();
		String url = urlView.getText().toString().trim();
		boolean valid = ServiceRadioPlayer.validateUrl(url);
		if (valid)
		{
			ContentValues values = new ContentValues();
			//values.put(DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER, preset);
	        values.put(DbContractRadio.EntryStation.COLUMN_NAME_TITLE, title);
	        values.put(DbContractRadio.EntryStation.COLUMN_NAME_URL, url);
			//CursorLoader var = getLoaderManager().getLoader(ActivityMain.LOADER_STATIONS);
			Uri uri = getContentResolver().insert(ContentProviderRadio.CONTENT_URI_STATIONS, values);
			int id = (int) ContentUris.parseId(uri);
			if (id == -1)
			{
				throw new SQLiteException("Insert failed");
			}
			log("uri of addition:" + uri, "v");
			this.updateDetails();
		}
		else
		{
			//FIXME code duplication in FragmentStations
			log("URL " + url + " not valid", "v");
			LayoutInflater inflater = LayoutInflater.from(this);
			final View editView = inflater.inflate(R.layout.dialog_station_details, null);
			titleView = ((EditText)editView.findViewById(R.id.station_title));
			titleView.setText(title);
			urlView = ((EditText)editView.findViewById(R.id.station_url));
			urlView.setText(url);
			urlView.requestFocus();
			AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
			builder.setView(editView);
			builder.setPositiveButton(R.string.edit_station, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					onDialogPositiveClick(editView);
				}
			});
			builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					onDialogNegativeClick();
					
				}
			});
			builder.setTitle("URL appears invalid. Try again");
			builder.show();
			//TODO
		}
    }

    @Override
    public void onDialogNegativeClick() {
        // User touched the dialog's negative button
    	log("add station cancelled", "i");
    }
	
    @Override
	public void onDialogEventPositiveClick(DialogFragment dialogFragment) {
    	log("event details", "i");
    	EditText detailsView = (EditText)dialogFragment.getDialog().findViewById(R.id.event_details);
    	log("--------------------{-----------------", "i");
    	log(detailsView.getText().toString(), "i");
    	log("--------------------}-----------------", "i");
	}


	@Override
	public void onDialogEventNegativeClick(DialogFragment dialogFragment) {
		log("--------------------{-----------------", "i");
		log("event details cancelled", "i");
		log("--------------------}-----------------", "i");
	}	


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == R.id.mark_event)
		{
			log("--------------------------------------", "i");
			log("Event button pressed", "i");
			log("--------------------------------------", "i");
			DialogFragment dialog = new DialogFragmentEvent();
			dialog.show(this.getSupportFragmentManager(), "DialogFragmentEvent");
			return true;	
		}
		return false;
		
	}
	
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		FragmentPlayer fragment = (FragmentPlayer) this.getSupportFragmentManager().findFragmentByTag(FragmentPlayer.FRAGMENT_TAG);
		if(fragment != null && (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP))
        {
			 fragment.updateSlider();
        }
		return super.onKeyUp(keyCode, event);
	}
	
	public void play(int id)
	{
		log("Play button received, sending play intent", "d");
		Intent intent = new Intent(this, ServiceRadioPlayer.class);
		intent.setAction(ServiceRadioPlayer.ACTION_PLAY);
		intent.putExtra(EXTRA_STATION_PRESET, id);
		startService(intent);
		//mService.play(url);
	}
	
	public void setVolume(int volume)
	{
		log("setVolume()", "v");
		mService.setVolume(volume);
	}
	

	public void stop(View view)
	{
		log("stop()", "v");
		mService.stop();
	}
	
	public void next(View view)
	{
		log("next()", "v");
		mService.nextPreset();
	}
	
	public void prev(View view)
	{
		log("prev()", "v");
		mService.previousPreset();
	}
	
/*
	public void stop(View view)
	{
		log("stop(View view)", "v");
		//Intent intent = new Intent(this, ServiceRadioPlayer.class);
		//stopService(intent);
		
		//log("Stop button received, sending stop intent", "d");
		//Intent intent = new Intent(this, ServiceRadioPlayer.class);
		//intent.setAction(ServiceRadioPlayer.ACTION_STOP);
		//startService(intent);
		
		mService.stop();
	}*/
	

	//tell service to copy logs to sd card
	public boolean copy(MenuItem item)
	{
		//Intent intent = new Intent(this, ServiceRadioPlayer.class);
		//stopService(intent);
		mService.copyLog();
		return true;
	}
	
	//tell service to clear local logs
	public boolean clear(MenuItem item)
	{
		//Intent intent = new Intent(this, ServiceRadioPlayer.class);
		//stopService(intent);
		mService.clearLog();
		return true;
	}
	

	
	protected void setActivityPlayerDetails(String station, String status, String artist, String song)
	{
		TextView songText = (TextView) this.findViewById(R.id.player_song_details);
		TextView stationText = (TextView) this.findViewById(R.id.player_station_details);
		songText.setText(artist + " - " + song);
		stationText.setText(station+ " : " + status);
	}
	
	public int getPlayingPreset()
	{
		//log("getPreset()", "v");
		int preset;
		if (mService == null)
		{
			log("service not bound", "w");
			preset = 0;
		}
		else
		{
			preset = mService.getPlayingPreset(); //returns 0 if not playing
		}
		//log("got preset " + String.valueOf(preset), "v");
		return preset;
		
	}
	
	//calls service's method which sends a broadcast to widget and activity player with current details
	public void updateDetails()
	{
		log("updateDetails()", "d");
		if (mService == null)
		{
			log("service not bound", "e");
		}
		else
		{
			mService.updateDetails();	
		}
		
		
	}

	private void log(String text, String level)
	{
		mLogger.log(this, text, level);
	}
	
	public class ReceiverDetails extends BroadcastReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	if (intent.getAction().equals(ActivityMain.ACTION_UPDATE_TEXT))
	    	{
	    		log("activity onreceive(), updating main activity text", "v");
	    		Bundle extras = intent.getExtras();
				String station = extras.getString(ActivityMain.EXTRA_STATION);
				String status = extras.getString(ActivityMain.EXTRA_STATUS);
				String artist = extras.getString(ActivityMain.EXTRA_ARTIST);
				String song = extras.getString(ActivityMain.EXTRA_SONG);
				//int preset = extras.getInt(ActivityMain.EXTRA_PRESET);
				setActivityPlayerDetails(station, status, artist, song);
				
				FragmentStations fragment = (FragmentStations) getSupportFragmentManager().findFragmentByTag(FragmentStations.FRAGMENT_TAG);
				fragment.refresh();
	    	}
	    }
	}

}
