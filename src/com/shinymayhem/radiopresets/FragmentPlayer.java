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

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class FragmentPlayer extends Fragment {
	
	protected ActivityLogger mLogger = new ActivityLogger();
	public static final String FRAGMENT_TAG = "com.shinymayhem.radiopresets.PlayerFragmentTag";
	
	//define functions that this class depends on for communication with the rest of the app
	public interface PlayerListener
	{
		//public void stop(View view);
		//public void next(View view);
		//public void prev(View view);
		public void setVolume(int volume);
	}
	
	protected PlayerListener mListener;
	
	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		try
		{
			// Instantiate the PlayerListener so we can send events to the host
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
		//inflate view from layout
		View view = inflater.inflate(R.layout.player_fragment, container, false);
		
		AudioManager audio = (AudioManager) this.getActivity().getSystemService(Context.AUDIO_SERVICE);
		SeekBar volumeSlider = (SeekBar) view.findViewById(R.id.volume_slider);
		
		
		//slider max = volume stream max
		volumeSlider.setMax(audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
		
		//detect slider updates
		volumeSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

			//update volume as slider is moved
			@Override
			public void onProgressChanged(SeekBar slider, int progress, boolean fromUser) {
				log("onProgressChanged(): " + String.valueOf(progress), "v");
				if (fromUser) //responding to touch slide event
				{
					mListener.setVolume(progress);
				}
				else //progress probably changed as a result of volume changing
				{
					
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar slider) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar slider) {
			}
		});
		
		return view;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);
	}
	
	//update volume slider to current volume
	public void updateSlider()
	{
		SeekBar volumeSlider = (SeekBar) this.getView().findViewById(R.id.volume_slider);
		AudioManager audio = (AudioManager) this.getActivity().getSystemService(Context.AUDIO_SERVICE);
		volumeSlider.setProgress(audio.getStreamVolume(AudioManager.STREAM_MUSIC));
	}
	
	@Override 
	public void onResume()
	{
		super.onResume();
		//update slider on startup, or in case volume was changed while fragment paused
		updateSlider();
		
	}
	
	public void log(String text, String level)
	{
		mLogger.log(this.getActivity(), "FragmentPlayer:\t\t"+text, level);
	}

}
