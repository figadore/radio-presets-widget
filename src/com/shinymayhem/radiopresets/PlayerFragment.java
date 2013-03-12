package com.shinymayhem.radiopresets;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class PlayerFragment extends Fragment {
	
	protected Logger mLogger = new Logger();
	
	public interface PlayerListener
	{
		public void stop(View view);
		public void next(View view);
		public void prev(View view);
		public void setVolume(int percent);
	}
	
	protected PlayerListener mListener;
	
	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		try
		{
			// Instantiate the AddDialogListener so we can send events to the host
			mListener = (PlayerListener) activity;
		}
		catch (ClassCastException e)
		{
			// The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement PlayerListener");
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		log("onCreateView()", "v");
		//super.onCreateView(inflater, container, savedInstanceState);
		//View view = super.onCreateView(inflater, container, savedInstanceState);
		
		View view = inflater.inflate(R.layout.player_fragment, container, false);
		return view;
		
	}
	
	public void log(String text, String level)
	{
		mLogger.log(this.getActivity(), "PlayerFragment:\t\t"+text, level);
	}

}
