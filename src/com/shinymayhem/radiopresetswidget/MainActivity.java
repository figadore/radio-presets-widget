/*
 * Copyright 2013 Reese Wilson | Shiny Mayhem

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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.shinymayhem.radiopresetswidget.RadioPlayer.LocalBinder;

public class MainActivity extends Activity {

	public final static String URL = "com.shinymayhem.radiopresetswidget.URL";
	
	protected boolean mBound = false;
	protected RadioPlayer mService;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(getClass().toString(), "creating main activity");
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	public void start(View view)
	{
		if (!mBound)
		{
			Intent intent = new Intent(this, RadioPlayer.class);
			
			//intent.putExtra(URL, url);
			startService(intent);
			bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		}
		ConnectivityManager network = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = network.getActiveNetworkInfo();
		if (info == null || info.isConnected() == false)
		{
			//TextView status = (TextView) findViewById(R.id.status);
			//status.setText("No network");
			Toast.makeText(this, "No network", Toast.LENGTH_SHORT).show();
			Log.i(getClass().toString(), "no network, can't do anything");
			return;
		}
		else
		{
			int id = view.getId();
			Button esr = (Button) findViewById(R.id.esr_button);
			Button jr = (Button) findViewById(R.id.jr_button);
			String url = "";
			if (id == esr.getId())
			{
				url = "http://streamplus17.leonex.de:39060";
				
			}
			else if (id == jr.getId())
			{
				url = "http://jazz-wr04.ice.infomaniak.ch/jazz-wr04-128.mp3";
			}
			mService.play(url);
		}
		
	}
	
	@Override
	protected void onStart()
	{
		super.onStart();
		Log.d(getClass().toString(), "starting main activity");
		Intent intent = new Intent(this, RadioPlayer.class);
		
		//intent.putExtra(URL, url);
		Log.v(getClass().toString(), "binding");
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
		if (!mBound)
		{
			Log.v(getClass().toString(), "not bound, rebinding");
			Intent intent = new Intent(this, RadioPlayer.class);
			
			//intent.putExtra(URL, url);
			startService(intent);
			bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		}
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
	
	public void copy(View view)
	{
		//Intent intent = new Intent(this, RadioPlayer.class);
		//stopService(intent);
		mService.copyLog();
	}
	
	public void clear(View view)
	{
		//Intent intent = new Intent(this, RadioPlayer.class);
		//stopService(intent);
		mService.clearLog();
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
