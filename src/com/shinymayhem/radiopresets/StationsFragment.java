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
import android.app.DialogFragment;
import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.CursorLoader;
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
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.shinymayhem.radiopresets.RadioDbContract.StationsDbHelper;

public class StationsFragment extends ListFragment implements LoaderCallbacks<Cursor>, 
		OnItemLongClickListener, MultiChoiceModeListener {

	protected StationsDbHelper mDbHelper;
	protected Context mContext;
	protected ListView mListView;
	protected Logger mLogger = new Logger();
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
		switch(item.getItemId())
		{
			case R.id.delete_station:
				//todo delete
				return true;
			
			case R.id.edit_station:
				//todo edit
				return true;
			
		}
		return false;
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		MenuInflater inflater = mode.getMenuInflater();
		int count = mListView.getCheckedItemCount();
		if (count == 1)
		{
			inflater.inflate(R.menu.station_selected, menu);
		}
		else
		{
			inflater.inflate(R.menu.stations_selected, menu);
		}
		return true;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
		// TODO Auto-generated method stub
		mode = mode;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		// TODO Auto-generated method stub
		int count = mListView.getCheckedItemCount();
		//menu.clear();
		
		return false;
	}

	@Override
	public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
			boolean checked) {
		// TODO Auto-generated method stub
		
		int count = mListView.getCheckedItemCount();
		if (count == 1)
		{
			
		}
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
