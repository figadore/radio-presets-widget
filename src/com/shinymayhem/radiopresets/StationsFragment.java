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

import java.util.NoSuchElementException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.shinymayhem.radiopresets.RadioDbContract.StationsDbHelper;

public class StationsFragment extends ListFragment implements LoaderCallbacks<Cursor>, 
		OnItemLongClickListener, MultiChoiceModeListener {

	protected StationsDbHelper mDbHelper;
	protected Context mContext;
	protected ListView mListView;
	protected Logger mLogger = new Logger();
	protected ActionMode mActionMode;
	protected int mSelectedCount;
	
	RadioCursorAdapter mAdapter;
	
	public interface PlayerListener
	{
		public void play(String url);
	}
	
	PlayerListener mListener;
	
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

	
	protected Context getContext()
	{
		return mContext;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		getLoaderManager().initLoader(MainActivity.LOADER_STATIONS, null, this);
		
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
			log("add station button clicked", "v");
			DialogFragment dialog = new AddDialogFragment();
			dialog.show(this.getFragmentManager(), "AddDialogFragment");
			return true;	
		}
		return false;
		
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		log("onCreateView()", "v");

		//FLAG_REGISTER_CONTENT_OBSERVER makes RadioCursorAdapter.onContentChanged method get called
		mAdapter = new RadioCursorAdapter(this.getActivity(), null, RadioCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
		//mAdapter = new RadioCursorAdapter(this.getActivity(), null, 0);
		//this.setEmptyText("No stations added"); //this is done by layout, right?
		this.setListAdapter(mAdapter);
		mContext = container.getContext();
		
		
		
		View view = super.onCreateView(inflater, container, savedInstanceState);
		
		
		
		return view;
		
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		log("onViewCreated()", "v");
		super.onViewCreated(view, savedInstanceState);
		//this.registerForContextMenu(this.getListView());
		
		/*
		//implemented in this
		getListView().setOnItemClickListener(new OnItemClickListener()
		{

			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position,
					long id) {
				
				Cursor cursor = (Cursor)view.getTag();
				final String url = cursor.getString(cursor.getColumnIndexOrThrow(RadioDbContract.StationEntry.COLUMN_NAME_URL));
				String str= "list item clicked, play "+ url;
				log(str, "v");
			}
			
		});
		*/
		mListView = (ListView)getListView(); 
		//FIXME for some reason the xml attribute isn't working
		mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		//doesn't do anything if multi-choice-modal set
		//listView.setOnItemLongClickListener((OnItemLongClickListener)this);
		mListView.setMultiChoiceModeListener((MultiChoiceModeListener)this);
		
	}
	
	@Override 
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
	}
	
	//doesn't do anything if multi-choice-modal set
	public boolean onItemLongClick(AdapterView<?> adapterView, View view,
			int position, long id) {
		String str= "list item longclicked";
		log(str, "v");
		
		boolean checked = true;
		ListView listView = (ListView) adapterView;
		listView.setItemChecked(position, checked);
		//SparseBooleanArray checkedPositions = listView.getCheckedItemPositions();
		//int checkedCount = listView.getCheckedItemCount();
		//long[] var = listView.getCheckedItemIds();
		
		//view.setSelected(true);
		listView.startActionMode(this);
		
		
		
		return true;
	}
	
	@Override
	public void onListItemClick(ListView listView, View view, int position, long id)
	{
		//TODO handle playing here, if possible
		super.onListItemClick(listView, view, position, id);
		
		
		Cursor cursor = (Cursor)listView.getItemAtPosition(position);
		final String url = cursor.getString(cursor.getColumnIndexOrThrow(RadioDbContract.StationEntry.COLUMN_NAME_URL));
		
		String str= "list item clicked, play " + url + ". view position:";
		str += Integer.toString(position);
		str += ", row id:";
		str += Long.toString(id);
		log(str, "v");
		mListener.play(url);
	}
	
	
	
	private void log(String text, String level)
	{
		mLogger.log(this.getActivity(), "StationsFragment:\t\t"+text, level);
	}
	

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		log("onCreateLoader()", "v");
		String[] projection = {
				RadioDbContract.StationEntry._ID,
				RadioDbContract.StationEntry.COLUMN_NAME_PRESET_NUMBER,
				RadioDbContract.StationEntry.COLUMN_NAME_TITLE,
				RadioDbContract.StationEntry.COLUMN_NAME_URL
		};
		
		String sortOrder = RadioDbContract.StationEntry.COLUMN_NAME_PRESET_NUMBER + " ASC";
		Uri uri = RadioContentProvider.CONTENT_URI_STATIONS;
		return new CursorLoader(this.getActivity().getApplicationContext(), uri, projection, null, null, sortOrder);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		log("onLoadFinished()", "v");
		switch(loader.getId())
		{
			case MainActivity.LOADER_STATIONS:
				mAdapter.swapCursor((Cursor)cursor);
				break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		log("onLoaderReset()", "v");
		switch(loader.getId())
		{
			case MainActivity.LOADER_STATIONS:
				mAdapter.swapCursor(null);
				break;
		}
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		boolean handled = false;
		switch(item.getItemId())
		{
			case R.id.delete_station:
				deleteSelected();
				handled = true;
				break;
			case R.id.edit_station:
				editSelected();
				handled = true;
				break;
			default:
				throw new IllegalArgumentException("Unknown action menu item");
		}
		if (handled)
		{
			mode.finish();
			//mActionMode = null;
		}
		return handled;
	}
	
	private void deleteSelected()
	{
		log("deleting selected", "i");
		long[] selected = mListView.getCheckedItemIds();
		String[] values = new String[selected.length];
		String where = RadioDbContract.StationEntry._ID + " in (";
		//String[] values = new String[1];
		//values[0] = String.valueOf(selected[0]);
		for (int i=0; i<selected.length; i++)
		{
			where += "?, ";
			values[i] = String.valueOf(selected[i]);
		}
		where += "'') ";
		//where= "_id in (?, ?, '')"
		int deletedCount = getActivity().getContentResolver().delete(RadioContentProvider.CONTENT_URI_STATIONS, where, values);//(RadioContentProvider.CONTENT_URI_STATIONS, selected);
		log("deleted " + String.valueOf(deletedCount), "v");
	}
	
	private void editSelected()
	{
		log("editing selected", "i");
		int position = getSelectedPosition();
		if (position == -1) //should never happen
		{
			throw new NoSuchElementException("Selected element not found.");
		}
		Cursor cursor = (Cursor)mListView.getItemAtPosition(position);
		String title = cursor.getString(cursor.getColumnIndexOrThrow(RadioDbContract.StationEntry.COLUMN_NAME_TITLE));
		String url = cursor.getString(cursor.getColumnIndexOrThrow(RadioDbContract.StationEntry.COLUMN_NAME_URL));
		url += "";
		LayoutInflater inflater = LayoutInflater.from(mContext);
		final View editView = inflater.inflate(R.layout.dialog_station_details, null);
		final long id = cursor.getLong(cursor.getColumnIndexOrThrow(RadioDbContract.StationEntry._ID));
		((EditText)editView.findViewById(R.id.station_title)).setText(title);
		((EditText)editView.findViewById(R.id.station_url)).setText(url);
		AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
		builder.setView(editView);
		builder.setPositiveButton(R.string.edit_station, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				editStation(id, editView);
			}
		});
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				log("edit station cancelled", "i");
				
			}
		});
		builder.show();
	}
	
	private void editStation(long id, View view)
	{
		log("id:" + String.valueOf(id), "i"); 
		// User touched the dialog's positive button
		log("edit station confirmed", "i");
		EditText titleView = (EditText)view.findViewById(R.id.station_title);
		EditText urlView = (EditText)view.findViewById(R.id.station_url);
		
		//int preset = 1;
		String title = titleView.getText().toString();
		String url = urlView.getText().toString();
		ContentValues values = new ContentValues();
		//values.put(RadioDbContract.StationEntry.COLUMN_NAME_PRESET_NUMBER, preset);
        values.put(RadioDbContract.StationEntry.COLUMN_NAME_TITLE, title);
        values.put(RadioDbContract.StationEntry.COLUMN_NAME_URL, url);
		//CursorLoader var = getLoaderManager().getLoader(MainActivity.LOADER_STATIONS);
        //TODO see if there is a better way to do this, like addId or something
        Uri uri = Uri.parse(RadioContentProvider.CONTENT_URI_STATIONS.toString() + "/" + String.valueOf(id));
		int updatedCount = this.getActivity().getContentResolver().update(uri, values, null, null);
		log("updated " + updatedCount + " rows.", "v");
	}
	
	//returns last found matching checked item position, -1 on no match
	private int getSelectedPosition()
	{
		SparseBooleanArray positions = mListView.getCheckedItemPositions();
		int position = -1;
		for (int i=0; i<mListView.getCount(); i++)
		{
			boolean selected = positions.get(i); 
			if (selected)
			{
				position = i;
			}
		}
		return position;
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		mSelectedCount=mListView.getCheckedItemCount();;
		mActionMode = mode;
		MenuInflater inflater = mode.getMenuInflater();
		
		inflater.inflate(R.menu.station_selected, menu);
		
		return true;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
		mActionMode = null;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		// TODO Auto-generated method stub
		int newCount = mListView.getCheckedItemCount();
		if (mSelectedCount == 1 || newCount == 1)
		{
			menu.clear();
			MenuInflater inflater = mode.getMenuInflater();
			if (newCount == 1)
			{
				inflater.inflate(R.menu.station_selected, menu);
			}
			else
			{
				inflater.inflate(R.menu.stations_selected, menu);
			}
			mSelectedCount = newCount;
			return true;
		}
		
		//menu.clear();
		
		return false; //no change, don't update
	}

	@Override
	public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
			boolean checked) {
		// TODO Auto-generated method stub
		
		int count = mListView.getCheckedItemCount();
		mode.setTitle(String.valueOf(count) + " selected");
		//mode.setSubtitle("Subtitle");

		String str = "Position " + String.valueOf(position) + " ";
		RelativeLayout item = (RelativeLayout) mListView.getChildAt(position);
		//item.setActivated(checked);
		if (checked)
		{
			//Object item = mListView.getItemAtPosition(position);
			
			//item.setBackgroundColor("android:attr/activatedBackgroundColor");
			str += "checked";
		}
		else
		{
			str += "unchecked";
		}
		log(str, "i");
		mode.invalidate();
	}
}
