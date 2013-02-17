package com.shinymayhem.radiopresets;

import android.app.DialogFragment;
import android.app.ListFragment;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ListView;

import com.shinymayhem.radiopresets.RadioDbContract.StationsDbHelper;

public class StationsFragment extends ListFragment {

	protected StationsDbHelper mDbHelper;
	protected Context mContext;
	protected Logger mLogger = new Logger();
	
	protected Context getContext()
	{
		return mContext;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		super.onCreate(savedInstanceState);
		
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// Inflate the menu; this adds items to the action bar if it is present.
		inflater.inflate(R.menu.stations, menu);
		super.onCreateOptionsMenu(menu, inflater);
		
		
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == R.id.add_station)
		{
			DialogFragment dialog = new AddDialogFragment();
			dialog.show(this.getFragmentManager(), "AddDialogFragment");
			return true;	
		}
		return false;
		
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		//return inflater.inflate(R.layout.stations_fragment, container, false);
		mContext = container.getContext();
		//get view
		View inflated = inflater.inflate(R.layout.stations_fragment, null, false);
		ListView stationsLayout;
		if (inflated instanceof ListView)
		{
			stationsLayout = (ListView)inflated;
		}
		else
		{
			stationsLayout = (ListView)inflated.findViewById(android.R.id.list);
		}
		 //= (ListView)inflater.inflate(R.layout.stations_fragment, container, false);
				//this.findViewById(R.layout.stations_fragment); 

		//get stations from sqlite
		mDbHelper  = new StationsDbHelper(getContext());
		SQLiteDatabase db = mDbHelper.getReadableDatabase();
		String[] projection = {
				"_id",
				RadioDbContract.StationEntry.COLUMN_NAME_PRESET_NUMBER,
				RadioDbContract.StationEntry.COLUMN_NAME_TITLE,
				RadioDbContract.StationEntry.COLUMN_NAME_URL
		};
		
		String sortOrder = RadioDbContract.StationEntry.COLUMN_NAME_PRESET_NUMBER + " ASC";
		
		Cursor cursor = db.query(RadioDbContract.StationEntry.TABLE_NAME, projection, null, null, null, null, sortOrder, Integer.toString(MainActivity.BUTTON_LIMIT));
		RadioCursorAdapter adapter = new RadioCursorAdapter(this.getActivity(), cursor, RadioCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
		stationsLayout.setAdapter(adapter);
		stationsLayout.setOnItemSelectedListener(new OnItemSelectedListener()
		{

			@Override
			public void onItemSelected(AdapterView<?> adapter, View view, int position,
					long id) {
				// TODO Auto-generated method stub
				String str= "item selected, view position:";
				str += Integer.toString(position);
				str += ", row id:";
				str += Long.toString(id);
				log(str, "v");
				//Toast.makeText(getContext(), str, Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onNothingSelected(AdapterView<?> adapter) {
				// TODO Auto-generated method stub
				String str = "nothing selected";
				log(str, "v");
				//Toast.makeText(getContext(), "nothing selected", Toast.LENGTH_SHORT).show();
				
			}
			
		});
		
		stationsLayout.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> adapter, View view, int position,
					long id) {
				// TODO Auto-generated method stub
				String str= "item clicked, view position:";
				str += Integer.toString(position);
				str += ", row id:";
				str += Long.toString(id);
				log(str, "v");
				
			}
		});
		
		stationsLayout.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> adapter, View view, int position,
					long id) {
				// TODO Auto-generated method stub
				String str= "item long clicked, view position:";
				str += Integer.toString(position);
				str += ", row id:";
				str += Long.toString(id);
				log(str, "v");
				//Toast.makeText(getContext(), str, Toast.LENGTH_SHORT).show();
				return true;
			}
		});
		
		db.close();
		return inflated;
	}
	
	private void log(String text, String level)
	{
		mLogger.log(this.getActivity(), "StationsFragment:\t\t"+text, level);
	}
	/*
		String str = "StationsFragment:\t\t" + text;
		FileOutputStream file;
		if (level == "v")
		{
			str = "VERBOSE:\t\t" + str;
			Log.v("StationsFragment:", str);
		}
		else if (level == "d")
		{
			str = "DEBUG:\t\t" + str;
			Log.d("StationsFragment:", str);
		}
		else if (level == "i")
		{
			str = "INFO:\t\t" + str;
			Log.i("StationsFragment:", str);
		}
		else if (level == "w")
		{
			str = "WARN:\t\t" + str;
			Log.w("StationsFragment:", str);
		}
		else if (level == "e")
		{
			str = "ERROR:\t\t" + str;
			Log.e("StationsFragment", str);
		}
		else
		{
			Toast.makeText(getContext(), "new log level", Toast.LENGTH_SHORT).show();
			Log.e("StationsFragment", "new log level");
			str = level + str;
			Log.e("StationsFragment", str);
		}
		
		try {
			Calendar cal = Calendar.getInstance();
	    	cal.getTime();
	    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
	    	str += "\n";
	    	str = sdf.format(cal.getTime()) + "\t\t" + str;
			file = getContext().openFileOutput(RadioPlayer.LOG_FILENAME, Context.MODE_APPEND);
			file.write(str.getBytes());
			file.flush();
			file.close();
			//file = File.createTempFile(fileName, null, this.getCacheDir());
			//file.
		}
		catch (Exception e)
		{
	    	Toast.makeText(getContext(), "error writing to log file", Toast.LENGTH_SHORT).show();
	    	e.printStackTrace();
		}
	}
*/
}
