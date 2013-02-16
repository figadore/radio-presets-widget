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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class RadioPlayer extends Service implements OnPreparedListener, OnInfoListener, OnCompletionListener, OnErrorListener {
	
	public final static int ONGOING_NOTIFICATION = 1;
	public final static String ACTION = "com.shinymayhem.radiopresetswidget.ACTION";
	public final static String STOP = "Stop";
	public final static String LOG_FILENAME = "log.txt";
	public String state = STATE_UNINITIALIZED;
	public final static String STATE_UNINITIALIZED = "Uninitialized";
	public final static String STATE_INITIALIZING = "Initializing";
	public final static String STATE_PREPARING = "Preparing";
	public final static String STATE_PLAYING = "Playing";
	public final static String STATE_BUFFERING = "Buffering";
	public final static String STATE_PAUSED = "Paused";
	public final static String STATE_ERROR = "Error";
	public final static String STATE_RESTARTING = "Restarting";
	public final static String STATE_STOPPING = "Stopping";
	public final static String STATE_STOPPED = "Stopped";
	public final static String STATE_COMPLETE = "Complete";
	public final static String STATE_END = "Ended";
	public final static int NETWORK_STATE_DISCONNECTED = -1;
	protected NetworkInfo mNetworkInfo;
	protected int mNetworkState;
	protected MediaPlayer mMediaPlayer;
	protected MediaPlayer mNextPlayer;
	private NetworkReceiver mReceiver = new NetworkReceiver();
	private NoisyAudioStreamReceiver mNoisyReceiver = new NoisyAudioStreamReceiver(); 
	protected String mUrl;
	protected boolean mInterrupted = false;
	private final IBinder mBinder = new LocalBinder();
	protected boolean mBound = false;
	
	public class LocalBinder extends Binder
	{
		RadioPlayer getService()
		{
			return RadioPlayer.this;
		}
	}
	
	protected PendingIntent getStopIntent()
	{
		Intent intent = new Intent(this, RadioPlayer.class).setAction(STOP);
		return PendingIntent.getService(this, 0, intent, 0);
			 
	}
	
	@SuppressLint("NewApi")
	public void onPrepared(MediaPlayer mediaPlayer)
	{
		log("onPrepared()", "d");
		if (state == RadioPlayer.STATE_RESTARTING || state == RadioPlayer.STATE_COMPLETE)
		{
			log("newPlayer ready", "i");
		}
		else
		{
			log("mediaPlayer.start()", "v");
			mediaPlayer.start();
			state = RadioPlayer.STATE_PLAYING;
			mInterrupted = false;

			NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
				.setContentTitle("Radio Presets")
				.setContentText("Playing")
				.addAction(R.drawable.ic_launcher, "Stop", getStopIntent())
				.setSmallIcon(R.drawable.ic_launcher);
			//PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
			//TODO taskstack builder only available since 4.1
			TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
			stackBuilder.addParentStack(MainActivity.class);
			//stackBuilder.addNextIntent(nextIntent)
			Intent resultIntent = new Intent(this, MainActivity.class);
			stackBuilder.addNextIntent(resultIntent);
			PendingIntent intent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_CANCEL_CURRENT);
			builder.setContentIntent(intent);
			startForeground(ONGOING_NOTIFICATION, builder.build());
			log("start foreground notification: playing", "v");
			Toast.makeText(this, "Playing", Toast.LENGTH_SHORT).show();
		}
	}
	
	public boolean onInfo(MediaPlayer mediaPlayer, int what, int extra)
	{
		log("onInfo()", "d");
		//TextView status = (TextView) findViewById(R.id.status);
		if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END)
		{
			//Log.i(getPackageName(), "done buffering");
			log("done buffering", "v");
			if (mediaPlayer.isPlaying())
			{
			//	status.setText("Playing");
			}
			else
			{
			//	status.setText("Stopped");	
			}
			//TODO update notification to indicate resumed playback
			state = RadioPlayer.STATE_PLAYING;
			return true;
			
		}
		else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START)
		{
			
			log("start buffering", "v");
			//Log.i(getPackageName(), "start buffering");
			//status.setText("Buffering...");
			//TODO update notification to buffering
			state = RadioPlayer.STATE_BUFFERING;
			return true;
		}
		else if (what == 703)
		{
			String str = "bandwidth:";
			str += extra;
			log(str, "v");
			
			//Log.i(getPackageName(), str);
		}
		else
		{
			log("media player info", "v");
			//Log.i(getPackageName(), "media player info");
		}
		/*String statusString = "\nInfo: ";
		statusString += what;
		statusString += ":";
		statusString += extra;
		status.append(statusString);*/
		return false;
	}
	
	public void onCompletion(MediaPlayer mediaPlayer)
	{
		//mediaPlayer.reset();
		//mediaPlayer.release();
		log("onCompletion()", "d");
		state = RadioPlayer.STATE_COMPLETE;
		if (mInterrupted)
		{
			Toast.makeText(this, "Playback completed after interruption", Toast.LENGTH_SHORT).show();	
			log("Playback completed after interruption, should restart when network connects again", "i");
		}
		else
		{
			//still an interruption, streaming shouldn't complete
			Toast.makeText(this, "Playback completed, no interruption reported", Toast.LENGTH_SHORT).show();
			log("Playback completed, no network interruption reported, restart", "i");
			//restart();
			play();
		}
		if (mNextPlayer != null)
		{
			log("swapping players", "v");
			mediaPlayer.release();
			mediaPlayer = mNextPlayer;
			mNextPlayer = null;
			if (mediaPlayer.isPlaying())
			{
				state = RadioPlayer.STATE_PLAYING;
				mInterrupted = false;
				log("new player playing", "i");
				//TODO update notification to indicate resumed playback
			}
			else
			{
				log("new player not playing", "w");
			}
		}
		else
		{
			log("no next player specified", "w");
		}
		//stopForeground(true);
		//playing = false;
		//TextView status = (TextView) findViewById(R.id.status);
		//status.append("\nComplete");
	}
	
	public boolean onError(MediaPlayer mediaPlayer, int what, int extra)
	{
		log("onError()", "e");
		//check if mediaPlayer is or needs to be released
		if (mediaPlayer != null)
		{
			try
			{
				mediaPlayer.stop();
				log("media player stopped", "i");
			}
			catch (IllegalStateException e)
			{
				log("Illegal state to stop. uninitialized? not yet prepared?", "e");
			}
			mediaPlayer.release();
			mediaPlayer = null;
		}
		state = RadioPlayer.STATE_ERROR;
		//TextView status = (TextView) findViewById(R.id.status);
		String statusString = "Error: ";
		if (what == MediaPlayer.MEDIA_ERROR_UNKNOWN && extra == MediaPlayer.MEDIA_ERROR_IO)
		{
			statusString += "Media IO Error";
		}
		else if (what == MediaPlayer.MEDIA_ERROR_UNKNOWN && extra == MediaPlayer.MEDIA_ERROR_TIMED_OUT)
		{
			statusString += "Timed out";
		}
		else
		{
			statusString += what;
			statusString += ":";
			statusString += extra;
		}
		log(statusString, "e");
		//status.append(statusString);
		Toast.makeText(this, statusString, Toast.LENGTH_SHORT).show(); 
		return false;
		
	}
	
	@Override
	public void onCreate()
	{
		log("onCreate()", "d");
		IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		mReceiver = new NetworkReceiver();
        //Log.i(getPackageName(), "creating service, registering broadcast receiver");
        log("registering network change broadcast receiver", "v");
		this.registerReceiver(mReceiver, filter);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// Registers BroadcastReceiver to track network connection changes.
		log("onStartCommand()", "d");
		//Log.i(getPackageName(), "service start command");
		if (intent == null)
		{
			log("no intent", "w");
			//Log.i(getPackageName(), "No intent");
		}
		else
		{
			//TODO find out why service starts again after STOP action in intent
			String action = intent.getAction();
			if (action == null)
			{
				log("no action specified", "v");
				//Log.i(getPackageName(), "No action specified");
			}
			else if (action.equals(STOP.toString()))
			{
				log("STOP action in intent", "i");
				//Log.i(getPackageName(), "STOP");	
				end();
			}
			else
			{
				String str = "Action:";
				str += action;
				log(str, "w");
				//Log.i(getPackageName(), str);
			
			}
		}
		//this.url = intent.getStringExtra(MainActivity.URL);
		//String url = intent.getStringExtra(MainActivity.URL);
		//if (url != null)
		//{
			//this.url = url;
		//}
		//String str = "url:";
		//str += this.url;
		//Log.i(getPackageName(), str);
		//this.play();
		
		
		
		//return START_STICKY;
		return START_REDELIVER_INTENT;
	}
	
	protected void play()
	{
		log("play()", "d");
		if (this.mUrl == null)
		{
			log("url not set", "e");
			//Log.e(getPackageName(), "url not set");
		}
		else
		{
			String str = "url:";
			str += this.mUrl;
			log(str, "v");
			//Log.i(getPackageName(), str);
		}
		this.play(this.mUrl);
	}
	
	//called from bound ui, possibly notification action, if implemented
	//possible start states: any
	protected void play(String url)
	{
		log("play(url)", "d");
		//TODO start foreground notification here with initializing status
		state = RadioPlayer.STATE_INITIALIZING;
		if (isConnected())
		{
			log("setting network type", "v");
			mNetworkState = this.getConnectionType();
		}
		else
		{
			
			log("setting network state to disconnected", "v");
			mNetworkState = RadioPlayer.NETWORK_STATE_DISCONNECTED;
			Toast.makeText(this, "tried to play with no network", Toast.LENGTH_SHORT).show();
			log("no network, should be handled by ui? returning", "e");
			
			return;
		}
		//get url from param, fallback to instance variable
		if (url != null)
		{
			this.mUrl = url;	
		}
		else if (this.mUrl != null && url == null)
		{
			url = this.mUrl;
		}
		else if(this.mUrl == null && url == null)
		{
			log("trouble: url not set", "e");
		}
		
		//begin listen for headphones unplugged
		IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
		registerReceiver(mNoisyReceiver, intentFilter);
		log("register noisy receiver", "v");
		
		
		//Toast.makeText(this, "Initializing", Toast.LENGTH_SHORT).show();
		/*
		if (playing && mediaPlayer.isPlaying() == false)
		{
			log("play state wrong, class says it is playing but it isn't", "v");
			Toast.makeText(this, "Play state wrong", Toast.LENGTH_SHORT).show();
			playing = false;
		}
		else if (playing && mediaPlayer.isPlaying() == true)
		{
			log("play state wrong2, class says it isn't playing but it is", "v");
			Toast.makeText(this, "Play state wrong", Toast.LENGTH_SHORT).show();
			playing = true;
		}*/
		if (mMediaPlayer != null)
		{
			log("releasing old media player", "v");
			mMediaPlayer.release();
			mMediaPlayer = null;
		}
		log("creating new media player", "v");
		this.mMediaPlayer = new MediaPlayer();
		
		mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);	
		
		String str = "setting data source: ";
		str += url;
		log(str, "v");
		try
		{
			mMediaPlayer.setDataSource(url);
		}
		catch (IOException e) 
		{
			//TODO handle this somehow
			log("setting data source failed", "e");
			Toast.makeText(this, "Setting data source failed", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
		initializePlayer(mMediaPlayer); 

		state = RadioPlayer.STATE_PREPARING;
	
		mMediaPlayer.prepareAsync(); // might take long! (for buffering, etc)
		log("preparing async", "d");
		Toast.makeText(this, "Preparing", Toast.LENGTH_SHORT).show();
	}
	
	protected void initializePlayer(MediaPlayer player)
	{
		log("initializePlayer()", "d");
		player.setOnPreparedListener(this);		
		player.setOnInfoListener(this);
		player.setOnCompletionListener(this);
		player.setOnErrorListener(this);
		return;
	}
	
	//called from unbound, headphones unplugged, notification->stop 
	protected void end()
	{
		log("end()", "d");
		stop();
		if (!mBound)
		{
			log("stopping service with stopself", "d");
			stopSelf();
			state = RadioPlayer.STATE_END;
		}
		else
		{
			String str = "still bound";
			log(str, "v");
		}
	}

	//called from onDestroy, end
	protected void stop()
	{
		log("stop()", "d");
		//stop command called, reset interrupted flag
		mInterrupted = false;
		if (state == RadioPlayer.STATE_STOPPED || state == RadioPlayer.STATE_END || state == RadioPlayer.STATE_UNINITIALIZED)
		{
			log("already stopped", "v");
			return;
		}
		state = RadioPlayer.STATE_STOPPING;
		if (mMediaPlayer == null)
		{
			log("null media player", "e");
		}
		else
		{
			log("stopping playback", "v");
			Toast.makeText(this, "Stopping playback", Toast.LENGTH_SHORT).show();
			stopForeground(true);
			mMediaPlayer.stop();
			//mediaPlayer.reset();
			mMediaPlayer.release();
			mMediaPlayer = null;
				
		}
		try
		{
			unregisterReceiver(mNoisyReceiver);
			log("unregistering noisyReceiver", "v");
		}
		catch (IllegalArgumentException e)
		{
			log("noisyReceiver already unregistered", "e");
		}
		
		
		state = RadioPlayer.STATE_STOPPED;
	}
	
	//called from onComplete or network change if no network and was playing
	protected void restart()
	{
		log("restart()", "d");
		state = RadioPlayer.STATE_RESTARTING;
		
		if (mNextPlayer != null)
		{
			log("nextPlayer not null","e");
			mNextPlayer.release();
			mNextPlayer = null;
		}
		
		this.mNextPlayer = new MediaPlayer();
		
		mNextPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);	
		
		String str = "setting nextPlayer data source: ";
		str += mUrl;
		log(str, "v");
		try
		{
			mNextPlayer.setDataSource(mUrl);
		}
		catch (IOException e) 
		{
			//TODO handle this somehow
			log("setting nextPlayer data source failed", "e");
			Toast.makeText(this, "Setting data source failed", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
		initializePlayer(mNextPlayer); 
		//TODO handle older versions
		//check for errors or invalid states
		try
		{
			mMediaPlayer.stop();
		}
		catch (IllegalStateException e)
		{
			log("old player in wrong state to stop", "e");
		}
		
		try
		{
			boolean playing = mMediaPlayer.isPlaying();
			if (playing)
			{
				log("old player playing, new player ready to set", "i");
			}
			else
			{
				log("old player not playing, might not be safe to setnext", "w");
			}
		}
		catch (IllegalStateException e)
		{
			log("old player in wrong state to check if playing", "e");
		}
		
		//check to see if playback completed 
		if (state == RadioPlayer.STATE_COMPLETE)
		{
			log("playback completed", "e");
			return;
		}
		try
		{
			mMediaPlayer.setNextMediaPlayer(mNextPlayer);
			log("nextplayer set", "i");
		}
		catch (IllegalStateException e)
		{
			log("old player in wrong state to setnext", "e");
		}
		/*
		 
		
		if (mediaPlayer.isPlaying())
		{
			log("was still playing, stopping", "v");
			mediaPlayer.stop();	
		}
		log("restarting/preparing", "v");
		mediaPlayer.prepareAsync();
		
		*/
		
		
		//Log.i(getPackageName(), "restarting playback");
		/*if (playing)
		{
			mediaPlayer.stop();	
			mediaPlayer.release();
			playing = false;
		}
		*/
		//play();
	}
	
	public boolean isPlaying()
	{
		if (
				state == RadioPlayer.STATE_BUFFERING || 
				state == RadioPlayer.STATE_PLAYING ||
				state == RadioPlayer.STATE_PAUSED ||
				state == RadioPlayer.STATE_PREPARING ||
				state == RadioPlayer.STATE_INITIALIZING ||
				state == RadioPlayer.STATE_COMPLETE || //service should still stay alive and listen for network changes to resume
				state == RadioPlayer.STATE_RESTARTING
			)
		{
			return true;
		}
		return false;
	}
	
	protected boolean isConnected()
	{
		ConnectivityManager network = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = network.getActiveNetworkInfo();
		mNetworkInfo = info;
		if (info == null)
		{
			return false;
		}
		return true;
	}
	
	protected int getConnectionType()
	{
		String str = "";
		int newState = mNetworkInfo.getType();
		switch (newState)
		{
			case ConnectivityManager.TYPE_WIFI:
				str += "wifi";
				break;
			case ConnectivityManager.TYPE_MOBILE:
				str += "mobile";
				break;
			case ConnectivityManager.TYPE_MOBILE_DUN:
				str += "mobile-dun";
				break;
			case ConnectivityManager.TYPE_MOBILE_HIPRI:
				str += "moblie-hipri";
				break;
			case ConnectivityManager.TYPE_MOBILE_MMS:
				str += "mobile-mms";
				break;
			case ConnectivityManager.TYPE_MOBILE_SUPL:
				str += "mobile-supl";
				break;
			case ConnectivityManager.TYPE_WIMAX:
				str += "wimax";
				break;
			case ConnectivityManager.TYPE_ETHERNET:
				str += "ethernet";
				break;
			case ConnectivityManager.TYPE_BLUETOOTH:
				str += "bluetooth";
				break;
			case ConnectivityManager.TYPE_DUMMY:
				str += "dummy";
				break;
		}
		str += " detected";
		log(str, "i");
		return newState;
	}
	/*
		if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI)
		{
			return this.WIRELESS_STATE_WIFI;
		}
		else if (info != null && info.getType() == ConnectivityManager.T)
		if (info == null || info.isConnected() == false)
		{
			Toast.makeText(this, "No network", Toast.LENGTH_SHORT).show();
			log("no network, can't do anything", "v");
			return false;
		}
		return true;
	}*/

	@Override
	public IBinder onBind(Intent intent) {
		log("onBind()", "d");
		//Log.i(getPackageName(), "binding service");
		mBound=true;
		return mBinder;
	}
	
	@Override
	public void onRebind(Intent intent)
	{
		log("onRebind()", "d");
		//Log.i(getPackageName(), "rebinding service");
	}
	
	@Override
	public boolean onUnbind(Intent intent)
	{
		log("onUnbind()", "d");
		//Log.i(getPackageName(), "unbinding service");
		mBound = false;
		if (!isPlaying())
		{
			end();
		}
		return false;
	}
	
	@Override
	public void onDestroy() {
		log("onDestroy()", "d");
		this.stop();
		Toast.makeText(this, "Stopping service", Toast.LENGTH_SHORT).show();
		//don't need to listen for network changes anymore
		if (mReceiver != null) {
			log("unregister network receiver", "v");
            this.unregisterReceiver(mReceiver);
        }
		else
		{
			log("unregistering network receiver failed, null", "e");
		}
		
		 
	}
	
	private class NoisyAudioStreamReceiver extends BroadcastReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
	        	log("headphones unplugged", "i");
				//Log.i(getPackageName(), "headphones unplugged");
	        	end();
	        }
	    }
	}
	
	//handle network changes
	public class NetworkReceiver extends BroadcastReceiver {   
	      
		@Override
		public void onReceive(Context context, Intent intent) {
			log("received network change broadcast", "i");
			//Log.i(getPackageName(), "received network change broadcast");
			if (mMediaPlayer == null && state == RadioPlayer.STATE_PREPARING)
			{
				log("recover from disconnect while preparing", "v");
				//Log.i(getPackageName(), "no media player, don't care about connection updates");
				play();
				return;
			}
			else if (mMediaPlayer == null && state != RadioPlayer.STATE_UNINITIALIZED)
			{
				log("media player null, will cause problems if connected", "e");
			}
			String str = "";
			if (isConnected())
			{
				int newState = getConnectionType();
				if (mNetworkState != newState)
				{
					
					str = "network type changed";
					if (mInterrupted == false)
					{
						str += " and now interrupted";
						mInterrupted = true;	
					}
					else
					{
						str += " but previously interrupted";
					
					}
					str += "old:" + mNetworkState;
					str += ", new:" + newState;
					log(str, "v");
					
					
					log("setting network state ivar", "v");
					mNetworkState = newState;
					
					//if uninitialized, no url set yet. 
					if (state == RadioPlayer.STATE_UNINITIALIZED)
					{
						log("unitialized, don't try to start or restart", "i");
						return;
					}
					boolean start = false;
					try 
					{
						mMediaPlayer.isPlaying();
					}
					catch (IllegalStateException e)
					{
						log("illegal state detected, can't restart, start from scratch instead", "e");
						start = true;
					}
					//can't set nextplayer after complete, so just start fresh
					if (state == RadioPlayer.STATE_COMPLETE || start)
					{
						log("complete or start=true", "v");
						play();
					}
					//network interrupted playback but isn't done playing from buffer yet
					//set nextplayer and wait for current player to complete
					else  
					{
						log("!complete && start!=true", "v");
						play();
						//TODO figure out if this is possible, or too buggy
						//restart();
					}
				}
				else
				{
					str ="network type same";
					log(str, "v");
				}
				
			}
			else
			{
				str = "";
				if (mNetworkState == RadioPlayer.NETWORK_STATE_DISCONNECTED)
				{
					str = "still ";
				}
				str += "not connected. ";
				str += "old:" + mNetworkState;
				str += ", new:" + Integer.toString(RadioPlayer.NETWORK_STATE_DISCONNECTED);
				log(str, "v");
				
				log(str, "w");
				
				log("setting network state ivar to disconnected", "v");
				mNetworkState = RadioPlayer.NETWORK_STATE_DISCONNECTED;
				

				//TODO figure out the best thing to do for each state
				if (state == RadioPlayer.STATE_PREPARING) //this will lead to a mediaioerror when it reaches prepared
				{
					mMediaPlayer.release();
					mMediaPlayer = null;
					log("-------------------------------", "d");
					log("network connection lost while preparing? set null mediaplayer", "d");
					log("-------------------------------", "d");
				}
				else if (state == RadioPlayer.STATE_ERROR)
				{
					mMediaPlayer.release();
					mMediaPlayer = null;
					log("-------------------------------", "d");
					log("not sure what to do, how did we get here?", "d");
					log("-------------------------------", "d");
				}
				else
				{
					log("-------------------------------", "d");
					log("other state", "i");
					log("-------------------------------", "d");
				}
			}
			
			
			/*
		    String state;
		    String player;
		    String interrupt;
		    if (mediaPlayer.isPlaying())
		    {
		    	player = "mediaPlayer: playing";
		    }
		    else
		    {
		    	player = "mediaPlayer: not playing";
		    }
		    if (isPlaying())
		    {
		    	state= "state: playing";
		    }
		    else
		    {
		    	state = "state: not playing";
		    }
		    if (interrupted)
	    	{
		    	interrupt = "interrupted, restarting";
	    	}
	    	else
	    	{
	    		interrupt = "not interrupted";
	    	}
		    //String str;
		    if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
		    	 str = "wifi type detected";
		    	
		    	
		    	
		    // If the setting is ANY network and there is a network connection
		    // (which by process of elimination would be mobile)
		    } else if (networkInfo != null) {
		    	str = "mobile type detected";
		    	
		    // Otherwise, the app can't download content--either because there is no network
		    // connection (mobile or Wi-Fi)
		    } else {
		    	str = "no network detected";
		    	//log("no network", "v");

		        if (isPlaying())
		        {
		        	if (interrupted == false)
		        	{
		        		interrupted = true;
		        		interrupt += " until now";	
		        	}
		        	
		        	//Toast.makeText(context, "no network, interrupt", Toast.LENGTH_SHORT).show();
		        	//log("no network, interrupt", "e");
		        }
		        else
		        {
		        	//Toast.makeText(context, "no network, not interrupted", Toast.LENGTH_SHORT).show();
		        	//log("no network, not interrupted", "e");
		        }
		    }
		    str += ", ";
		    str += state;
		    str += ", ";
		    str += player;
		    str += ", ";
		    str += interrupt;
		    log(str, "v");
		    if (interrupted && state != RadioPlayer.STATE_PAUSED)
		    {
		    	restart();
		    }
		    */
		}
	}
	

	private void log(String text, String level)
	{
		String str = "State:";
		str += state;
		str += ":\t\t\t\t";
		str += text;
		FileOutputStream file;
		if (level == "v")
		{
			str = "VERBOSE:\t\t" + str;
			Log.v("RadioPlayerService", str);
		}
		else if (level == "d")
		{
			str = "DEBUG:\t\t" + str;
			Log.d("RadioPlayerService", str);
		}
		else if (level == "i")
		{
			str = "INFO:\t\t" + str;
			Log.i("RadioPlayerService", str);
		}
		else if (level == "w")
		{
			str = "WARN:\t\t" + str;
			Log.w("RadioPlayerService", str);
		}
		else if (level == "e")
		{
			str = "ERROR:\t\t" + str;
			Log.e("RadioPlayerService", str);
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
			file = openFileOutput(LOG_FILENAME, Context.MODE_APPEND);
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
	
	public void clearLog()
	{
		//File file = getFileStreamPath(LOG_FILENAME);
		deleteFile(LOG_FILENAME);
		//logging something should recreate the log file
		log("log file deleted", "i");
	}
	
	public void copyLog()
	{
		//String path = Environment.getExternalStorageDirectory().getAbsolutePath();
		String path = getExternalFilesDir(null).getAbsolutePath();
		
		File src = getFileStreamPath(LOG_FILENAME); 
		File dst = new File(path + File.separator + LOG_FILENAME);
		try {
			if (dst.createNewFile())
			{
				log("sd file created", "v");
				//Log.i(getPackageName(), "sd file created");
			}
			else
			{
				log("sd file exists?", "v");
				//Log.i(getPackageName(), "sd file exists?");
			}
		} catch (IOException e2) {
			log("sd file error", "e");
			Toast.makeText(this, "sd file error", Toast.LENGTH_SHORT).show();
			e2.printStackTrace();
		}
		
		FileChannel in = null;
		FileChannel out = null;
		try {
			in = new FileInputStream(src).getChannel();
		} catch (FileNotFoundException e1) {
			log("in file not found", "e");
			Toast.makeText(this, "in file not found", Toast.LENGTH_SHORT).show();
			e1.printStackTrace();
		}
		try {
			out = new FileOutputStream(dst).getChannel();
		} catch (FileNotFoundException e1) {
			log("out file not found", "e");
			Toast.makeText(this, "out file not found", Toast.LENGTH_SHORT).show();
			e1.printStackTrace();
		}
		
		try
		{
			in.transferTo(0, in.size(), out);
			String str = "log file copied to ";
			str += path + File.separator + LOG_FILENAME;
			log(str, "i");
			if (in != null)
			{
				in.close();
			}
			if (out != null)
			{
				out.close();
			}
			clearLog();
		} catch (IOException e) {
			log("error copying log file", "e");
			Toast.makeText(this, "error copying log file", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
		finally
		{
			
		}
	}
	
}
