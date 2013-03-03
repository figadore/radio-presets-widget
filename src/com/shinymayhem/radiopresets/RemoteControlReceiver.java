package com.shinymayhem.radiopresets;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

//this class is mostly just a wrapper, receiving remote control broadcasts and passing them on to RadioPlayer.MediaButtonReceiver, if registered
public class RemoteControlReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i("RemoteControlReceiver", "onReceive()");
		
		KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        if (event.getAction() == KeyEvent.ACTION_UP)
        {
    		Intent newIntent = (Intent) intent.clone();
    		//ComponentName component = new ComponentName(intent.getComponent().getPackageName(), RadioPlayer.MediaButtonReceiver.class.getName());
    		newIntent.setComponent(null);
            newIntent.setAction(RadioPlayer.ACTION_MEDIA_BUTTON);
            Log.i("RemoteControlReceiver", "cloned");
            // Rebroadcasts to radio player inner class receiver. 
            // This receiver is not exported; it'll only be received if the receiver is currently registered.
            context.sendBroadcast(newIntent);	
        }
        
		
		 

	}

}
