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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.webkit.URLUtil;
import android.widget.Toast;

public class ServiceRadioPlayer extends Service implements OnPreparedListener, OnInfoListener, OnCompletionListener, OnErrorListener, OnAudioFocusChangeListener {
	
	public final static int ONGOING_NOTIFICATION = 1;
	//public final static String ACTION = "com.shinymayhem.radiopresets.ACTION";
	public final static String ACTION_STOP = "com.shinymayhem.radiopresets.ACTION_STOP";
	public final static String ACTION_PLAY = "com.shinymayhem.radiopresets.ACTION_PLAY";
	public final static String ACTION_NEXT = "com.shinymayhem.radiopresets.ACTION_NEXT";
	public final static String ACTION_PREVIOUS = "com.shinymayhem.radiopresets.ACTION_PREVIOUS";
	public final static String ACTION_MEDIA_BUTTON = "com.shinymayhem.radiopresets.MEDIA_BUTTON";
	private String mCurrentPlayerState = STATE_UNINITIALIZED;
	public final static String STATE_UNINITIALIZED = "Uninitialized";
	public final static String STATE_INITIALIZING = "Initializing";
	public final static String STATE_PREPARING = "Preparing";
	public final static String STATE_PLAYING = "Playing";
	public final static String STATE_BUFFERING = "Buffering";
	public final static String STATE_PAUSED = "Paused";
	public final static String STATE_PHONE = "Phone";
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
	private ReceiverNetwork mReceiver;
	private ReceiverNoisyAudioStream mNoisyReceiver;
	private ReceiverPhoneCall mPhoneReceiver;
	private AudioManager mAudioManager;
	private boolean mMediaButtonEventReceiverRegistered = false;
	private ReceiverMediaButton mButtonReceiver;// = new ReceiverMediaButton();
	//private OnAudioFocusChangeListener mFocusListener;
	private boolean mAudioFocused = false;
	protected String mUrl;
	protected String mTitle;
	protected int mPreset;
	protected boolean mInterrupted = false;
	private final IBinder mBinder = new LocalBinder();
	protected boolean mBound = false;
	protected ActivityLogger mLogger = new ActivityLogger();
	protected Intent mIntent;
	protected NotificationManager mNotificationManager;
	
	public class LocalBinder extends Binder
	{
		ServiceRadioPlayer getService()
		{
			return ServiceRadioPlayer.this;
		}
	}

	@Override
	public void onCreate()
	{
		log("onCreate()", "v");
		
		//initialize managers
		if (mNotificationManager == null)
		{
			mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		} 
		if (mAudioManager == null)
		{
			mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		}
		
		//remove pending intents if they exist
		stopInfo(); 

		registerNetworkReceiver();
		registerButtonReceiver();
		
	}
	
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		log("onStartCommand()", "v");

		//check if resuming after close for memory, or other crash
		if ((flags & Service.START_FLAG_REDELIVERY) != 0)
		{
			log("------------------------------------------", "v");
			log("intent redelivery, restarting. maybe handle this with dialog? notification with resume action?", "v");
			log("------------------------------------------", "v");
		}
		
		mIntent = intent;
		if (intent == null)
		{
			log("no intent", "w");
		}
		else
		{
			//TODO find out why service starts again after STOP action in intent (does it still?)
			String action = intent.getAction();
			if (action == null)
			{
				log("no action specified, not sure why", "w");
				//return flag indicating no further action needed if service is stopped by system and later resumes
				return START_NOT_STICKY;
			}
			else if (action.equals(Intent.ACTION_RUN)) //called when service is being bound
			{
				log("service being started before bound", "v");
				return START_NOT_STICKY;
			}
			else if (action.equals(ACTION_PLAY.toString())) //Play intent
			{
				log("PLAY action in intent", "v");
				int preset = Integer.valueOf(intent.getIntExtra(ActivityMain.EXTRA_STATION_PRESET, 0));	
				log("preset in action:" + String.valueOf(preset), "v");
				play(preset);
				//return START_REDELIVER_INTENT;
				return START_NOT_STICKY; 
			}
			else if (action.equals(ACTION_NEXT.toString())) //Next preset intent
			{
				log("NEXT action in intent", "v");	
				nextPreset();
				return START_NOT_STICKY;
			}
			else if (action.equals(ACTION_PREVIOUS.toString())) //Previous preset intent
			{
				log("PREVIOUS action in intent", "v");	
				previousPreset();
				return START_NOT_STICKY;
			}
			else if (action.equals(ACTION_STOP.toString())) //Stop intent
			{
				log("STOP action in intent", "v");	
				end();
				return START_NOT_STICKY;
			}
			else
			{
				String str = "Unknown Action:";
				str += action;
				log(str, "w");
				//Log.i(getPackageName(), str);
			
			}
		}
		
		//return START_STICKY;
		return START_NOT_STICKY;
		//return START_REDELIVER_INTENT;
	}
	
	
	@SuppressLint("NewApi")
	public void onPrepared(MediaPlayer mediaPlayer)
	{
		log("onPrepared()", "v");
		if (mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_RESTARTING) || mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_COMPLETE))
		{
			log("newPlayer ready", "i");
		}
		else
		{
			int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
			if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
			{
				mAudioFocused = true;
				
				log("mediaPlayer.start()", "v");
				mediaPlayer.start();
				mCurrentPlayerState = ServiceRadioPlayer.STATE_PLAYING;
				if (mInterrupted)
				{
					log("set interrupted = false", "v");
				}
				mInterrupted = false;

				Notification notification = updateNotification("Playing", "Stop", true);
				mNotificationManager.notify(ONGOING_NOTIFICATION, notification);
				startForeground(ONGOING_NOTIFICATION, notification);
				
				
				log("start foreground notification: playing", "v");
				Toast.makeText(this, "Playing", Toast.LENGTH_SHORT).show();
			}
			else if (result == AudioManager.AUDIOFOCUS_REQUEST_FAILED)
			{
				mAudioFocused = false;
				log("audio focus failed", "w");
				//TODO find a better way to deal with this
				Toast.makeText(this, "Could not gain audio focus", Toast.LENGTH_SHORT).show();
			}
			
			
		}
	}
	
	public boolean onInfo(MediaPlayer mediaPlayer, int what, int extra)
	{
		log("onInfo()", "v");
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
			
			Notification notification = updateNotification("Playing", "Stop", true);
			mNotificationManager.notify(ONGOING_NOTIFICATION, notification);
			mCurrentPlayerState = ServiceRadioPlayer.STATE_PLAYING;
			return true;
			
		}
		else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START)
		{
			
			log("start buffering", "v");
			//Log.i(getPackageName(), "start buffering");
			//status.setText("Buffering...");
			
			Notification notification = updateNotification("Buffering", "Cancel", true);
			mNotificationManager.notify(ONGOING_NOTIFICATION, notification);
			

			mCurrentPlayerState = ServiceRadioPlayer.STATE_BUFFERING;
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
		log("onCompletion()", "v");
		mCurrentPlayerState = ServiceRadioPlayer.STATE_COMPLETE;
		//TODO update notification?
		if (mInterrupted)
		{
			Toast.makeText(this, "Playback completed after interruption", Toast.LENGTH_SHORT).show();	
			log("Playback completed after interruption, should restart when network connects again", "v");
		}
		else
		{
			//still an interruption, streaming shouldn't complete
			Toast.makeText(this, "Playback completed, no interruption reported", Toast.LENGTH_SHORT).show();
			log("Playback completed, no network interruption reported, restart", "v");
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
				mCurrentPlayerState = ServiceRadioPlayer.STATE_PLAYING;
				if (mInterrupted)
				{
					log("set interrupted = false", "v");
				}
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
		stopInfo(getResources().getString(R.string.widget_error_status)); //stopForeground(true);
		if (mediaPlayer != null)
		{
			try
			{
				if (!mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_PREPARING))
				{
					mediaPlayer.stop();	
				}
				log("media player stopped", "v");
			}
			catch (IllegalStateException e)
			{
				log("Illegal state to stop. uninitialized? not yet prepared?", "e");
			}
			mediaPlayer.release();
			mediaPlayer = null;
			mMediaPlayer = null;
		}
		String oldState = mCurrentPlayerState;
		mCurrentPlayerState = ServiceRadioPlayer.STATE_ERROR;
		//TextView status = (TextView) findViewById(R.id.status);
		String statusString = "";
		if (oldState.equals("Preparing"))
		{
			//TODO notify user somehow
			statusString = "An error occurred while trying to connect to the server. ";
			if (what == MediaPlayer.MEDIA_ERROR_UNKNOWN && extra == -2147483648)
			{
				statusString += "Bad URL or file format?";
			}
		}
		else
		{
			statusString = "Not error while preparing:"; 
		}
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
		Toast.makeText(this, statusString, Toast.LENGTH_LONG).show();
		if (oldState.equals("Preparing"))
		{
			stop();
		}
		return false;
		
	}
	

	@Override
	public void onAudioFocusChange(int focusChange) {
		if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
		{
			mAudioFocused = false;
			log("AUDIOFOCUS_LOSS_TRANSIENT", "v");
	        // Pause playback
			pause();
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
        	mAudioFocused = true;
        	log("AUDIOFOCUS_GAIN", "v");
        	// Resume playback or unduck
        	if (!isPlaying())
        	{
        		if (mPreset == 0)
        		{
        			//should this really start playing? 
        			play(1); //TODO get from storage
        		} 
        	}
        	else
        	{
        		if (mCurrentPlayerState.equals(STATE_PAUSED))
        		{
        			resume();
        		}
        		else
        		{
        			log("focus gained,  but not resumed?", "w");
        			//play();
        		}
        	}
        	unduck();
            
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
        	
        	log("AUDIOFOCUS_LOSS", "v");
        	if (mMediaButtonEventReceiverRegistered)
        	{
        		log("unregister button receiver", "v");
        		mAudioManager.unregisterMediaButtonEventReceiver(new ComponentName(getPackageName(), ReceiverRemoteControl.class.getName()));
            	mMediaButtonEventReceiverRegistered = false;	
        	}
        	else
        	{
        		log("button receiver already unregistered", "v");
        	}
        	if (mAudioFocused)
        	{
        		log("abandon focus", "v");
        		mAudioManager.abandonAudioFocus(this);
        		mAudioFocused = false;
        		
        	}
        	else
        	{
        		log("focus already abandoned", "v");
        	}
            // Stop playback
        	stop();

        }
        else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            // Lower the volume
        	log("AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK", "v");
        	duck();
        } 
		
	}
	
	private void duck()
	{
		log("duck()", "v");
		//mDucked = true;
		if (mMediaPlayer != null)
		{
			
			int volume = 20;
			float log1=(float)(Math.log(100-volume)/Math.log(100));
			log("lowering volume to " +  String.valueOf(log1), "v");
			mMediaPlayer.setVolume(1-log1, 1-log1);
			//mMediaPlayer.setVolume(leftVolume, rightVolume)
		}
	}
	
	private void unduck()
	{
		log("unduck()", "v");
		//mDucked = false;
		if (mMediaPlayer != null)
		{
			mMediaPlayer.setVolume(1, 1);
			
			//mMediaPlayer.setVolume(leftVolume, rightVolume)
		}
	}

	private void stopInfo(String status)
	{
		stopForeground(true);
		this.updateDetails(getResources().getString(R.string.widget_initial_title), status);
	}
	
	private void stopInfo()
	{
		this.stopInfo(getResources().getString(R.string.widget_stopped_status));
	}
	
	private void registerNetworkReceiver()
	{
		//register network receiver
		IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		if (mReceiver != null)
		{
			log("------------------------------------------", "v");
			log("network receiver already registered, find out why", "w"); 
			log("------------------------------------------", "v");
			this.unregisterReceiver(mReceiver);
			mReceiver = null;
		}
		mReceiver = new ReceiverNetwork();
        log("registering network change broadcast receiver", "v");
		this.registerReceiver(mReceiver, filter);


	}
	
	private void registerButtonReceiver()
	{
		//register media button receiver
		if (mButtonReceiver != null)
		{
			log("media button listener already registered", "v");
			this.unregisterReceiver(mButtonReceiver);
			mButtonReceiver = null;	
		}
		log("register media button listener", "v");
		mButtonReceiver = new ReceiverMediaButton();
		registerReceiver(mButtonReceiver, new IntentFilter(ACTION_MEDIA_BUTTON));
		
		//set ReceiverRemoteControl to be the sole receiver of media button actions
		if (mMediaButtonEventReceiverRegistered == false)
		{
			log("registering media button listener", "v");
			mAudioManager.registerMediaButtonEventReceiver(new ComponentName(getPackageName(), ReceiverRemoteControl.class.getName()));
			mMediaButtonEventReceiverRegistered = true;
		}
	}
	
	protected PendingIntent getPreviousIntent()
	{
		Intent intent = new Intent(this, ServiceRadioPlayer.class).setAction(ACTION_PREVIOUS);
		return PendingIntent.getService(this, 0, intent, 0);	 
	}
	
	protected PendingIntent getStopIntent()
	{
		Intent intent = new Intent(this, ServiceRadioPlayer.class).setAction(ACTION_STOP);
		return PendingIntent.getService(this, 0, intent, 0);	 
	}
	
	protected PendingIntent getNextIntent()
	{
		Intent intent = new Intent(this, ServiceRadioPlayer.class).setAction(ACTION_NEXT);
		return PendingIntent.getService(this, 0, intent, 0);	 
	}
	
	protected void play()
	{
		log("play()", "v");
		
		/*if (this.mUrl == null)
		{
			log("url not set", "e");
		}
		else
		{
			String str = "url:";
			str += this.mUrl;
			log(str, "v");
		}
		this.play(this.mUrl);*/
		if (mPreset == 0)
		{
			log("preset = 0", "e");
		}
		play(mPreset);
	}
	
	//called from bound ui, possibly notification action, if implemented
	//possible start states: any
	protected void play(int preset)
	{
		log("play(preset)", "v");
		if (preset == 0)
		{
			log("preset = 0", "e");
			throw new IllegalArgumentException("Preset = 0");
		}
		
		this.mPreset = preset; 
		mCurrentPlayerState = ServiceRadioPlayer.STATE_INITIALIZING;
		if (isConnected())
		{
			log("setting network type", "v");
			mNetworkState = this.getConnectionType();
		}
		else
		{
			//TODO handle this, let user know
			log("setting network state to disconnected", "v");
			mNetworkState = ServiceRadioPlayer.NETWORK_STATE_DISCONNECTED;
			//TODO ask user if it should play on network resume
			Toast.makeText(this, "tried to play with no network", Toast.LENGTH_LONG).show();
			log("no network, should be handled by ui? returning", "e");
			//if disconnected while preparing, mediaplayer is null, then getting here will stop playback attempts
			if (mInterrupted)
			{
				log("interrupted, so set state to playing so it will wait for network to resume", "v");
				mCurrentPlayerState = ServiceRadioPlayer.STATE_PLAYING;
				Notification notification = updateNotification("Waiting for network", "Cancel", true);
				mNotificationManager.notify(ONGOING_NOTIFICATION, notification);
			}
			else
			{
				log("wasn't interrupted, so remove any notifications and reset to uninitialized", "v");
				mCurrentPlayerState = ServiceRadioPlayer.STATE_UNINITIALIZED;
				if (mMediaPlayer != null)
				{
					log("media player not null, releasing", "v");
					mMediaPlayer.release();
					mMediaPlayer = null;
				}
				stopInfo(); //stopForeground(true);
			}
			
			return;
		}
		
		this.mPreset = preset;

		Uri uri = Uri.parse(ContentProviderRadio.CONTENT_URI_PRESETS.toString() + "/" + String.valueOf(preset));
		String[] projection = {DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER, DbContractRadio.EntryStation.COLUMN_NAME_TITLE, DbContractRadio.EntryStation.COLUMN_NAME_URL};  
		String selection = null;
		String[] selectionArgs = null;
		String sortOrder = DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER;
		Cursor cursor = getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
		int count = cursor.getCount();
		if (count < 1)
		{
			log("no results found", "e");
			throw new SQLiteException("Selected preset not found"); //TODO find correct exception to throw, or handle this some other way
		}
		else
		{
			cursor.moveToFirst();
			mPreset = (int)cursor.getLong(cursor.getColumnIndexOrThrow(DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER));
			mTitle = cursor.getString(cursor.getColumnIndexOrThrow(DbContractRadio.EntryStation.COLUMN_NAME_TITLE));
			mUrl = cursor.getString(cursor.getColumnIndexOrThrow(DbContractRadio.EntryStation.COLUMN_NAME_URL));	
		}
		
		//begin listen for headphones unplugged
		if (mNoisyReceiver == null)
		{
			mNoisyReceiver = new ReceiverNoisyAudioStream();
			IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
			registerReceiver(mNoisyReceiver, intentFilter);
			log("register noisy receiver", "v");	
		}
		else
		{
			log("noisy receiver already registered", "v");
		}
		
		
		//begin listen for phone call
		if (mPhoneReceiver == null)
		{
			mPhoneReceiver = new ReceiverPhoneCall();
			IntentFilter intentFilter = new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
			registerReceiver(mPhoneReceiver, intentFilter);
			log("register phone receiver", "v");	
		}
		else
		{
			log("phone receiver already registered", "v");
		}
		
		
		if (mMediaPlayer != null)
		{
			log("releasing old media player", "v");
			mMediaPlayer.release();
			mMediaPlayer = null;
		}
		log("creating new media player", "v");
		this.mMediaPlayer = new MediaPlayer();
		
		mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);	
		
		//str += mUrl;
		log("setting datasource for '" + mTitle + "' at '" + mUrl + "'", "v");
		try
		{
			mMediaPlayer.setDataSource(mUrl);
		}
		catch (IOException e) 
		{
			//TODO handle this somehow, let user know
			log("setting data source failed", "e");
			Toast.makeText(this, "Setting data source failed", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
		initializePlayer(mMediaPlayer); 

		mCurrentPlayerState = ServiceRadioPlayer.STATE_PREPARING;
		
		Notification notification = updateNotification("Preparing", "Cancel", true);
		//mNotificationManager.notify(ONGOING_NOTIFICATION, notification);
		startForeground(ONGOING_NOTIFICATION, notification);
		//updateDetails("Preparing");
	
		mMediaPlayer.prepareAsync(); // might take long! (for buffering, etc)
		log("preparing async", "v");
		
		Toast.makeText(this, "Preparing", Toast.LENGTH_SHORT).show();
	}
	
	protected void nextPreset()
	{
		log("nextPreset()", "v");
		mPreset++;
		Uri uri = Uri.parse(ContentProviderRadio.CONTENT_URI_PRESETS.toString() + "/" + String.valueOf(mPreset));
		String[] projection = {DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER};  
		String selection = null;
		String[] selectionArgs = null;
		String sortOrder = DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER;
		Cursor cursor = getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
		int count = cursor.getCount();
		if (count < 1 && mPreset == 1)
		{
			log("no stations, unless db integrity lost", "w");
			return; //TODO notify user?
		}
		else if (count < 1)
		{
			mPreset = 0; 
			log("incremented preset but nothing found, must be at end, start at 1", "v");
			this.nextPreset(); //try again, starting at 0
		}
		else
		{
			cursor.moveToFirst();
			mPreset = (int)cursor.getLong(cursor.getColumnIndexOrThrow(DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER));
			log("incremented preset, playing " + String.valueOf(mPreset), "v");
			play();	
		}
	}
	
	//FIXME duplicate code from radioContentProvider
	protected int getMaxPresetNumber()
	{
		Uri uri = ContentProviderRadio.CONTENT_URI_PRESETS_MAX;
		String[] projection = null;
		String selection = null;
		String[] selectionArgs = null;
		String sortOrder = null;
		Cursor cursor = getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
		long preset = 0;
		if (cursor.getCount() > 0)		
		{
			cursor.moveToFirst();
			preset = cursor.getLong(cursor.getColumnIndexOrThrow(DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER));	
		}
		
		cursor.close();
		return (int)preset;
	}
	
	protected void previousPreset()
	{
		log("previousPreset()", "v");
		mPreset--;
		if (mPreset <= 0)
		{
			////call() not supported until api 11
			//Uri maxUri = ContentProviderRadio.CONTENT_URI_PRESETS_MAX;
			//Bundle values = getContentResolver().call(maxUri, "getMaxPresetNumber", null, null);  
			//mPreset = values.getInt(DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER);
			mPreset = getMaxPresetNumber();
			if (mPreset == 0) //no stations? 
			{
				log("no stations, unless getMaxPresetNumber doesn't work", "w");
				return; //TODO notify user?
			}
			//play();
			//return;
		}
		//find out if the desired station exists
		Uri uri = Uri.parse(ContentProviderRadio.CONTENT_URI_PRESETS.toString() + "/" + String.valueOf(mPreset));
		String[] projection = {DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER};  
		String selection = null;
		String[] selectionArgs = null;
		String sortOrder = DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER;
		Cursor cursor = getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
		int count = cursor.getCount();
		if (count < 1)
		{
			log("no station found below current, but not 0", "e");
			throw new SQLiteException("No station found below current, but not 0"); //TODO find correct exception to throw, or handle this some other way
		}
		else
		{
			cursor.moveToFirst();
			mPreset = (int)cursor.getLong(cursor.getColumnIndexOrThrow(DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER));
			log("decremented preset, playing " + String.valueOf(mPreset), "v");
			play();	
		}
	}
	
	protected void setVolume(int newVolume)
	{
		AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		
		//int maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		//int newVolume = (int)(((float)(percent))/100*maxVolume);
		audio.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);
		/*log("setting volume to " + String.valueOf(percent), "v");
		float log1=(float)(Math.log(100-percent)/Math.log(100));
		log("lowering volume to " +  String.valueOf(log1), "v");
		mMediaPlayer.setVolume(1-log1, 1-log1);*/
		
		
	}
	
	protected Intent getDetailsUpdateIntent(String station, String status)
	{
		/*
		//Intent intent = new Intent(this, com.shinymayhem.radiopresets.WidgetProviderPresets.class);
		Intent intent = new Intent(ActivityMain.ACTION_UPDATE_TEXT);
		//intent.setAction(ActivityMain.ACTION_UPDATE_TEXT);
		intent.putExtra(com.shinymayhem.radiopresets.ActivityMain.EXTRA_STATION, station);
		intent.putExtra(com.shinymayhem.radiopresets.ActivityMain.EXTRA_STATUS, status);
        //intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
        */
		String artist = getResources().getString(R.string.initial_artist);
		String song = getResources().getString(R.string.initial_song);
		return this.getDetailsUpdateIntent(station, status, artist, song);
	}
	
	protected Intent getDetailsUpdateIntent(String station, String status, String artist, String song)
	{
		//Intent intent = new Intent(this, com.shinymayhem.radiopresets.WidgetProviderPresets.class);
		Intent intent = new Intent(ActivityMain.ACTION_UPDATE_TEXT);
		//intent.setAction(ActivityMain.ACTION_UPDATE_TEXT);
		intent.putExtra(com.shinymayhem.radiopresets.ActivityMain.EXTRA_STATION, station);
		intent.putExtra(com.shinymayhem.radiopresets.ActivityMain.EXTRA_STATUS, status);
		intent.putExtra(com.shinymayhem.radiopresets.ActivityMain.EXTRA_ARTIST, artist);
		intent.putExtra(com.shinymayhem.radiopresets.ActivityMain.EXTRA_SONG, song);
        //intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
	}
	
	//when no title specified, default to "[preset]. [station name]"
	protected void updateDetails(String text)
	{
		Intent intent = this.getDetailsUpdateIntent(String.valueOf(mPreset) + ". " + mTitle, text);
		//update widget. doesn't receive localbroadcasts
		//LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
		this.sendBroadcast(intent);
		
		
		
	}
	
	protected void updateDetails(String title, String status)
	{
		Intent intent = this.getDetailsUpdateIntent(title, status);
		//LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
		
		this.sendBroadcast(intent);
	}
	
	protected Notification updateNotification(String status, String stopText, boolean updateTicker)
	{
		
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
			.setContentTitle(String.valueOf(mPreset) + ". " + mTitle)
			.setContentText(status)
			//.addAction(R.drawable.av_previous, getResources().getString(R.string.previous), getPreviousIntent())
			//.addAction(R.drawable.av_stop, stopText, getStopIntent())
			//.addAction(R.drawable.av_next, getResources().getString(R.string.next), getNextIntent())
			.addAction(R.drawable.av_previous, null, getPreviousIntent())
			.addAction(R.drawable.av_stop, null, getStopIntent())
			.setUsesChronometer(true)
			.addAction(R.drawable.av_next, null, getNextIntent())
			.setLargeIcon(((BitmapDrawable)getResources().getDrawable(R.drawable.app_icon)).getBitmap())
			.setSmallIcon(R.drawable.app_notification);
		//PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
		//TODO taskstack builder only available since 4.1
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		stackBuilder.addParentStack(ActivityMain.class);
		//stackBuilder.addNextIntent(nextIntent)
		Intent resultIntent = new Intent(this, ActivityMain.class);
		//resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP); //why is this not needed? 
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent intent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_CANCEL_CURRENT);
		builder.setContentIntent(intent);
		if (updateTicker)
		{
			builder.setTicker(status);
			//startForeground(ONGOING_NOTIFICATION, builder.build());
		}
		else
		{
			//mNotificationManager.notify(ONGOING_NOTIFICATION, builder.build());
		}
		this.updateDetails(status);
		return builder.build();
	}
	
	protected void initializePlayer(MediaPlayer player)
	{
		log("initializePlayer()", "v");
		player.setOnPreparedListener(this);		
		player.setOnInfoListener(this);
		player.setOnCompletionListener(this);
		player.setOnErrorListener(this);
		//player.setOnBufferingUpdateListener(this);
		return;
	}
	
	
	protected void pause()
	{
		log("pause()", "v");
		if (mMediaPlayer == null)
		{
			log("null media player", "w");
		}
		else if (mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_PREPARING))
		{
			log("pause called while preparing", "v");
			mMediaPlayer.release();
			mMediaPlayer = null;
		}
		else
		{
			log("pause playback", "v");
			
			mMediaPlayer.stop();
			if (mAudioFocused)
			{
				log("abandon audio focus", "v");
				mAudioManager.abandonAudioFocus(this);
			}
			else
			{
				log("audio focus already abandoned", "w");
			}
			mMediaPlayer.release();
			mMediaPlayer = null;
			
				
		}
		Notification notification = updateNotification("Paused", "Cancel", false);
		mNotificationManager.notify(ONGOING_NOTIFICATION, notification);
		/*try
		{
			unregisterReceiver(mNoisyReceiver);
			mNoisyReceiver = null;
			log("unregistering noisyReceiver", "v");
		}
		catch (IllegalArgumentException e)
		{
			log("noisyReceiver already unregistered", "e");
		}*/
		mCurrentPlayerState = STATE_PAUSED;
	}
	
	protected void resume()
	{
		log("resume()", "v");
		/*mNoisyReceiver = new ReceiverNoisyAudioStream();
		IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
		registerReceiver(mNoisyReceiver, intentFilter);
		log("register noisy receiver", "v");*/
		play(); 
	}
	
	//called from onDestroy, end
	protected void stop()
	{
		log("stop()", "v");
		
		/*
		log("Stop button received, sending stop intent", "v");
		Intent intent = new Intent(this, ServiceRadioPlayer.class);
		intent.setAction(ServiceRadioPlayer.ACTION_STOP);
		startService(intent);*/
		
		//stop command called, reset interrupted flag
		if (mInterrupted)
		{
			log("set interrupted = false", "v");
		}
		mInterrupted = false;
		
		if (mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_STOPPED) || mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_END) || mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_UNINITIALIZED))
		{
			log("already stopped", "v");
			return;
		}
		
		if (mMediaPlayer == null)
		{
			mCurrentPlayerState = ServiceRadioPlayer.STATE_STOPPING;
			log("null media player", "w");
		}
		else if (mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_PREPARING))
		{
			mCurrentPlayerState = ServiceRadioPlayer.STATE_STOPPING; //store oldstate to move state change outside of conditionals
			log("stop called while preparing", "v");
			stopInfo(); //stopForeground(true);
			mMediaPlayer.release();
			mMediaPlayer = null;
		}
		else
		{
			mCurrentPlayerState = ServiceRadioPlayer.STATE_STOPPING;
			log("stopping playback", "v");
			Toast.makeText(this, "Stopping playback", Toast.LENGTH_SHORT).show();
			stopInfo(); //stopForeground(true);
			mMediaPlayer.stop();
			if (mAudioFocused == true)
			{
				log("abandon audio focus", "v");
				mAudioManager.abandonAudioFocus(this);
			}
			else
			{
				log("focus already abandoned", "w");
			}
			//mediaPlayer.reset();
			mMediaPlayer.release();
			mMediaPlayer = null;
				
		}
		
		if (mNoisyReceiver == null)
		{
			log("noisy receiver null, probably already unregistered", "v");
		}
		else
		{
			try
			{
				unregisterReceiver(mNoisyReceiver);
				
				log("unregistering noisyReceiver", "v");
			}
			catch (IllegalArgumentException e)
			{
				log("noisyReceiver already unregistered", "w");
			}
			mNoisyReceiver = null;
		}
		
		if (mPhoneReceiver == null)
		{
			log("mPhoneReceiver null, probably already unregistered", "v");
		}
		else
		{
			try
			{
				unregisterReceiver(mPhoneReceiver);
				log("unregistering phoneReceiver", "v");
			}
			catch (IllegalArgumentException e)
			{
				log("phoneReceiver already unregistered", "w");
			}
			mPhoneReceiver = null;
		}
		
		//clear any intents so player isn't started accidentally
		//log("experimental fix for service autostarting, redeliver-intent flag", "v");
		//mIntent.setAction(null);
		
		mCurrentPlayerState = ServiceRadioPlayer.STATE_STOPPED;
	}
	
	//called from unbound, headphones unplugged, notification->stop 
	protected void end()
	{
		log("end()", "v");
		stop();
		if (!mBound)
		{
			log("not bound, stopping service with stopself", "v");
			stopSelf();
			//Intent intent = new Intent(this, ServiceRadioPlayer.class);
			//stopSelf();
			//only stops service intent started with play action? not sure. not even sure if that would work
			//what should happen is 
			//intent.setAction(ServiceRadioPlayer.ACTION_PLAY);
			//this.stopService(intent);
			mCurrentPlayerState = ServiceRadioPlayer.STATE_END;
		} 
		else
		{
			String str = "still bound";
			log(str, "v");
			
		}
		
	}
	
	//called from onComplete or network change if no network and was playing
	/*
	protected void restart()
	{
		log("restart()", "v");
		mCurrentPlayerState = ServiceRadioPlayer.STATE_RESTARTING;
		
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
				log("old player playing, new player ready to set", "v");
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
		if (mCurrentPlayerState == ServiceRadioPlayer.STATE_COMPLETE)
		{
			log("playback completed", "e");
			return;
		}
		try
		{
			mMediaPlayer.setNextMediaPlayer(mNextPlayer);
			log("nextplayer set", "v");
		}
		catch (IllegalStateException e)
		{
			log("old player in wrong state to setnext", "e");
		}
		
		 
		
		//if (mediaPlayer.isPlaying())
		//{
		//	log("was still playing, stopping", "v");
		//	mediaPlayer.stop();	
		//}
		//log("restarting/preparing", "v");
		//mediaPlayer.prepareAsync();
		
		
		
		
		//Log.i(getPackageName(), "restarting playback");
	
		//if (playing)
		//{
		//	mediaPlayer.stop();	
		//	mediaPlayer.release();
		//	playing = false;
		//}
		
		//play();
	}
	*/
	
	public boolean isPlaying()
	{
		return (
				mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_BUFFERING) || 
				mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_PLAYING) ||
				mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_PAUSED) ||
				mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_PHONE) ||
				mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_PREPARING) ||
				mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_INITIALIZING) ||
				mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_COMPLETE) || //service should still stay alive and listen for network changes to resume
				mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_RESTARTING)
		);
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
		log(str, "v");
		return newState;
	}
	
	public static boolean validateUrl(String url)
	{	
		
		if (!URLUtil.isHttpUrl(url) && !URLUtil.isHttpsUrl(url))
		{
			//log("not a valid http or https url", "v");
			return false;
		}
		//check for empty after prefix
		if (url == "http://" || url == "https://")
		{
			return false;
		}
		
		MediaPlayer mediaPlayer = new MediaPlayer();
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		try
		{
			mediaPlayer.setDataSource(url);
			mediaPlayer.release();
		}
		catch (IOException e) 
		{
			return false;
		}
		return true;
	}
	

	@Override
	public IBinder onBind(Intent intent) {
		log("onBind()", "v");
		//Log.i(getPackageName(), "binding service");
		mBound=true;
		return mBinder;
	}
	
	@Override
	public void onRebind(Intent intent)
	{
		log("onRebind()", "v");
		//Log.i(getPackageName(), "rebinding service");
	}
	
	@Override
	public boolean onUnbind(Intent intent)
	{
		log("onUnbind()", "v");
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
		log("onDestroy()", "v");
		this.stop();
		Toast.makeText(this, "Stopping service", Toast.LENGTH_SHORT).show();
		//don't need to listen for network changes anymore
		if (mReceiver != null) {
			log("unregister network receiver", "v");
            this.unregisterReceiver(mReceiver);
            mReceiver = null;
        }
		else
		{
			log("unregistering network receiver failed, null", "e");
		}
		if (mButtonReceiver != null) {
			log("unregister media button receiver", "v");
            this.unregisterReceiver(mButtonReceiver);
            mButtonReceiver = null;
        }
		else
		{
			log("unregistering network receiver failed, null", "e");
		}
		 
	}
	
	
	
	public class ReceiverMediaButton extends BroadcastReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	log("received remote control action", "v");
	        if (ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
	        	KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
	        	switch (event.getKeyCode())
	        	{
	        		case KeyEvent.KEYCODE_MEDIA_PLAY:
	        			break;
	        		case KeyEvent.KEYCODE_MEDIA_NEXT:
	        		case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
	        			nextPreset();
	        			break;
	        		case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
	        		case KeyEvent.KEYCODE_MEDIA_REWIND:
	        			previousPreset();
	        			break;
	        		case KeyEvent.KEYCODE_MEDIA_PAUSE:
	        		case KeyEvent.KEYCODE_MEDIA_STOP:
	        			stop();
	        			break;
	        		case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
	        		case KeyEvent.KEYCODE_HEADSETHOOK:
	        			if (isPlaying() && !mCurrentPlayerState.equals(STATE_PAUSED))
	        			{
	        				pause();
	        			}
	        			else
	        			{
	        				if (mPreset == 0) //TODO store last played preset somewhere
	        				{
	        					play(1);	
	        				}
	        				else
	        				{
	        					resume();
	        				}
	        				
	        			}
	        		default:
	        			log("other button:" + String.valueOf(event.getKeyCode()), "v");
	        	}
	           
	        }
	        else
	        {
	        	log("other remote action?", "v");
	        }
	    }
	}
	
	private class ReceiverNoisyAudioStream extends BroadcastReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        if (!mCurrentPlayerState.equals(STATE_PHONE) && !mCurrentPlayerState.equals(STATE_PAUSED) && AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
	        	log("headphones unplugged", "v");
	        	end();
	        }
	        else
	        {
	        	log("headphones unplugged, but it is paused", "v");
	        }
	    }
	}
	
	private class ReceiverPhoneCall extends BroadcastReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	log("phone call receiver onReceive()", "v");
	    	String phoneState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
	    	if (phoneState.equals(TelephonyManager.EXTRA_STATE_RINGING))
	    	{
	    		log("phone ringing", "v");
	    		if (isPlaying())
	    		{
	    			pause();
	    			mCurrentPlayerState = STATE_PHONE;
	    		}
	    		
	    	}
	    	else if (phoneState.equals(TelephonyManager.EXTRA_STATE_IDLE) && mCurrentPlayerState.equals(STATE_PHONE))
	    	{
	    		log("resuming after phone call", "v");
	    		resume();
	    	}
	    	else
	    	{
	    		log("outgoing phone call?", "v");
	    		if (isPlaying())
	    		{
	    			pause();
	    			mCurrentPlayerState = STATE_PHONE;
	    		}
	    	}
	        
	    }
	}
	
	//handle network changes
	public class ReceiverNetwork extends BroadcastReceiver {   
	      
		@Override
		public void onReceive(Context context, Intent intent) {
			log("received network change broadcast", "v");
			//Log.i(getPackageName(), "received network change broadcast");
			if (mMediaPlayer == null && mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_PREPARING))
			{
				log("recover from disconnect while preparing", "v");
				//Log.i(getPackageName(), "no media player, don't care about connection updates");
				play();
				return;
			}
			else if (mMediaPlayer == null && !mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_UNINITIALIZED))
			{
				log("media player null, will cause problems if connected", "e");
			}
			else if (mMediaPlayer != null && mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_UNINITIALIZED))
			{
				log("-------------------------------", "d");
				log ("mediaPlayer not null, but uninitialized. how did this happen?", "w");
				log("-------------------------------", "d");
			}
			String str = "";
			if (isConnected())
			{
				int newState = getConnectionType();
				if (mNetworkState != newState)
				{
					
					str = "network type changed";
					if (mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_UNINITIALIZED))
					{
						str += " but uninitialized so it doesn't matter. ";
					}
					else if (mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_STOPPED) || mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_STOPPING))
					{
						str += " but stopped so it doesn't matter. ";
					}
					else if (mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_END))
					{
						str += " but ended so it doesn't matter. ";
					}
					else if (mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_PAUSED) || mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_PHONE))
					{
						str += " but paused so it doesn't matter. ";
					}
					else if (mInterrupted == false)
					{
						Notification notification = updateNotification("Network updated, reconnecting", "Cancel", true);
						mNotificationManager.notify(ONGOING_NOTIFICATION, notification);
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
					
					//if uninitialized, no preset picked yet. if stopped or paused, don't restart
					if (mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_UNINITIALIZED) || 
							mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_STOPPED) || 
							mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_STOPPING) ||
							mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_PAUSED) || 
							mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_PHONE) || 
							mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_END)
						)
					{
						log("unitialized or stopped/paused/ended, don't try to start or restart", "v");
						return;
					}
					boolean start = false;
					if (mMediaPlayer == null)
					{
						log("-------------------------------", "d");
						log("find out how we got here", "e");
						log("trying to play when not connected?", "v");
						log("disconnected while initializing? ", "v");
						log("-------------------------------", "d");
					}
					else
					{
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
						if (mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_COMPLETE) || start)
						{
							log("complete or start=true", "v");
							
						}
						//network interrupted playback but isn't done playing from buffer yet
						//set nextplayer and wait for current player to complete
						else  
						{
							log("!complete && start!=true", "v");
							//play();
							//TODO figure out if restart is possible, or too buggy
							//restart();
							//return;
						}
						
					}
					play();
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
				boolean alreadyDisconnected = (mNetworkState == ServiceRadioPlayer.NETWORK_STATE_DISCONNECTED);
				if (alreadyDisconnected)
				{
					str = "still ";
				}
				else if (isPlaying())
				{
					mInterrupted = true;
					log ("setting interrupted to true", "v");
				}
				str += "not connected. ";
				str += "old:" + mNetworkState;
				str += ", new:" + Integer.toString(ServiceRadioPlayer.NETWORK_STATE_DISCONNECTED);
				log(str, "v");
				
				log("setting network state ivar to disconnected", "v");
				mNetworkState = ServiceRadioPlayer.NETWORK_STATE_DISCONNECTED;
				

				//TODO figure out the best thing to do for each state
				
				if (mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_PREPARING)) //this will lead to a mediaioerror when it reaches prepared
				{
					mMediaPlayer.release();
					mMediaPlayer = null;
					Notification notification = updateNotification("Waiting for network", "Cancel", true);
					mNotificationManager.notify(ONGOING_NOTIFICATION, notification);
					log("-------------------------------", "d");
					log("network connection lost while preparing? set null mediaplayer", "d");
					log("-------------------------------", "d");
				}
				else if (mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_INITIALIZING))
				{
					if (alreadyDisconnected)
					{
						log("looks like it was trying to play when not connected", "d");
					}
					else
					{
						log("disconnected while initializing? this could be bad", "e");
					}
					
				}
				else if (mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_ERROR))
				{
					Notification notification = updateNotification("Error. Will resume?", "Cancel", true);
					mNotificationManager.notify(ONGOING_NOTIFICATION, notification);
					mMediaPlayer.release();
					mMediaPlayer = null;
					log("-------------------------------", "d");
					log("not sure what to do, how did we get here?", "d");
					log("-------------------------------", "d");
				}
				else if (mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_STOPPED))
				{
					log("disconnected while stopped, don't care", "v");
				}
				else if (mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_PLAYING))
				{
					Notification notification = updateNotification("Waiting for network", "Cancel", true);
					mNotificationManager.notify(ONGOING_NOTIFICATION, notification);
					log("disconnected while playing. should resume when network does", "v");
				}
				else if (mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_BUFFERING))
				{
					Notification notification = updateNotification("Waiting for network", "Cancel", true);
					mNotificationManager.notify(ONGOING_NOTIFICATION, notification);
					log("disconnected while buffering. should resume when network does", "v");
				}
				else if (mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_UNINITIALIZED))
				{
					if (mMediaPlayer == null)
					{
						log("disconnected while uninitialized", "v");
					}
					else
					{
						//TODO throw exception? handle silently?
						//might not cause problems, but shouldn't happen
						log("-------------------------------", "d");
						log("media player is not null, how did we get here?", "i");
						log("-------------------------------", "d");
					}
				}
				else
				{
					if (mPreset == 0)
					{
						Notification notification = updateNotification("bad state detected", "stop?", true);
						mNotificationManager.notify(ONGOING_NOTIFICATION, notification);
					}
					else
					{
						Notification notification = updateNotification("Waiting for network?", "Cancel", true);
						mNotificationManager.notify(ONGOING_NOTIFICATION, notification);
					}
					log("-------------------------------", "d");
					log("other state", "i");
					log("-------------------------------", "d");
				}
			}
			
			
		}
	}
	
	private void log(String text, String level)
	{
		mLogger.log(this, "State:" + mCurrentPlayerState + ":\t\t\t\t" + text, level);
	}
	
	
	public void clearLog()
	{
		//File file = getFileStreamPath(LOG_FILENAME);
		deleteFile(ActivityMain.LOG_FILENAME);
		//logging something should recreate the log file
		log("log file deleted", "i");
	}
	
	public void copyLog()
	{
		//String path = Environment.getExternalStorageDirectory().getAbsolutePath();
		String path = getExternalFilesDir(null).getAbsolutePath();
		
		File src = getFileStreamPath(ActivityMain.LOG_FILENAME); 
		File dst = new File(path + File.separator + ActivityMain.LOG_FILENAME);
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
			str += path + File.separator + ActivityMain.LOG_FILENAME;
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
