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
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;



public class CursorAdapterStations extends CursorAdapter {

	protected Activity mContext;

	public CursorAdapterStations(Activity context, Cursor c, int flags) {
		super(context, c, flags);
		this.mContext = context;
		
	}

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		TextView titleView = (TextView)view.findViewById(R.id.station_title);
		int preset = (int)cursor.getLong(cursor.getColumnIndexOrThrow(DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER));
		String station = cursor.getString(cursor.getColumnIndexOrThrow(DbContractRadio.EntryStation.COLUMN_NAME_TITLE));
		String text =  String.valueOf(preset) + ". " + station;
		titleView.setText(text);
		int drawable;
		if (((ActivityMain) mContext).getPlayingPreset() == preset)
		{
			drawable = R.drawable.list_item_background_playing;
		}
		else
		{
			drawable = R.drawable.list_item_background;
		}
		if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN) {
			view.setBackground(mContext.getResources().getDrawable(drawable));
		}
		else
		{
			view.setBackgroundDrawable(mContext.getResources().getDrawable(drawable));	
		}
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		final LayoutInflater inflater = LayoutInflater.from(mContext);
		View v = inflater.inflate(R.layout.station_entry, null);
		//View v = inflater.inflate(R.layout.station_entry, parent, true);
		return v;
	}
	
	@Override
	public void onContentChanged()
	{
		//called if flag is set
		Log.v("CursorAdapter", "onContentChanged()");
	}

}
