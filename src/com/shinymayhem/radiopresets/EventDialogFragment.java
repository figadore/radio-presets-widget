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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;

public class EventDialogFragment extends DialogFragment {

	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}
	
	public interface EventDialogListener
	{
		public void onDialogEventPositiveClick(DialogFragment dialog);
		public void onDialogEventNegativeClick(DialogFragment dialog);
	}
	
	EventDialogListener mListener;
	
	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		try
		{
			// Instantiate the AddDialogListener so we can send events to the host
			mListener = (EventDialogListener) activity;
		}
		catch (ClassCastException e)
		{
			// The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement AddDialogListener");
		}
	}
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Build the dialog and set up the button click handlers
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        
     // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        
        builder.setView(inflater.inflate(R.layout.dialog_event_details, null))
               .setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       // Send the positive button event back to the host activity
                       mListener.onDialogEventPositiveClick(EventDialogFragment.this);
                   }
               })
               .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       // Send the negative button event back to the host activity
                       mListener.onDialogEventNegativeClick(EventDialogFragment.this);
                   }
               })
               .setTitle(R.string.event_details);
        return builder.create();
    }

}