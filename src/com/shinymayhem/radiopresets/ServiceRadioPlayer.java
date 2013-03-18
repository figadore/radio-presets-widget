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
import java.util.HashMap;

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
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
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
	public final static String ACTION_UPDATE_WIDGET = "com.shinymayhem.radiopresets.ACTION_UPDATE_WIDGET";
	private String mCurrentPlayerState = STATE_UNINITIALIZED;
	public final static String STATE_UNINITIALIZED = "Uninitialized";
	public final static String STATE_INITIALIZING = "Initializing";
	public final static String STATE_PREPARING = "Preparing";
	public final static String STATE_WAITING_FOR_NETWORK = "Waiting for network";
	public final static String STATE_PLAYING = "Playing";
	public final static String STATE_BUFFERING = "Buffering";
	public final static String STATE_PAUSING = "Pausing";
	public final static String STATE_PAUSED = "Paused";
	public final static String STATE_PHONE = "Phone";
	public final static String STATE_ERROR = "Error";
	public final static String STATE_RESTARTING = "Restarting";
	public final static String STATE_STOPPING = "Stopping";
	public final static String STATE_STOPPED = "Stopped";
	public final static String STATE_COMPLETE = "Complete";
	public final static String STATE_END = "Ended";
	public final static String EXTRA_METADATA_ARTIST = "artist";
	public final static String EXTRA_METADATA_SONG = "song";
	public final static int NETWORK_STATE_DISCONNECTED = -1;
	public final static int METADATA_REFRESH_INTERVAL = 10000;
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
	protected String mArtist;
	protected String mSong;
	protected int mPreset;
	protected boolean mInterrupted = false;
	private final IBinder mBinder = new LocalBinder();
	protected boolean mBound = false;
	protected ActivityLogger mLogger = new ActivityLogger();
	protected Intent mIntent;
	protected NotificationManager mNotificationManager;
	protected Handler mMetadataHandler = new Handler(); 
	
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
		//listen for network changes and media buttons
		registerNetworkReceiver();
		registerButtonReceiver();
	}
	
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		log("onStartCommand()", "v");

		//check if resuming after close for memory, or other crash
		if ((flags & Service.START_FLAG_REDELIVERY) != 0)
		{
			log("investigate: intent redelivery, restarting. maybe handle this with dialog? notification with resume action?", "d");
		}
		
		mIntent = intent;
		if (intent == null)
		{
			log("no intent", "w");
		}
		else
		{
			String action = intent.getAction();
			if (action == null)
			{
				log("no action specified, not sure why", "w");
				//return flag indicating no further action needed if service is stopped by system and later resumes
				return START_NOT_STICKY;
			}
			else if (action.equals(Intent.ACTION_RUN)) //called when service is being bound by player activity
			{
				log("service being started probably so it can be bound", "v");
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
			else if (action.equals(ACTION_UPDATE_WIDGET.toString())) //Stop intent
			{
				log("UPDATE_WIDGET action in intent", "v");	
				updateDetails();
				endIfNotNeeded();
				
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

				//this.startForegroundNotification(getResources().getString(R.string.status_playing), getResources().getString(R.string.stop), true);
				this.updateNotification(getResources().getString(R.string.status_playing), getResources().getString(R.string.stop), true);
				
				
				log("start foreground notification: playing", "v");
				//Toast.makeText(this, "Playing", Toast.LENGTH_SHORT).show();
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
		if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END)
		{
			log("done buffering", "v");
			
			updateNotification(getResources().getString(R.string.status_playing), getResources().getString(R.string.stop), true);
			mCurrentPlayerState = ServiceRadioPlayer.STATE_PLAYING;
			return true;
			
		}
		else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START)
		{
			
			log("start buffering", "v");
			//Log.i(getPackageName(), "start buffering");
			//status.setText("Buffering...");
			
			updateNotification(getResources().getString(R.string.status_buffering), getResources().getString(R.string.cancel), true);
			

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
		/*
		//TODO: get nextmediaplayer working 
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
		*/
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
			stopAndReleasePlayer(mediaPlayer);
			mediaPlayer = null;
			mMediaPlayer = null;
		}
		String oldState = mCurrentPlayerState;
		mCurrentPlayerState = ServiceRadioPlayer.STATE_ERROR;
		//set 'now playing' to error
		stopInfo(getResources().getString(R.string.status_error));
		String title;
		String text;
		boolean unknown = false;
		if (what == MediaPlayer.MEDIA_ERROR_UNKNOWN && extra == -2147483648)
		{
			title = getResources().getString(R.string.error_title);
			text = getResources().getString(R.string.error_url_format);
		}
		else if (extra == MediaPlayer.MEDIA_ERROR_IO)
		{
			title = getResources().getString(R.string.error_title);
			text = getResources().getString(R.string.error_media_io);
		}
		else if (extra == MediaPlayer.MEDIA_ERROR_TIMED_OUT)
		{
			title = getResources().getString(R.string.error_title);
			text = getResources().getString(R.string.error_timed_out);
		}
		else //unknown/unhandled error
		{
			title = getResources().getString(R.string.error_title);
			text = getResources().getString(R.string.error_unknown);
			this.getErrorNotification(title, text);
			unknown = true;
			
		}
		//show user error notification
		this.getErrorNotification(title, text);
		
		if (unknown)
		{
			text += ", " + String.valueOf(what) + ":" + String.valueOf(extra);
		}
		log("Previous:" + oldState + "-" + text, "e");
		
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
        	if (shouldResumeOnAudioFocus())
        	{
        		if (mCurrentPlayerState.equals(STATE_PAUSED))
        		{
        			resume();
        		}
        		else
        		{
        			//find out which states this is run into from. change shouldResumeOnAudioFocus() accordingly (only resume when paused?)
        			log("investigate: focus gained,  but not resumed?", "d");
        		}
        	}
        	else
        	{
        		log("investigate: focused gained but should not try to resume", "d");
        		if (mPreset == 0)
        		{
        			
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
        	//this.abandonAudioFocus(); //already done in stop()->pause()
            // Stop playback
        	stop();

        }
        else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            // Lower the volume
        	log("AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK", "v");
        	duck();
        } 
		
	}
	
	//reduce player volume for notifications or other transient audio focus
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

	//stop foreground notification and update 'now playing' in widget and activity
	private void stopInfo(String status)
	{
		//remove notification
		stopForeground(true);
		//update widget and activity player
		mPreset = 0;
		mSong = "";
		mArtist = "";
		this.updateDetails(getResources().getString(R.string.widget_initial_station), status);
		//TODO store preset?
		
	}
	
	//convenience method with default "stopped" status
	private void stopInfo()
	{
		this.stopInfo(getResources().getString(R.string.status_stopped));
	}
	
	private void registerNetworkReceiver()
	{
		//register network receiver
		IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		if (mReceiver != null)
		{
			log("investigate: network receiver already registered, find out why", "d"); 
			this.unregisterReceiver(mReceiver);
			mReceiver = null;
		}
		mReceiver = new ReceiverNetwork();
        log("registering network change broadcast receiver", "v");
		this.registerReceiver(mReceiver, filter);
	}
	
	private void unregisterNetworkReceiver()
	{
		if (mReceiver == null)
		{
			log("network receiver null, probably already unregistered", "v");
		}
		else
		{
			log("unregistering network receiver", "v");
			this.unregisterReceiver(mReceiver);
			mReceiver = null;
		}
	}
	
	private void registerButtonReceiver()
	{
		//register media button receiver
		if (mButtonReceiver != null)
		{
			log("investigate: media button listener already registered", "v");
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
	
	private void unregisterButtonReceiver()
	{
		if (mButtonReceiver == null) {
			log("media button receiver null, probably already unregistered", "v");
        }
		else
		{
			log("unregister media button receiver", "v");
            this.unregisterReceiver(mButtonReceiver);
            mButtonReceiver = null;
		}
	}
	
	private void registerNoisyReceiver()
	{
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
	}
	
	private void unregisterNoisyReceiver()
	{
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
		
	}
	
	private void registerPhoneReceiver()
	{
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
	}
	
	private void unregisterPhoneReceiver()
	{
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
	}
	
	//convenience method, play instance variable stored preset
	protected void play()
	{
		log("play()", "v");
		if (mPreset == 0)
		{
			log("preset = 0", "e");
		}
		play(mPreset);
	}
	
	//play specified preset and start notification. asynchronous, so could be called from any state
	//sets required listeners for broadcast events like headphones unplugged, network change, phone call
	protected void play(int preset)
	{
		log("play(preset)", "v");
		if (preset == 0)
		{
			log("preset = 0", "e");
			throw new IllegalArgumentException("Preset = 0");
		}
		
		this.mPreset = preset; 
		
		setStationData(preset);
		if (!isConnected())
		{
			mCurrentPlayerState = ServiceRadioPlayer.STATE_WAITING_FOR_NETWORK;
			mNetworkState = ServiceRadioPlayer.NETWORK_STATE_DISCONNECTED;
			this.getWaitingForNetworkNotification();
		}
		else
		{
			mCurrentPlayerState = ServiceRadioPlayer.STATE_INITIALIZING;
			log("setting network type", "v");
			mNetworkState = this.getConnectionType();
			this.mPreset = preset;

			
			
			//begin listen for headphones unplugged
			registerNoisyReceiver();
			//begin listen for phone call
			registerPhoneReceiver();
			
			this.stopAndReleasePlayer(mMediaPlayer);
			
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
			
			this.startForegroundNotification(getResources().getString(R.string.status_preparing), getResources().getString(R.string.cancel), true);
			//mNotificationManager.notify(ONGOING_NOTIFICATION, notification);
			//startForeground(ONGOING_NOTIFICATION, notification);
			//updateDetails("Preparing");
		
			mMediaPlayer.prepareAsync(); // might take long! (for buffering, etc)
			log("preparing async", "v");
			
			//Toast.makeText(this, "Preparing", Toast.LENGTH_SHORT).show();
			log("get metadata", "v");
			
			mMetadataHandler.post(new Runnable(){

				@Override
				public void run() {
					log("run metadata handler runnable", "d");
					AsyncTaskMetadata task = new AsyncTaskMetadata();
					//TODO handle io exceptions, probably from this or sub function, probably network related
					if (isConnected() && shouldPlay())
					{
						task.execute(mUrl);
						mMetadataHandler.postDelayed(this, METADATA_REFRESH_INTERVAL);
					}
				}
				
			});
		}
		
		
	}

	//stop music but keep notification and mPreset
	protected void pause()
	{
		log("pause()", "v");
		mCurrentPlayerState = STATE_PAUSING;
		stopPlayer();
		updateNotification(getResources().getString(R.string.status_paused), getResources().getString(R.string.cancel), false);
		mCurrentPlayerState = STATE_PAUSED;

	}
	
	protected void resume()
	{
		log("resume()", "v");
		play(); 
	}
	
	//safe way to completely stop mediaplayer from any state
	protected void stopAndReleasePlayer(MediaPlayer player)
	{
		log("stopAndReleasePlayer()", "v");
		if (player != null)
		{
			try
			{
				if (player.isPlaying())
				{
					player.stop();
					log("stopped mediaPlayer", "v");
				}
				log("mediaPlayer not playing", "v");
			}
			catch (IllegalStateException e)
			{
				log("player in wrong state to stop", "v");
			}
			try
			{
				player.reset();
				log("reset mediaPlayer", "v");
			}
			catch (IllegalStateException e)
			{
				log("player in wrong state to reset", "v");
			}
			player.release();
		}
	}
	
	//stops music and related broadcast listeners
	protected void stopPlayer()
	{
		this.stopAndReleasePlayer(mMediaPlayer);
		this.abandonAudioFocus();
		this.unregisterNoisyReceiver();
		if (mInterrupted)
		{
			log("set interrupted = false", "v");
		}
		mInterrupted = false;
	}
	
	//stop music, notifications, and put in stopped state
	protected void stop()
	{
		log("stop()", "v");
		mCurrentPlayerState = ServiceRadioPlayer.STATE_STOPPING;
		this.stopPlayer();
		this.stopInfo();
		this.unregisterPhoneReceiver();
		
		mCurrentPlayerState = ServiceRadioPlayer.STATE_STOPPED;
	}
	
	//e.g. called from unbound, headphones unplugged, notification->stop
	//end service if not bound, do any cleanup necessary
	protected void end()
	{
		log("end()", "v");
		stop();
		if (!mBound)
		{
			log("not bound, stopping service with stopself", "v");
			stopSelf();
			mCurrentPlayerState = ServiceRadioPlayer.STATE_END;
		} 
		else
		{
			String str = "still bound";
			log(str, "v");
		}
		
	}
	
	//end service if it isn't playing or going to play
	protected void endIfNotNeeded()
	{
		if (!couldPlay())
		{
			log("no reason to keep service alive", "v");
			end();	
		}
		else
		{
			log("could play, don't destroy service","v");
		}
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
	
	protected void setVolume(int newVolume)
	{
		AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		audio.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);
		log("volume set:" + String.valueOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC)), "v");
	}
	
	//convenience method
	protected Intent getDetailsUpdateIntent(String station, String status)
	{
		return this.getDetailsUpdateIntent(station, status, getArtist(), getSong(), getPlayingPreset());
	}
	
	protected Intent getDetailsUpdateIntent(String station, String status, String artist, String song, int preset)
	{
		//Intent intent = new Intent(this, com.shinymayhem.radiopresets.WidgetProviderPresets.class);
		Intent intent = new Intent(ActivityMain.ACTION_UPDATE_TEXT);
		//intent.setAction(ActivityMain.ACTION_UPDATE_TEXT);
		intent.putExtra(com.shinymayhem.radiopresets.ActivityMain.EXTRA_PRESET, preset);
		intent.putExtra(com.shinymayhem.radiopresets.ActivityMain.EXTRA_STATION, station);
		intent.putExtra(com.shinymayhem.radiopresets.ActivityMain.EXTRA_STATUS, status);
		intent.putExtra(com.shinymayhem.radiopresets.ActivityMain.EXTRA_ARTIST, artist);
		intent.putExtra(com.shinymayhem.radiopresets.ActivityMain.EXTRA_SONG, song);
        //intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
	}
	
	//called whenever widget or activity player details need updating
	public void updateDetails()
	{ 
		updateDetails(getStation(), getStatus());
	}
		
	//when no title specified, default to "[preset]. [station name]"
	protected void updateDetails(String status)
	{
		this.updateDetails(String.valueOf(mPreset) + ". " + mTitle, status);
	}
	
	protected void updateDetails(String station, String status)
	{
		//TODO async task? possibly causing slow responses
		Intent intent = this.getDetailsUpdateIntent(station, status);
		this.sendBroadcast(intent);
		log("sent broadcast", "v");
	}
	
	/*
	//if returning sticky or restart flag in onCreate, use this, allow tap to resume music upon service restarted
	protected NotificationCompat.Builder getResumeNotification()
	{
		//TODO
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
	            ;
		return builder;
	}*/
	       
	//notify user of error, tap to open app
	protected void getErrorNotification(String title, String text)
	{
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
			.setContentTitle(title)
			.setContentText(text)
			.setLargeIcon(((BitmapDrawable)getResources().getDrawable(R.drawable.app_icon)).getBitmap())
			.setSmallIcon(R.drawable.app_notification);
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		stackBuilder.addParentStack(ActivityMain.class);
		Intent resultIntent = new Intent(this, ActivityMain.class); 
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent intent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_CANCEL_CURRENT);
		builder.setContentIntent(intent);
		builder.setTicker(title);
		Notification notification = builder.build();
		notification.flags = Notification.FLAG_AUTO_CANCEL; //set notification to be cleared when tapped
		//startForeground(ONGOING_NOTIFICATION, builder.build());
	    mNotificationManager.notify(ONGOING_NOTIFICATION, notification);
	}
	
	//notify user that the service is running and will play once network is available
	protected void getWaitingForNetworkNotification()
	{
		String status = getResources().getString(R.string.status_waiting_for_network);
	    NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
			.setContentTitle(String.valueOf(mPreset) + ". " + mTitle)
			.setContentText(status)
			.addAction(R.drawable.content_remove, getResources().getString(R.string.cancel), getStopIntent())
			.setUsesChronometer(true)
			.setLargeIcon(((BitmapDrawable)getResources().getDrawable(R.drawable.app_icon)).getBitmap())
			.setSmallIcon(R.drawable.app_notification);
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		stackBuilder.addParentStack(ActivityMain.class);
		Intent resultIntent = new Intent(this, ActivityMain.class); 
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent intent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_CANCEL_CURRENT);
		builder.setContentIntent(intent);
		builder.setTicker(status);
		this.updateDetails(status);
		startForeground(ONGOING_NOTIFICATION, builder.build());
	    //mNotificationManager.notify(ONGOING_NOTIFICATION, builder.build());
	}
	
	//get notification for specified status, allow further actions by returning builder
	protected NotificationCompat.Builder getUpdateNotification(String status, String stopText, boolean updateTicker)
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
		return builder;
	}
	
	//start foreground notification 
	protected void startForegroundNotification(String status, String stopText, boolean updateTicker)
	{
		log("startForegroundNotification()", "v");
		NotificationCompat.Builder builder = this.getUpdateNotification(status, stopText, updateTicker);
		startForeground(ONGOING_NOTIFICATION, builder.build());
	}
	
	//update existing foreground notification (or create new non-foreground notification if it doesn't exist)
	protected void updateNotification(String status, String stopText, boolean updateTicker)
	{
		log("updateNotification()", "v");
		NotificationCompat.Builder builder = this.getUpdateNotification(status, stopText, updateTicker);
		mNotificationManager.notify(ONGOING_NOTIFICATION, builder.build());
		
	}

	//get currently playing preset number
	public int getPlayingPreset()
	{
		int preset = 0;
		if (shouldReturnPreset())
		{
			preset = mPreset;
		}
		return preset;
	}
	
	//get preset, gets even when stopped (if played and still running)
	public int getPresets()
	{
		return mPreset;
	}
	
	//get currently playing station, if applicable
	private String getStation()
	{
		String station = getResources().getString(R.string.widget_initial_station);
		if (mPreset != 0)
		{
			station = String.valueOf(mPreset) + ". " + mTitle;
		}
		return station;
	}
	
	//get current player status
	private String getStatus()
	{
		String status = getResources().getString(R.string.status_stopped);
		if (mCurrentPlayerState.equals(STATE_BUFFERING))
		{
			status = getResources().getString(R.string.status_stopped);
		}
		else if (mCurrentPlayerState.equals(STATE_PREPARING))
		{
			status = getResources().getString(R.string.status_preparing);
		}
		else if (!isConnected() && (mCurrentPlayerState.equals(STATE_PLAYING) || mCurrentPlayerState.equals(STATE_PREPARING)))
		{
			status = getResources().getString(R.string.status_waiting_for_network);
		}
		else if (mCurrentPlayerState.equals(STATE_PLAYING))
		{
			status = getResources().getString(R.string.status_playing);
		}
		else if (mCurrentPlayerState.equals(STATE_PAUSED) || mCurrentPlayerState.equals(STATE_PHONE))
		{
			status = getResources().getString(R.string.status_paused);
		}
		return status;
	}
	
	//get currently playing artist, if applicable
	private String getArtist()
	{
		String artist;
		if (mArtist == null || mArtist == "")
		{
			artist = getResources().getString(R.string.unknown_artist);	
		}
		else
		{
			artist = mArtist;
		}
		return artist;
	}
	
	//get currently playing song, if applicable
	private String getSong()
	{
		String song;
		if (mSong == null || mSong == "")
		{
			song = getResources().getString(R.string.unknown_song);	
		}
		else
		{
			song = mSong;
		}
		return song;
	}
	
	protected void setStationData(int preset)
	{
		Uri uri = Uri.parse(ContentProviderRadio.CONTENT_URI_PRESETS.toString() + "/" + String.valueOf(preset));
		String[] projection = {DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER, DbContractRadio.EntryStation.COLUMN_NAME_TITLE, DbContractRadio.EntryStation.COLUMN_NAME_URL};  
		String selection = null;
		String[] selectionArgs = null;
		String sortOrder = DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER;
		Cursor cursor = getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
		int count = cursor.getCount();
		if (count > 1)
		{
			log("Duplicate preset detected", "w");
		}
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
			mArtist = getResources().getString(R.string.loading_artist);
			mSong = getResources().getString(R.string.loading_song);
		}
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
	
	protected void abandonAudioFocus()
	{
		if (mAudioFocused)
		{
			log("abandon audio focus", "v");
			mAudioManager.abandonAudioFocus(this);
			mAudioFocused = false;
		}
		else
		{
			log("audio focus already abandoned", "v");
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
	
	//whether service is currently playing music or playback is pending 
	protected boolean shouldPlay()
	{
		return (
				mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_PLAYING)
				|| mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_COMPLETE) 
				|| mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_INITIALIZING) 
				|| mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_BUFFERING) 
				|| mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_PREPARING) 
				|| mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_RESTARTING)
				|| mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_WAITING_FOR_NETWORK)
				//|| mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_ERROR) 
				//|| mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_UNINITIALIZED)  
				//|| mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_STOPPED) 
				//|| mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_STOPPING) 
				//|| mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_PAUSED)  
				//|| mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_PHONE)  
				//|| mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_END) 
				
				
			);
	}
	
	//whether music is ready to be played, i.e. state probably has preset set
	//also used for determining whether service should stay alive when unbound
	protected boolean couldPlay()
	{
		return (
				this.shouldPlay() 
				|| mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_PAUSED)
				|| mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_PAUSING) 
				|| mCurrentPlayerState.equals(ServiceRadioPlayer.STATE_PHONE)  
		);
	}
	
	//when audio focus is regained, whether playback should resume
	//what happens when audio focus gained again while buffering, for example
	protected boolean shouldResumeOnAudioFocus()
	{
		return this.shouldPlay();
	}
	
	//when service is unbound by main activity, whether service should be ended
	protected boolean shouldEndOnUnbind()
	{
		return !this.couldPlay();
	}
	
	protected boolean shouldReturnPreset()
	{
		return this.couldPlay();
	}
	
	protected boolean shouldPauseOnPauseButton()
	{
		return this.shouldPlay();
	}
	
	//ringing or outgoing call, whether player should pause
	protected boolean shouldPauseOnPhone()
	{
		return this.shouldPlay();
	}
	
	protected boolean shouldStartOnReconnect()
	{
		return this.shouldPlay();
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
		if (url.equals("http://") || url.equals("https://"))
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
		if (!mBound)
		{
			log("investigate: this hopefully shouldn't happen", "d");
		}
		mBound = true;
		//Log.i(getPackageName(), "rebinding service");
	}
	
	@Override
	public boolean onUnbind(Intent intent)
	{
		log("onUnbind()", "v");
		//Log.i(getPackageName(), "unbinding service");
		mBound = false;
		if (shouldEndOnUnbind())
		{
			end();
		}
		return false;
	}
	
	@Override
	public void onDestroy() {
		log("onDestroy()", "v");
		this.stop();
		unregisterNetworkReceiver();
		unregisterButtonReceiver();
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
	        			if (shouldPauseOnPauseButton())
	        			{
	        				pause();
	        			}
	        			else
	        			{
	        				if (mPreset == 0) //TODO get stored previously played preset 
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
	    		if (shouldPauseOnPhone())
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
	    		if (shouldPauseOnPhone())
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
			
			if (shouldStartOnReconnect())
			{
				int oldNetworkState = mNetworkState;
				
				if (isConnected())
				{
					int newNetworkState = getConnectionType();
					if (mNetworkState != newNetworkState) 
					{
						log("connected or reconnected. new network type:" + String.valueOf(newNetworkState) + ", old network type:" + String.valueOf(mNetworkState) + ". try to play", "v");
						mInterrupted = true;
						play();	
					}
					else
					{
						log("same network type", "v");
					}
				}
				else //disconnected
				{
					if (oldNetworkState == ServiceRadioPlayer.NETWORK_STATE_DISCONNECTED)
					{
						log("still disconnected", "v");
					}
					else 
					{
						mInterrupted = true;
						getWaitingForNetworkNotification();
						log("interrupted disconnected while playing. should resume when network does", "v");
					}	
				}
			}
			else
			{
				log("doesn't need to start on reconnect", "v");
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
			}
			else
			{
				log("sd file exists?", "v");
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

	public class AsyncTaskMetadata extends AsyncTask<String, Void, HashMap<String, String>> {

		@Override
		protected HashMap<String, String> doInBackground(String... urls) {
			String url = urls[0];
			
			HashMap<String, String> map = new HashMap<String, String>();
			String artist = "";
			String song = "";
			
			MetadataParser parser = new MetadataParser();
			boolean parses = parser.setUrl(url);
			if (parses)
			{
				artist = parser.getArtist();
				song = parser.getSong();	
			}
			
			
			map.put("artist", artist);
			map.put("song", song);
			return map;
		}
		
		
		//TODO find out if it is ok that this is an inner class (what if service dies before onPostExecute is reached?)
		@Override protected void onPostExecute(HashMap<String, String> map)
		{
			String artist = map.get("artist");
			String song = map.get("song");
			mArtist = artist;
			mSong = song;
			updateDetails();
		}

	}
	
}
