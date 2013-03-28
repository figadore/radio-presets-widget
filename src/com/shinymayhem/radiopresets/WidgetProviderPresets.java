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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

public class WidgetProviderPresets extends AppWidgetProvider {
    
    protected ActivityLogger mLogger = new ActivityLogger();
    protected Context mContext;
    public static final int MIN_BUTTON_WIDTH = 65; //TODO get from preferences

    public void onEnabled(Context context)
    {
        
        mContext = context;
        log("onEnabled()", "v");
        super.onEnabled(context);
        pullInfo(context);
    }
    
    public void onReceive(Context context, Intent intent)
    {
        
        mContext = context;
        log("widget onReceive(" + String.valueOf(intent.getAction()) + ")", "v");
        super.onReceive(context, intent);   
        
    }
    
    
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions)
    {
        mContext = context;
        log("onAppWidgetOptionsChanged()", "v");
        this.updateWidget(context, newOptions);
        this.pullInfo(context);
        //super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }
    

    @SuppressLint("NewApi")
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        updateWidget(context, null);
        pullInfo(context);
    }
    
    /**
     * update the widget views
     * @param context
     * @param newOptions
     */
    private void updateWidget(Context context, Bundle newOptions)
    {
        Intent intent = new Intent(context, ServiceWidgetUpdate.class);
        intent.setAction(ServiceWidgetUpdate.ACTION_UPDATE_WIDGET);
        if (newOptions != null)
        {
            intent.putExtras(newOptions);   
        }
        context.startService(intent);
    }
    
    /**
     * Get the widget to be updated by the service with current info
     * @param context
     */
    private void pullInfo(Context context)
    {
        log("updateDetails()", "v");
        Intent intent = new Intent(context, ServiceRadioPlayer.class);
        intent.setAction(ServiceRadioPlayer.ACTION_PULL_WIDGET_INFO);
        context.startService(intent);
    }
    
    
    private void log(String text, String level)
    {
        mLogger.log(mContext, text, level);
    }
    
}
