package com.shinymayhem.radiopresets;

import com.shinymayhem.radiopresets.R;

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
import android.widget.ListView;
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
		
		//ListViewItem item = (ListViewItem)titleView.getParent().getParent();
		final String url = cursor.getString(cursor.getColumnIndexOrThrow(RadioDbContract.StationEntry.COLUMN_NAME_URL));
		final Context c = context;
		titleView.setText(cursor.getString(cursor.getColumnIndexOrThrow(RadioDbContract.StationEntry.COLUMN_NAME_TITLE)));
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
					Log.i(getClass().toString(), "play");
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
				return false;
			}
			
		}
		
		TitleListener titleListener = new TitleListener();
		titleView.setOnLongClickListener(titleListener);
		titleView.setOnDragListener(titleListener);
		titleView.setOnGenericMotionListener(titleListener);
		titleView.setOnTouchListener(titleListener);
		titleView.setOnClickListener(titleListener);
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
