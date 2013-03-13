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
import android.database.Cursor;
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

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		//make cursor available to list item, for onlistitemclick or onitemlongclick
		//view.setTag(cursor);
		TextView titleView = (TextView)view.findViewById(R.id.station_title);
		String text = cursor.getString(cursor.getColumnIndexOrThrow(DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER)) + ". " +
				cursor.getString(cursor.getColumnIndexOrThrow(DbContractRadio.EntryStation.COLUMN_NAME_TITLE));
		titleView.setText(text);
		//ImageView imageView = (ImageView)view.findViewById(R.id.station_drag);
		/*
		imageView.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View arg0) {
				Log.i(getClass().toString(), "imageView.onClick()");
				
			}
			
		});*/
		
		/*
		//ListViewItem item = (ListViewItem)titleView.getParent().getParent();
		final String url = cursor.getString(cursor.getColumnIndexOrThrow(DbContractRadio.EntryStation.COLUMN_NAME_URL));
		final Context c = context;
		
		
		
		class TitleListener implements View.OnLongClickListener, View.OnDragListener, View.OnGenericMotionListener, View.OnClickListener, View.OnTouchListener
		{

			@Override
			public boolean onTouch(View view, MotionEvent event) {
				// TODO Auto-generated method stub
				Log.i(getClass().toString(), "titleView.onTouch()");
				return false;
			}

			@Override
			public void onClick(View view) {
				Log.i(getClass().toString(), "titleView.onClick()");
				//TODO see if this still works, or if context is something else now, in fragment
				ActivityMain activity = (ActivityMain) view.getContext();
				ConnectivityManager network = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo info = network.getActiveNetworkInfo();
				if (info == null || info.isConnected() == false)
				{
					//TextView status = (TextView) findViewById(R.id.status);
					//status.setText("No network");
					Toast.makeText(c, "No network", Toast.LENGTH_SHORT).show();
					Log.i(getClass().toString(), "no network, can't do anything");
					return;
				}
				else
				{
					Log.i(getClass().toString(), "play");
					//TODO have the fragment handle this? or send straight to service? 
					activity.play(url);
				}
				ListView parent = (ListView)view.getParent().getParent();
				boolean called = parent.callOnClick();
				if (called)
				{
					Log.i(getClass().toString(), "listview onclick attempted");
				}
				
			}

			@Override
			public boolean onGenericMotion(View view, MotionEvent event) {
				Log.i(getClass().toString(), "titleView.onGenericMotion()");
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean onDrag(View view, DragEvent event) {
				// TODO Auto-generated method stub
				Log.i(getClass().toString(), "titleView.onDrag()");
				return false;
			}

			@Override
			public boolean onLongClick(View view) {
				Log.i(getClass().toString(), "titleView.onLongClick()");
				// TODO Auto-generated method stub
				return true;
			}
			
		}
		
		TitleListener titleListener = new TitleListener();
		titleView.setOnLongClickListener(titleListener);
		titleView.setOnClickListener(titleListener);
		*/
		//titleView.setOnDragListener(titleListener);
		//titleView.setOnGenericMotionListener(titleListener);
		//titleView.setOnTouchListener(titleListener);
		
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
