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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

/**
 * this class is mostly just a wrapper, receiving remote control broadcasts and passing them on to ServiceRadioPlayer.ReceiverMediaButton, if registered
 * @author Reese Wilson
 *
 */
public class ReceiverRemoteControl extends BroadcastReceiver {
    private static final boolean LOCAL_LOGV = ActivityMain.LOCAL_LOGV;
    private static final String TAG = "ReceiverRemoteControl";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (LOCAL_LOGV) Log.v(TAG, "onReceive()");
        
        KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        if (event.getAction() == KeyEvent.ACTION_UP)
        {
            Intent newIntent = (Intent) intent.clone();
            //ComponentName component = new ComponentName(intent.getComponent().getPackageName(), ServiceRadioPlayer.ReceiverMediaButton.class.getName());
            newIntent.setComponent(null);
            newIntent.setAction(ServiceRadioPlayer.ACTION_MEDIA_BUTTON);
            if (LOCAL_LOGV) Log.v(TAG, "cloned");
            // Rebroadcasts to radio player inner class receiver. 
            // This receiver is not exported; it'll only be received if the receiver is currently registered.
            context.sendBroadcast(newIntent);   
        }
        
        
         

    }

}
