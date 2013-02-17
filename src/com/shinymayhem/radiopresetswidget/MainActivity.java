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

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ListView;
import android.widget.Toast;

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

		log("creating main activity", "d");

		//set content view first so findViewById works
		setContentView(R.layout.activity_main);
		
		//get view
		ListView stationsLayout = (ListView)this.findViewById(R.id.stations); 

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

		
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	public void play(String url)
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
		log("pausing main activity", "d");
		super.onPause();
		
	}
	
	public void onDestroy()
	{
		log("destroying main activity", "d");
		super.onDestroy();
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
		String str = "MainActivity\t\t" + text;
		FileOutputStream file;
		if (level == "v")
		{
			str = "VERBOSE:\t\t" + str;
			Log.v("MainActivity", str);
		}
		else if (level == "d")
		{
			str = "DEBUG:\t\t" + str;
			Log.d("MainActivity", str);
		}
		else if (level == "i")
		{
			str = "INFO:\t\t" + str;
			Log.i("MainActivity", str);
		}
		else if (level == "w")
		{
			str = "WARN:\t\t" + str;
			Log.w("MainActivity", str);
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
			file = openFileOutput(RadioPlayer.LOG_FILENAME, Context.MODE_APPEND);
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
	

}
