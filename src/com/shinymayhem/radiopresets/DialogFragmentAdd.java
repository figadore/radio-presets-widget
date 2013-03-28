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
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;

public class DialogFragmentAdd extends DialogFragment {

    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }
    
    public interface ListenerAddDialog
    {
        public void onDialogPositiveClick(View view);
        public void onDialogNegativeClick();
    }
    
    ListenerAddDialog mListener;
    
    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        try
        {
            // Instantiate the ListenerAddDialog so we can send events to the host
            mListener = (ListenerAddDialog) activity;
        }
        catch (ClassCastException e)
        {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement ListenerAddDialog");
        }
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Build the dialog and set up the button click handlers
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        
     // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_station_details, null);
        //EditText titleView = (EditText) view.findViewById(R.id.station_title);
        builder.setView(view)
               .setPositiveButton(R.string.add_station, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       // Send the positive button event back to the host activity
                       mListener.onDialogPositiveClick(view);
                   }
               })
               .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       // Send the negative button event back to the host activity
                       mListener.onDialogNegativeClick();
                   }
               })
               .setTitle(R.string.station_details_title);
        return builder.create();
    }

}
