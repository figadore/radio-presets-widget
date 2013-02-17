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

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.shinymayhem.radiopresets.AddDialogFragment.AddDialogListener;
import com.shinymayhem.radiopresets.RadioDbContract.StationsDbHelper;
import com.shinymayhem.radiopresets.RadioPlayer.LocalBinder;

public class MainActivity extends Activity implements AddDialogListener {

	//string-extra key for intent
	public final static String URL = "com.shinymayhem.radiopresets.URL";

	public static final int BUTTON_LIMIT = 20;
	
	public final static String LOG_FILENAME = "log.txt";
	
	protected boolean mBound = false;
	//protected StationsDbHelper mDbHelper;
	protected RadioPlayer mService;
	protected StationsDbHelper mDbHelper;
	protected Logger mLogger = new Logger();
	
	protected Context getContext()
	{
		return this;
	}
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		log("creating main activity", "d");

		//set content view first so findViewById works
		setContentView(R.layout.activity_main);
		
		if (findViewById(R.id.fragment_container) != null) {
			if (savedInstanceState != null) { //don't create overlapping fragments
                return;
            }
			
			StationsFragment stationsFragment = new StationsFragment();
			stationsFragment.setArguments(getIntent().getExtras());
			
			FragmentManager fragmentManager = getFragmentManager();
			fragmentManager.beginTransaction()
					.add(R.id.fragment_container, stationsFragment)
					.commit();
		}
		/*
		//get view
		ListView stationsLayout = (ListView)this.findViewById(R.layout.stations_fragment); 

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
		RadioCursorAdapter adapter = new RadioCursorAdapter(this, cursor, RadioCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
		stationsLayout.setAdapter(adapter);
		stationsLayout.setOnItemSelectedListener(new OnItemSelectedListener()
		{

			@Override
			public void onItemSelected(AdapterView<?> adapter, View view, int position,
					long id) {
				// TODO Auto-generated method stub
				String str= "item selected, view position:";
				str += Integer.toString(position);
				str += ", row id:";
				str += Long.toString(id);
				log(str, "v");
				//Toast.makeText(getContext(), str, Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onNothingSelected(AdapterView<?> adapter) {
				// TODO Auto-generated method stub
				String str = "nothing selected";
				log(str, "v");
				//Toast.makeText(getContext(), "nothing selected", Toast.LENGTH_SHORT).show();
				
			}
			
		});
		
		stationsLayout.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> adapter, View view, int position,
					long id) {
				// TODO Auto-generated method stub
				String str= "item clicked, view position:";
				str += Integer.toString(position);
				str += ", row id:";
				str += Long.toString(id);
				log(str, "v");
				
			}
		});
		
		stationsLayout.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> adapter, View view, int position,
					long id) {
				// TODO Auto-generated method stub
				String str= "item long clicked, view position:";
				str += Integer.toString(position);
				str += ", row id:";
				str += Long.toString(id);
				log(str, "v");
				//Toast.makeText(getContext(), str, Toast.LENGTH_SHORT).show();
				return true;
			}
		});
		
		db.close();

		*/
		
	}
	
	@Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        // User touched the dialog's positive button
		log("add station confirmed", "i");
		EditText titleView = (EditText)dialog.getDialog().findViewById(R.id.new_station_title);
		EditText urlView = (EditText)dialog.getDialog().findViewById(R.id.new_station_url);
		String title = titleView.getText().toString();
		String url = urlView.getText().toString();
		
		mDbHelper  = new StationsDbHelper(getContext());
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(RadioDbContract.StationEntry.COLUMN_NAME_PRESET_NUMBER, 1);
        values.put(RadioDbContract.StationEntry.COLUMN_NAME_TITLE, title);
        values.put(RadioDbContract.StationEntry.COLUMN_NAME_URL, url);
        db.insertOrThrow(RadioDbContract.StationEntry.TABLE_NAME, null, values);
        /*values = new ContentValues();
        values.put(RadioDbContract.StationEntry.COLUMN_NAME_PRESET_NUMBER, 2);
        values.put(RadioDbContract.StationEntry.COLUMN_NAME_TITLE, "Jazz Radio");
        values.put(RadioDbContract.StationEntry.COLUMN_NAME_URL, "http://jazz-wr04.ice.infomaniak.ch/jazz-wr04-128.mp3");
        db.insertOrThrow(RadioDbContract.StationEntry.TABLE_NAME, null, values);*/
        db.close();
        
        
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        // User touched the dialog's negative button
    	log("add station cancelled", "i");
    }
	
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	public void play(String url)
	{
		log("Play button received, sending play intent", "d");
		Intent intent = new Intent(this, RadioPlayer.class);
		intent.setAction(RadioPlayer.ACTION_PLAY);
		intent.putExtra(URL, url);
		startService(intent);
		//mService.play(url);
	}
	

	public void stop(View view)
	{
		//Intent intent = new Intent(this, RadioPlayer.class);
		//stopService(intent);
		mService.stop();
	}
	
	//tell service to copy logs to sd card
	public boolean copy(MenuItem item)
	{
		//Intent intent = new Intent(this, RadioPlayer.class);
		//stopService(intent);
		mService.copyLog();
		return true;
	}
	
	//tell service to clear local logs
	public boolean clear(MenuItem item)
	{
		//Intent intent = new Intent(this, RadioPlayer.class);
		//stopService(intent);
		mService.clearLog();
		return true;
	}
	

	private ServiceConnection mConnection = new ServiceConnection()
	{
		@Override
		public void onServiceConnected(ComponentName className, IBinder service)
		{
			log("service connected", "d");
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService();
			mBound = true;
		}
		@Override
		public void onServiceDisconnected(ComponentName arg0)
		{
			log("service disconnected", "d");
			mBound = false;
		}
	};
	

	private void log(String text, String level)
	{
		mLogger.log(this, text, level);
	}
	/*
	private void log(String text, String level)
	{
		String str = "MainActivity:\t\t" + text;
		FileOutputStream file;
		if (level == "v")
		{
			str = "VERBOSE:\t\t" + str;
			Log.v("MainActivity:", str);
		}
		else if (level == "d")
		{
			str = "DEBUG:\t\t" + str;
			Log.d("MainActivity:", str);
		}
		else if (level == "i")
		{
			str = "INFO:\t\t" + str;
			Log.i("MainActivity:", str);
		}
		else if (level == "w")
		{
			str = "WARN:\t\t" + str;
			Log.w("MainActivity:", str);
		}
		else if (level == "e")
		{
			str = "ERROR:\t\t" + str;
			Log.e("MainActivity", str);
		}
		else
		{
			Toast.makeText(this, "new log level", Toast.LENGTH_SHORT).show();
			Log.e(getPackageName(), "new log level");
			str = level + str;
			Log.e(getPackageName(), str);
		}
		
		try {
			Calendar cal = Calendar.getInstance();
	    	cal.getTime();
	    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
	    	str += "\n";
	    	str = sdf.format(cal.getTime()) + "\t\t" + str;
			file = openFileOutput(MainActivity.LOG_FILENAME, Context.MODE_APPEND);
			file.write(str.getBytes());
			file.flush();
			file.close();
			//file = File.createTempFile(fileName, null, this.getCacheDir());
			//file.
		}
		catch (Exception e)
		{
	    	Toast.makeText(this, "error writing to log file", Toast.LENGTH_SHORT).show();
	    	e.printStackTrace();
		}
	}
	*/
	
	@Override
	protected void onStart()
	{
		super.onStart();
		log("starting main activity", "d");
		bindRadioPlayer();
	}
	
	protected void bindRadioPlayer()
	{
		log("binding radio player", "d");
		Intent intent = new Intent(this, RadioPlayer.class);
		startService(intent);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	}
	
	protected void onRestart()
	{
		super.onRestart();
		log("restarting main activity", "d");
	}
	
	protected void onResume()
	{
		super.onResume();
		log("resuming main activity", "d");
	}
	
	@Override
	protected void onStop()
	{
		log("stopping main activity", "d");
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
	
	
	
	@Override
	public void onPause()
	{
		log("pausing main activity", "d");
		super.onPause();
		
	}
	
	public void onDestroy()
	{
		log("destroying main activity", "d");
		super.onDestroy();
	}
	

}
