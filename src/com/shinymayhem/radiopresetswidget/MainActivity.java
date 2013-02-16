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
package com.shinymayhem.radiopresetswidget;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.shinymayhem.radiopresetswidget.RadioDbContract.StationsDbHelper;
import com.shinymayhem.radiopresetswidget.RadioPlayer.LocalBinder;

public class MainActivity extends Activity {

	//string-extra key for intent
	public final static String URL = "com.shinymayhem.radiopresetswidget.URL";

	protected final int BUTTON_LIMIT = 6;
	
	protected boolean mBound = false;
	protected StationsDbHelper mDbHelper;
	protected RadioPlayer mService;
	
	protected Context getContext()
	{
		return this;
	}
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(getClass().toString(), "creating main activity");

		//set content view first so findViewById works
		setContentView(R.layout.activity_main);
		
		//get view
		ListView stationsLayout = (ListView)this.findViewById(R.id.stations); //TODO not created yet?
				
		//get stations from sqlite
		mDbHelper  = new StationsDbHelper(getContext());
		SQLiteDatabase db = mDbHelper.getReadableDatabase();
		String[] projection = {
				"_id",
				RadioDbContract.StationEntry.COLUMN_NAME_PRESET_NUMBER,
				RadioDbContract.StationEntry.COLUMN_NAME_TITLE,
				RadioDbContract.StationEntry.COLUMN_NAME_URL
		};
		
		String sortOrder = RadioDbContract.StationEntry.COLUMN_NAME_PRESET_NUMBER + " ASC";
		
		Cursor cursor = db.query(RadioDbContract.StationEntry.TABLE_NAME, projection, null, null, null, null, sortOrder, Integer.toString(BUTTON_LIMIT));
		//startManagingCursor(cursor);
		String[] fromColumns = {RadioDbContract.StationEntry.COLUMN_NAME_TITLE};
		int[] toViews = {R.id.station_title};
		//TODO extend SimpleCursorAdapter, add data row to constructor, put url in view with setTag, and onclick listeners 
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.station_entry, cursor, fromColumns, toViews, 0); 
		stationsLayout.setAdapter(adapter);
		/*
		if (!cursor.moveToFirst())
		{
			//no rows found
			db.close();
			db = mDbHelper.getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(RadioDbContract.StationEntry.COLUMN_NAME_PRESET_NUMBER, 1);
			values.put(RadioDbContract.StationEntry.COLUMN_NAME_TITLE, "ElectroSwing Revolution");
			values.put(RadioDbContract.StationEntry.COLUMN_NAME_URL, "http://streamplus17.leonex.de:39060");
			db.insertOrThrow(RadioDbContract.StationEntry.TABLE_NAME, null, values);
			values = new ContentValues();
			values.put(RadioDbContract.StationEntry.COLUMN_NAME_PRESET_NUMBER, 2);
			values.put(RadioDbContract.StationEntry.COLUMN_NAME_TITLE, "Jazz Radio");
			values.put(RadioDbContract.StationEntry.COLUMN_NAME_URL, "http://jazz-wr04.ice.infomaniak.ch/jazz-wr04-128.mp3");
			db.insertOrThrow(RadioDbContract.StationEntry.TABLE_NAME, null, values);
			
		}
		else
		{
			while (!cursor.isAfterLast())
			{
				long presetNumber = cursor.getLong(cursor.getColumnIndexOrThrow(RadioDbContract.StationEntry.COLUMN_NAME_PRESET_NUMBER));
				String title = cursor.getString(cursor.getColumnIndexOrThrow(RadioDbContract.StationEntry.COLUMN_NAME_TITLE));
				final String url = cursor.getString(cursor.getColumnIndexOrThrow(RadioDbContract.StationEntry.COLUMN_NAME_URL));
				cursor.moveToNext();
				Button button = new Button(this);
				button.setText(title);
				button.setId((int) presetNumber);
				final int id = button.getId();
				stationsLayout.addView(button);
				Button btn = ((Button) findViewById(id));
				btn.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View view) {
						ConnectivityManager network = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
						NetworkInfo info = network.getActiveNetworkInfo();
						if (info == null || info.isConnected() == false)
						{
							//TextView status = (TextView) findViewById(R.id.status);
							//status.setText("No network");
							Toast.makeText(getContext(), "No network", Toast.LENGTH_SHORT).show();
							Log.i(getClass().toString(), "no network, can't do anything");
							return;
						}
						else
						{
							mService.play(url);
						}
						
					}
				});
			}
		}*/
		db.close();
		//get stations from preferences
		/*SharedPreferences stations = this.getSharedPreferences(getString(R.string.stations), Context.MODE_PRIVATE);
		Map<String, ?> stationsMap = stations.getAll();
		if (stationsMap.size()>0)
		{
			Iterator<?> iterator = stationsMap.entrySet().iterator();
			while (iterator.hasNext())
			{
				Map.Entry<String, String> pairs = (Map.Entry<String, String>)iterator.next();
				String key = (String)pairs.getKey();
				Set<String> strings = (Set<String>)pairs.getValue();
				String title = (String) strings.toArray()[0];
				String url = (String) strings.toArray()[1];
				url = url;
			}
		}
		else //no saved stations
		{
			Set<String> esrRadio = new HashSet<String>(Arrays.asList("ElectroSwing Revolution", "http://streamplus17.leonex.de:39060"));
			Set<String> jazzRadio = new HashSet<String>(Arrays.asList("Jazz Radio", "http://jazz-wr04.ice.infomaniak.ch/jazz-wr04-128.mp3"));
			SharedPreferences.Editor editor = stations.edit();
			editor.putStringSet("1", esrRadio); 
			editor.putStringSet("2", jazzRadio);
			editor.commit();
		}*/
		
		//add buttons dynamically with onclick listeners
		
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	/*
	public void start(View view)
	{
		Log.d(getClass().toString(), "Play button received, sending play intent");
		Intent intent = new Intent(this, RadioPlayer.class);
		intent.setAction(RadioPlayer.ACTION_PLAY);
		intent.putExtra(URL, url);
		startService(intent);
		//mService.play(url);
	}
	
	@Override
	protected void onStart()
	{
		super.onStart();
		Log.d(getClass().toString(), "starting main activity");
		bindRadioPlayer();
	}
	
	protected void bindRadioPlayer()
	{
		Log.v(getClass().toString(), "binding radio player");
		Intent intent = new Intent(this, RadioPlayer.class);
		startService(intent);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	}
	
	protected void onRestart()
	{
		super.onRestart();
		Log.d(getClass().toString(), "restarting main activity");
	}
	
	protected void onResume()
	{
		super.onResume();
		Log.d(getClass().toString(), "resuming main activity");
	}
	
	@Override
	protected void onStop()
	{
		Log.i(getClass().toString(), "stopping main activity");
		if (mService.isPlaying() == false)
		{
			mService.stop();
		}
		if (mBound)
		{
			unbindService(mConnection);
			mBound = false;
		}
		super.onStop();
	}
	
	
	public void stop(View view)
	{
		//Intent intent = new Intent(this, RadioPlayer.class);
		//stopService(intent);
		mService.stop();
	}
	
	public boolean copy(MenuItem item)
	{
		//Intent intent = new Intent(this, RadioPlayer.class);
		//stopService(intent);
		mService.copyLog();
		return true;
	}
	
	public boolean clear(MenuItem item)
	{
		//Intent intent = new Intent(this, RadioPlayer.class);
		//stopService(intent);
		mService.clearLog();
		return true;
	}
	
	@Override
	public void onPause()
	{
		Log.i(getClass().toString(), "pausing main activity");
		super.onPause();
		
	}
	
	public void onDestroy()
	{
		Log.i(getClass().toString(), "destroying main activity");
		super.onDestroy();
	}
	
	private ServiceConnection mConnection = new ServiceConnection()
	{
		@Override
		public void onServiceConnected(ComponentName className, IBinder service)
		{
			Log.i(getClass().toString(), "service connected");
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService();
			mBound = true;
		}
		@Override
		public void onServiceDisconnected(ComponentName arg0)
		{
			Log.i(getClass().toString(), "service disconnected");
			mBound = false;
		}
	};

}
