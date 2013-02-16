package com.shinymayhem.radiopresetswidget;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class RadioCursorAdapter extends CursorAdapter {

	protected Activity mContext;

	public RadioCursorAdapter(Activity context, Cursor c, int flags) {
		super(context, c, flags);
		this.mContext = context;
		
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		TextView titleView = (TextView)view.findViewById(R.id.station_title);
		final String url = cursor.getString(cursor.getColumnIndexOrThrow(RadioDbContract.StationEntry.COLUMN_NAME_URL));
		final Context c = context;
		titleView.setText(cursor.getString(cursor.getColumnIndexOrThrow(RadioDbContract.StationEntry.COLUMN_NAME_TITLE)));
		//TODO set listeners, do something with url, etc here
		titleView.setOnLongClickListener(new View.OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				// TODO Auto-generated method stub
				return true;
			}
		});
		
		titleView.setOnDragListener(new View.OnDragListener() {
			
			@Override
			public boolean onDrag(View v, DragEvent event) {
				// TODO Auto-generated method stub
				return true;
			}
		});
		
		titleView.setOnGenericMotionListener(new View.OnGenericMotionListener() {
			
			@Override
			public boolean onGenericMotion(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				return true;
			}
		});
		
		titleView.setOnTouchListener(new View.OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				return false;
			}
		});
		
		titleView.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View view) {
				MainActivity activity = (MainActivity) view.getContext();
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
					
					activity.play(url);
				}
				
			}
		});
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
	}

}
