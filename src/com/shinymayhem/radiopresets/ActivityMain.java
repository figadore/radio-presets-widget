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
import android.graphics.drawable.Drawable;
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
import android.widget.ImageButton;
import android.widget.TextView;

import com.shinymayhem.radiopresets.DbContractRadio.DbHelperRadio;
import com.shinymayhem.radiopresets.DialogFragmentAdd.ListenerAddDialog;
import com.shinymayhem.radiopresets.DialogFragmentEvent.ListenerEventDialog;
import com.shinymayhem.radiopresets.FragmentPlayer.PlayerListener;
import com.shinymayhem.radiopresets.FragmentStations.PresetListener;
import com.shinymayhem.radiopresets.ServiceRadioPlayer.LocalBinder;

public class ActivityMain extends FragmentActivity implements ListenerAddDialog, ListenerEventDialog, PresetListener, PlayerListener {
    
    public static final boolean LOCAL_LOGV = true; //also change for other packages (metadata)
    public static final boolean LOCAL_LOGD = true;
    private static final String TAG = "ActivityMain";
    //actions
    public final static String ACTION_UPDATE_INFO = "com.shinymayhem.radiopresets.mainactivity.ACTION_UPDATE_INFO";
    //extras
    public final static String EXTRA_STATION_PRESET = "com.shinymayhem.radiopresets.mainactivity.STATION_ID";
    public final static String EXTRA_STATION = "com.shinymayhem.radiopresets.mainactivity.EXTRA_STATION";
    public final static String EXTRA_STATUS = "com.shinymayhem.radiopresets.mainactivity.EXTRA_STATUS";
    public final static String EXTRA_ARTIST = "com.shinymayhem.radiopresets.mainactivity.EXTRA_ARTIST";
    public final static String EXTRA_SONG = "com.shinymayhem.radiopresets.mainactivity.EXTRA_SONG";
    public final static String EXTRA_PRESET = "com.shinymayhem.radiopresets.mainactivity.EXTRA_PRESET";
    public final static String EXTRA_LIKED = "com.shinymayhem.radiopresets.mainactivity.EXTRA_LIKED";
    public final static String EXTRA_DISLIKED = "com.shinymayhem.radiopresets.mainactivity.EXTRA_DISLIKED";
    //loader manager
    public static final int LOADER_STATIONS = 0;
    
    protected boolean mBound = false;
    protected ServiceRadioPlayer mService;
    protected DbHelperRadio mDbHelper;
    protected ActivityLogger mLogger;
    protected ReceiverDetails mDetailsReceiver;
    
    private ServiceConnection mConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            if (LOCAL_LOGV) log("service connected", "v");
            LocalBinder binder = (LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            updateDetails();
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0)
        {
            if (LOCAL_LOGV) log("service disconnected", "v");
            mBound = false;
        }
    };
    
    
    protected Context getContext()
    {
        return this;
    }
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mLogger = new ActivityLogger(this);
        super.onCreate(savedInstanceState);
        if (LOCAL_LOGV) log("creating main activity", "v");
        
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
        if (LOCAL_LOGV) log("starting main activity", "v");
        bindRadioPlayer();
    }
    
    protected void bindRadioPlayer()
    {
        if (LOCAL_LOGV) log("binding radio player", "v");
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
        if (LOCAL_LOGV) log("restarting main activity", "v");
    }
    
    protected void onResume()
    {
        super.onResume();
        if (LOCAL_LOGV) log("resuming main activity", "v");
        //while app is visible, volume buttons should adjust music stream volume
        if (LOCAL_LOGV) log("setting volume control stream", "v");
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        
        registerDetailsReceiver();
        
        //update player details
        //if (mService == null)
        //{
//          if (LOCAL_LOGV) log("Service not bound yet", "e");
    //  }
        //mService.updateDetails();
    }
    
    //register for details updates
    private void registerDetailsReceiver()
    {
        //register network receiver
        IntentFilter filter = new IntentFilter(ActivityMain.ACTION_UPDATE_INFO);
        if (mDetailsReceiver != null)
        {
            if (LOCAL_LOGV) log("------------------------------------------", "v");
            if (LOCAL_LOGV) log("details receiver already registered, find out why", "w"); 
            if (LOCAL_LOGV) log("------------------------------------------", "v");
            //widget doesn't accept localbroadcasts
            //LocalBroadcastManager.getInstance(this).unregisterReceiver(mDetailsReceiver);
            this.unregisterReceiver(mDetailsReceiver);
            mDetailsReceiver = null;
        }
        mDetailsReceiver = new ReceiverDetails();
        if (LOCAL_LOGV) log("registering details broadcast receiver", "v");
        //widget doesn't accept localbroadcasts
        //LocalBroadcastManager.getInstance(this).registerReceiver(mDetailsReceiver, filter); 
        this.registerReceiver(mDetailsReceiver, filter);
    }
    
    private void unregisterDetailsReceiver()
    {
        if (mDetailsReceiver == null)
        {
            if (LOCAL_LOGV) log("mDetailsReceiver null, probably already unregistered", "v");
        }
        else
        {
            try
            {
                //widget doesn't accept localbroadcasts
                //LocalBroadcastManager.getInstance(this).unregisterReceiver(mDetailsReceiver);
                this.unregisterReceiver(mDetailsReceiver);
                if (LOCAL_LOGV) log("unregistering details receiver", "v");
            }
            catch (IllegalArgumentException e)
            {
                if (LOCAL_LOGV) log("details receiver already unregistered", "w");
            }
            mDetailsReceiver = null;    
        }
    }
    
    @Override
    protected void onStop()
    {
        if (LOCAL_LOGV) log("stopping main activity", "v");
        
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
        if (LOCAL_LOGV) log("pausing main activity", "v");
        super.onPause();
        unregisterDetailsReceiver();
        
    }
    
    public void onDestroy()
    {
        if (LOCAL_LOGV) log("onDestroy()", "v");
        super.onDestroy();
    }

    
    @Override
    public void onDialogPositiveClick(View view) {
        // User touched the dialog's positive button
        if (LOCAL_LOGD) log("Add station confirmed", "d");
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
            Uri uri = getContentResolver().insert(ContentProviderRadio.CONTENT_URI_STATIONS, values);
            int id = (int) ContentUris.parseId(uri);
            if (id == -1)
            {
                throw new SQLiteException("Insert failed");
            }
            if (LOCAL_LOGV) log("uri of addition:" + uri, "v");
            this.updateDetails();
        }
        else
        {
            //FIXME code duplication in FragmentStations
            if (LOCAL_LOGV) log("URL " + url + " not valid", "v");
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
        if (LOCAL_LOGD) log("add station cancelled", "d");
    }
    
    @Override
    public void onDialogEventPositiveClick(DialogFragment dialogFragment) {
        log("event details", "i");
        EditText detailsView = (EditText)dialogFragment.getDialog().findViewById(R.id.event_details);
        if (LOCAL_LOGV) log("--------------------{-----------------", "i");
        if (LOCAL_LOGV) log(detailsView.getText().toString(), "i");
        if (LOCAL_LOGV) log("--------------------}-----------------", "i");
    }


    @Override
    public void onDialogEventNegativeClick(DialogFragment dialogFragment) {
        if (LOCAL_LOGV) log("--------------------{-----------------", "i");
        if (LOCAL_LOGV)  log("event details cancelled", "i");
        if (LOCAL_LOGV) log("--------------------}-----------------", "i");
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
            if (LOCAL_LOGV) log("--------------------------------------", "i");
            if (LOCAL_LOGV) log("Event button pressed", "i");
            if (LOCAL_LOGV) log("--------------------------------------", "i");
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
        if (LOCAL_LOGD) log("Play button received, sending play intent", "d");
        Intent intent = new Intent(this, ServiceRadioPlayer.class);
        intent.setAction(ServiceRadioPlayer.ACTION_PLAY);
        intent.putExtra(EXTRA_STATION_PRESET, id);
        startService(intent);
        //mService.play(url);
    }
    
    public void setVolume(int volume)
    {
        if (LOCAL_LOGV) log("setVolume()", "v");
        mService.setVolume(volume);
    }
    

    public void stop(View view)
    {
        if (LOCAL_LOGV) log("stop()", "v");
        mService.stop();
    }
    
    public void next(View view)
    {
        if (LOCAL_LOGV) log("next()", "v");
        mService.nextPreset();
    }
    
    public void prev(View view)
    {
        if (LOCAL_LOGV) log("prev()", "v");
        mService.previousPreset();
    }
    
    public void like(View view)
    {
        ImageButton button = (ImageButton)view;
        String tag = (String) button.getTag();
        
        if (tag.equals("selected"))
        {
            this.unsetLiked(button);
            //this.updateDetails();
        }
        else if (tag.equals("unselected"))
        {
            this.setLiked(button);
            //this.updateDetails();
        }
        else
        {
            throw new IllegalStateException("Button not selected or unselected");
        }
        
    }

    public void dislike(View view)
    {
        ImageButton button = (ImageButton)view;
        String tag = (String) button.getTag();
        
        if (tag.equals("selected"))
        {
            this.unsetDisliked(button);
            //this.updateDetails();
        }
        else if (tag.equals("unselected"))
        {
            this.setDisliked(button);
            //this.updateDetails();
        }
        else
        {
            throw new IllegalStateException("Button not selected or unselected");
        }
    }
    
    public void setLiked(ImageButton button)
    {
        this.setLiked(button, false);
    }
    
    /**
     * Sets image button view
     * @param button
     * @param override whether the button image should be changed regardless of whether service method worked
     */
    public void setLiked(ImageButton button, boolean override)
    {
        if ((mService != null && mService.like()) || override)
        {
            Drawable selected = getResources().getDrawable(R.drawable.song_like_selected);
            button.setImageDrawable(selected);
            button.setTag("selected");  
        }
    }
    
    public void unsetLiked(ImageButton button)
    {
        this.unsetLiked(button, false);
    }
    
    /**
     * Sets image button view
     * @param button
     * @param override whether the button image should be changed regardless of whether service method worked
     */
    public void unsetLiked(ImageButton button, boolean override)
    {
        if ((mService != null && mService.unlike()) || override)
        {
            Drawable unselected = getResources().getDrawable(R.drawable.song_like);
            button.setImageDrawable(unselected);
            button.setTag("unselected");
        }
        
    }
    
    
    /**
     * Sets image button view
     * @param button
     * @param override whether the button image should be changed regardless of whether service method worked
     */
    public void setDisliked(ImageButton button, boolean override)
    {
        if ((mService != null && mService.dislike()) || override)
        {
            Drawable selected = getResources().getDrawable(R.drawable.song_dislike_selected);
            button.setImageDrawable(selected);
            button.setTag("selected");
        }
    }
    
    public void setDisliked(ImageButton button)
    {
        this.setDisliked(button, false);
    }
    
    /**
     * Sets image button view
     * @param button
     * @param override whether the button image should be changed regardless of whether service method worked
     */
    public void unsetDisliked(ImageButton button, boolean override)
    {
        if ((mService != null && mService.undislike()) || override)
        {
            Drawable unselected = getResources().getDrawable(R.drawable.song_dislike);
            button.setImageDrawable(unselected);
            button.setTag("unselected");
        }
        
    }
    
    public void unsetDisliked(ImageButton button)
    {
        this.unsetDisliked(button, false);
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
    

    
    protected void setActivityPlayerDetails(String station, String status, String artist, String song, boolean liked, boolean disliked)
    {
        TextView songText = (TextView) this.findViewById(R.id.player_song_details);
        TextView stationText = (TextView) this.findViewById(R.id.player_station_details);
        songText.setText(artist + " - " + song);
        stationText.setText(station+ " : " + status);
        ImageButton likeButton = (ImageButton) findViewById(R.id.like_button);
        ImageButton dislikeButton = (ImageButton) findViewById(R.id.dislike_button);
        if (liked)
        {
            this.setLiked(likeButton);
            this.unsetDisliked(dislikeButton);
        }
        else if (disliked)
        {
            this.unsetLiked(likeButton);
            this.setDisliked(dislikeButton);
        }
        else
        {
            this.unsetLiked(likeButton);
            this.unsetDisliked(dislikeButton);
        }
    }
    
    public int getPlayingPreset()
    {
        //log("getPreset()", "v");
        int preset;
        if (mService == null)
        {
            //if (LOCAL_LOGV) log("service not bound", "w");
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
        if (LOCAL_LOGV) log("updateDetails()", "v");
        if (mService == null)
        {
            if (LOCAL_LOGV) log("service not bound", "e");
        }
        else
        {
            mService.updateDetails();   
        }
        
        
    }

    private void log(String text, String level)
    {
        mLogger.log(TAG, text, level);
    }
    
    public class ReceiverDetails extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ActivityMain.ACTION_UPDATE_INFO))
            {
                if (LOCAL_LOGV) log("activity onreceive(), updating main activity text", "v");
                Bundle extras = intent.getExtras();
                String station = extras.getString(ActivityMain.EXTRA_STATION);
                String status = extras.getString(ActivityMain.EXTRA_STATUS);
                String artist = extras.getString(ActivityMain.EXTRA_ARTIST);
                String song = extras.getString(ActivityMain.EXTRA_SONG);
                boolean liked = extras.getBoolean(ActivityMain.EXTRA_LIKED);
                boolean disliked = extras.getBoolean(ActivityMain.EXTRA_DISLIKED);
                //int preset = extras.getInt(ActivityMain.EXTRA_PRESET);
                setActivityPlayerDetails(station, status, artist, song, liked, disliked);
                
                FragmentStations fragment = (FragmentStations) getSupportFragmentManager().findFragmentByTag(FragmentStations.FRAGMENT_TAG);
                fragment.refresh();
            }
        }
    }

}
