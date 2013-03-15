package com.shinymayhem.radiopresets;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

//this class is mostly just a wrapper, receiving remote control broadcasts and passing them on to ServiceRadioPlayer.ReceiverMediaButton, if registered
public class ReceiverRemoteControl extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i("ReceiverRemoteControl", "onReceive()");
		
		KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        if (event.getAction() == KeyEvent.ACTION_UP)
        {
    		Intent newIntent = (Intent) intent.clone();
    		//ComponentName component = new ComponentName(intent.getComponent().getPackageName(), ServiceRadioPlayer.ReceiverMediaButton.class.getName());
    		newIntent.setComponent(null);
            newIntent.setAction(ServiceRadioPlayer.ACTION_MEDIA_BUTTON);
            Log.i("ReceiverRemoteControl", "cloned");
            // Rebroadcasts to radio player inner class receiver. 
            // This receiver is not exported; it'll only be received if the receiver is currently registered.
            context.sendBroadcast(newIntent);	
        }
        
		
		 

	}

}
