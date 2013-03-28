package com.shinymayhem.radiopresets;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

public class ServiceWidgetUpdate extends IntentService {

    public static final String ACTION_UPDATE_WIDGET = "com.shinymayhem.radiopresets.servicewidgetupdate.ACTION_UPDATE_WIDGET";
    protected Intent mMainIntent;
    private final int[] layoutIds = {R.layout.preset1, R.layout.preset2, R.layout.preset3, R.layout.preset4, R.layout.preset5, R.layout.preset6, R.layout.preset7, R.layout.preset8};
    private final int[] selectedLayoutIds = {R.layout.preset1_selected, R.layout.preset2_selected, R.layout.preset3_selected, R.layout.preset4_selected, R.layout.preset5_selected, R.layout.preset6_selected, R.layout.preset7_selected, R.layout.preset8_selected};
    private final int[] buttonIds = {R.id.widget_preset_1, R.id.widget_preset_2, R.id.widget_preset_3, R.id.widget_preset_4, R.id.widget_preset_5, R.id.widget_preset_6, R.id.widget_preset_7, R.id.widget_preset_8};
    
    public ServiceWidgetUpdate()
    {
        super("ServiceWidgetUpdateName");
    }
    
    @Override
    protected void onHandleIntent(Intent intent) {
        // TODO Auto-generated method stub
        String action = intent.getAction();
        if (action.equals(ActivityMain.ACTION_UPDATE_INFO))
        {
            Bundle extras = intent.getExtras();
            this.updateInfo(extras);
        }
        else if (action.equals(ACTION_UPDATE_WIDGET))
        {
            updateWidget();
        }
        //AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        stopSelf();
    }
    
    @SuppressLint("NewApi")
    private void updateWidget()
    {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName provider = new ComponentName(this, com.shinymayhem.radiopresets.WidgetProviderPresets.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(provider);
        final int N = appWidgetIds.length;
        RemoteViews views;
        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            log("updateWidget looping through widgets. on " + String.valueOf(i) + " of " + String.valueOf(N), "v");
            int appWidgetId = appWidgetIds[i];
            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN)
            {
                Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
                views = this.getViews(options, null); 
            }
            else
            {
                views = this.getViews(null, null);
            }
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
    
    @SuppressLint("NewApi")
    private void updateInfo(Bundle extras)
    {
        String status = extras.getString(ActivityMain.EXTRA_STATUS);
        String station = extras.getString(ActivityMain.EXTRA_STATION);
        String artist = extras.getString(ActivityMain.EXTRA_ARTIST);
        String song = extras.getString(ActivityMain.EXTRA_SONG);
        log("updating widget info:" + station + "," + status, "v");
        //boolean stopped = status.equals(getResources().getString(R.string.status_stopped)); 
        
        //mContext = context;
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName provider = new ComponentName(this, com.shinymayhem.radiopresets.WidgetProviderPresets.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(provider);
        
        final int N = appWidgetIds.length;
        
        boolean playing = status.equals(getResources().getString(R.string.status_playing));
        // Perform this loop procedure for each App Widget that belongs to this provider
        RemoteViews views;
        for (int i=0; i<N; i++) {
            log("updateInfo looping through widgets. on " + String.valueOf(i) + " of " + String.valueOf(N), "v");
            int appWidgetId = appWidgetIds[i];
            
            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN)
            {
                Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
                views = this.getViews(options, extras); 
            }
            else
            {
                views = this.getViews(null, extras);
            }
            
            views.setTextViewText(R.id.currently_playing, station);
            if (playing)
            {
                views.setTextViewText(R.id.widget_status, artist + " - " + song);   
            }
            else
            {
                views.setTextViewText(R.id.widget_status, status);
            }
            //appWidgetManager.updateAppWidget(provider, mViews);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
    
    /**
     * Gets the remote views to update the widget with
     * @param options Widget options, includes dimensions like appWidgetMinWidth to determine widget layout
     * @param extras Metadata and other info needed for populating dynamic parts of the widget
     * @return
     */
    private RemoteViews getViews(Bundle options, Bundle extras)
    {
        int tallWidgetHeight = getResources().getDimensionPixelSize(R.dimen.tall_widget_min_height);

        RemoteViews views;
        int height = getHeight(options); 
        if (height >= tallWidgetHeight)
        {
            log("tall widget", "v");
            views = this.getTallWidgetViews(options, extras);
        }
        else
        {
            log("short widget", "v");
            views = this.getShortWidgetViews(options, extras);
        }
        //return mViews;
        //mViews = views;
        return views;
    }

    /**
     * Get the min height of the widget
     * @param options AppWidgetOptions on JellyBean and higher, null on lower API levels
     * @return current min widget height, or xml dimension if null options
     */
    @SuppressLint("InlinedApi")
    private int getHeight(Bundle options)
    {
        //in landscape
        int minHeight = (int) getResources().getDimensionPixelSize(R.dimen.tall_widget_min_height);
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN && options.containsKey(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT))
        {
            minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);   
            log("got height:" + String.valueOf(minHeight), "v");
        }
        return minHeight;
    }

    /**
     * Get the min width of the widget
     * @param options AppWidgetOptions on JellyBean and higher, null on lower API levels
     * @return current min widget width, or xml dimension if null options
     */
    @SuppressLint("InlinedApi")
    private int getWidth(Bundle options)
    {
        
        int minWidth = (int) getResources().getDimensionPixelSize(R.dimen.widget_min_width);
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN && options.containsKey(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH))
        {
            //in portrait
            minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        }
        return minWidth;
    }

    private RemoteViews getShortWidgetViews(Bundle options, Bundle extras)
    {
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.preset_buttons_widget);
         
        PendingIntent launchIntent = this.getLaunchIntent();
        views.setOnClickPendingIntent(R.id.launch_main, launchIntent);
        
        PendingIntent stopIntent = this.getStopIntent();
        views.setOnClickPendingIntent(R.id.widget_stop, stopIntent);
        
        this.setPresets(options, extras, views);
        return views;
    }
    
    private RemoteViews getTallWidgetViews(Bundle options, Bundle extras)
    {
        boolean liked = false;
        boolean disliked = false;
        if (extras != null)
        {
            liked = extras.getBoolean(ActivityMain.EXTRA_LIKED);
            disliked = extras.getBoolean(ActivityMain.EXTRA_DISLIKED);
        }
        
        RemoteViews views = new RemoteViews(this.getPackageName(), R.layout.preset_player_widget);
        
        PendingIntent launchIntent = this.getLaunchIntent();
        views.setOnClickPendingIntent(R.id.launch_main, launchIntent);
        
        PendingIntent previousIntent = this.getPreviousIntent();
        views.setOnClickPendingIntent(R.id.widget_previous, previousIntent);
        
        PendingIntent stopIntent = this.getStopIntent();
        views.setOnClickPendingIntent(R.id.widget_stop, stopIntent);
        
        if (liked)
        {
            //log("liked", "v");
            views.setImageViewResource(R.id.widget_like_button, R.drawable.song_like_selected);
        }
        else
        {
            //log("not liked", "v");
            views.setImageViewResource(R.id.widget_like_button, R.drawable.song_like);
        }
        PendingIntent nextIntent = this.getNextIntent();
        views.setOnClickPendingIntent(R.id.widget_next, nextIntent);
        
        if (disliked)
        {
            views.setImageViewResource(R.id.widget_dislike_button, R.drawable.song_dislike_selected);
        }
        else
        {
            views.setImageViewResource(R.id.widget_dislike_button, R.drawable.song_dislike);
        }
        PendingIntent likeIntent = this.getLikeIntent(!liked);
        views.setOnClickPendingIntent(R.id.widget_like_button, likeIntent);
        
        PendingIntent dislikeIntent = this.getDislikeIntent(!disliked);
        views.setOnClickPendingIntent(R.id.widget_dislike_button, dislikeIntent);
        
        setPresets(options, extras, views);
        return views;
    }
    
    private void setPresets(Bundle options, Bundle extras, RemoteViews views)
    {
        log("setPresets()", "v");
        views.removeAllViews(R.id.preset_buttons);
        
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
        Cursor cursor = this.getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
        
        int stationsCount = cursor.getCount();
        
        int widgetWidth = getWidth(options);
        if (stationsCount*WidgetProviderPresets.MIN_BUTTON_WIDTH > widgetWidth)
        {
            maxButtons = widgetWidth/WidgetProviderPresets.MIN_BUTTON_WIDTH;
        }
        else
        {
            maxButtons = stationsCount;
        }
        int playingPreset = 0;
        if (extras != null)
        {
            playingPreset = extras.getInt(ActivityMain.EXTRA_PRESET);
        }

        for (int preset=1; preset <= maxButtons; preset++) {
            if (playingPreset == preset)
            {
                layoutId = selectedLayoutIds[preset-1];
            }
            else
            {
                layoutId = layoutIds[preset-1];
            }
            
            buttonId = buttonIds[preset-1];
            presetButton = new RemoteViews(this.getPackageName(), layoutId);
            //int viewId = presetButton.getLayoutId();
            
            presetButton.setTextViewText(buttonId, String.valueOf(preset));
            presetIntent = this.getPresetIntent(preset);
            views.addView(R.id.preset_buttons, presetButton);
            views.setOnClickPendingIntent(buttonId, presetIntent);
            
            
        }
        cursor.close();
    }
    
    private Intent getMainIntent()
    {
        if (mMainIntent == null)
        {
            mMainIntent = new Intent(this, ActivityMain.class);
        }
        return mMainIntent;
        
    }
    
    private PendingIntent getLaunchIntent()
    {
        Intent intent = this.getMainIntent()
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .setAction(Intent.ACTION_RUN)
                .setClass(this, ActivityMain.class);
        //intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0); //Intent.FLAG_ACTIVITY_NEW_TASK
        return pendingIntent;
    }
    
    private PendingIntent getPreviousIntent()
    {
        Intent intent = getMainIntent()
                .setFlags(0)
                .setAction(ServiceRadioPlayer.ACTION_PREVIOUS)
                .setClass(this, ServiceRadioPlayer.class);
        return PendingIntent.getService(this, 0, intent, 0);
             
    }
    
    private PendingIntent getStopIntent()
    {
        Intent intent = getMainIntent()
                .setFlags(0)
                .setAction(ServiceRadioPlayer.ACTION_STOP)
                .setClass(this, ServiceRadioPlayer.class);
        return PendingIntent.getService(this, 0, intent, 0);
             
    }
    
    private PendingIntent getNextIntent()
    {
        Intent intent = getMainIntent()
                .setFlags(0)
                .setAction(ServiceRadioPlayer.ACTION_NEXT)
                .setClass(this, ServiceRadioPlayer.class);
        return PendingIntent.getService(this, 0, intent, 0);         
    }
    
    private PendingIntent getLikeIntent(boolean like)
    {
        Intent intent = getMainIntent()
                .setFlags(0)
                .setAction(ServiceRadioPlayer.ACTION_LIKE)
                .putExtra(ServiceRadioPlayer.EXTRA_SET_TRUE, like)
                .setClass(this, ServiceRadioPlayer.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
    
    private PendingIntent getDislikeIntent(boolean dislike)
    {
        Intent intent = getMainIntent()
                .setFlags(0)
                .setAction(ServiceRadioPlayer.ACTION_DISLIKE)
                .putExtra(ServiceRadioPlayer.EXTRA_SET_TRUE, dislike)
                .setClass(this, ServiceRadioPlayer.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);         
    }
    
    private PendingIntent getPresetIntent(int preset)
    {
        Intent playIntent = getMainIntent()
                .setAction(ServiceRadioPlayer.ACTION_PLAY)
                .setFlags(0)
                .setClass(this, ServiceRadioPlayer.class);
        playIntent.putExtra(ActivityMain.EXTRA_STATION_PRESET, preset);
        PendingIntent presetIntent = PendingIntent.getService(this, preset, playIntent, PendingIntent.FLAG_UPDATE_CURRENT); //cancel current sometimes fails 
        return presetIntent;
    }
    
    
    
    private void log(String text, String level)
    {
        Log.v("ServiceWidgetUpdate", text);
    }
}
