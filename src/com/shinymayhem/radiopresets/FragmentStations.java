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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.SparseBooleanArray;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import com.shinymayhem.radiopresets.DbContractRadio.DbHelperRadio;

public class FragmentStations extends ListFragment implements LoaderCallbacks<Cursor> /*, 
        OnItemClickListener, OnItemLongClickListener, MultiChoiceModeListener */ {

    protected DbHelperRadio mDbHelper;
    protected Context mContext;
    protected ActivityLogger mLogger = new ActivityLogger();
    protected ListView mListView;
    public static final String FRAGMENT_TAG = "com.shinymayhem.radiopresets.StationsFragmentTag";
    
    CursorAdapterStations mAdapter;
    
    public interface PresetListener
    {
        public void play(int preset);
        public void updateDetails();
    }
    
    PresetListener mListener;
    
    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        try
        {
            // Instantiate the ListenerAddDialog so we can send events to the host
            mListener = (PresetListener) activity;
        }
        catch (ClassCastException e)
        {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement PresetListener");
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
        getLoaderManager().initLoader(ActivityMain.LOADER_STATIONS, null, this);
        
    }
    
    public void refresh()
    {
        //async task? possibly responsible for slow response times
        log("refresh()", "v");
        getLoaderManager().restartLoader(ActivityMain.LOADER_STATIONS, null, this);
        getListView().refreshDrawableState();
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        new MenuInflater(mContext).inflate(R.menu.station_selected, menu);
        
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        boolean handled = false;
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch(item.getItemId())
        {
            case R.id.delete_station:
                long[] ids = {(long)info.id}; 
                delete(ids);
                handled = true;
                break;
            case R.id.edit_station:
                edit(info.position);
                handled = true;
                break;
            default:
                throw new IllegalArgumentException("Unknown context menu item");
        }
        if (handled)
        {
            //mode.finish();
            //mActionMode = null;
        }
        return handled;
        //return super.onContextItemSelected(item);
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
            DialogFragment dialog = new DialogFragmentAdd();
            dialog.show(this.getFragmentManager(), "DialogFragmentAdd");
            return true;    
        }
        return super.onOptionsItemSelected(item);
        
    }
    
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        log("onCreateView()", "v");

        //FLAG_REGISTER_CONTENT_OBSERVER makes CursorAdapterStations.onContentChanged method get called
        mAdapter = new CursorAdapterStations(this.getActivity(), null, CursorAdapterStations.FLAG_REGISTER_CONTENT_OBSERVER);
        //mAdapter = new CursorAdapterStations(this.getActivity(), null, 0);
        
        this.setListAdapter(mAdapter);
        mContext = container.getContext();
        
        
        super.onCreateView(inflater, container, savedInstanceState);
        View view = super.onCreateView(inflater, container, savedInstanceState);
        
        //TODO use this custom view instead of default simple list view
        //View view = inflater.inflate(R.layout.stations_fragment, container, false);
        
        
        
        return view;
        
    }
    /*
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    { 
        
        log("onViewCreated()", "v");
        super.onViewCreated(view, savedInstanceState);
        //this.registerForContextMenu(this.getListView());
        
        
        
    }
    */
    @SuppressLint({ "NewApi", "InlinedApi" })
    @Override 
    public void onActivityCreated(Bundle savedInstanceState)
    {
        log("onActivityCreated()", "v");
        super.onActivityCreated(savedInstanceState);
        mListView = (ListView)getListView(); 
        
        
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB) {
            //FIXME for some reason the xml attribute for multiple modal isn't working
            mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
            mListView.setMultiChoiceModeListener(new MultiChoiceModeListenerStation(this, mListView));
        }
        else {
            //mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            registerForContextMenu(mListView);
        }
        
        
        
        
        //mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        //doesn't do anything if multi-choice-modal set
        
        //mListView.setMultiChoiceModeListener((MultiChoiceModeListener)this);
        //mListView.setOnItemClickListener(this);
        //mListView.setOnItemSelectedListener(this);
        //mListView.setOnItemLongClickListener(this);
        //TODO remove layout that doesn't handle emptiness
        this.setEmptyText(getResources().getString(R.string.loading_stations)); //this is done by layout, right?
    }
    

    //doesn't do anything if multi-choice-modal set (used to use AdapterView as first arg)
    @SuppressWarnings("deprecation")
    /*public boolean onItemLongClick(AdapterView<?> adapterView, View view,
            int position, long id) {
        String str= "list item longclicked";
        log(str, "v");
        
        boolean checked = true;
        ListView listView = (ListView) adapterView;
        listView.setItemChecked(position, checked);
        //view.setBackgroundColor(getResources().getColor(R.color.blue));
        //SparseBooleanArray checkedPositions = listView.getCheckedItemPositions();
        //int checkedCount = listView.getCheckedItemCount();
        //long[] var = listView.getCheckedItemIds();
        //view.setSelected(true);
        
        //listView.startActionMode(this);
        mActionMode = this.getSherlockActivity().startActionMode(this);
        mActionMode.setTitle(this.getCheckedItemCount() + " " + getResources().getString(R.string.selected)); 
        
        
        return true;
    }
    */
/*
    @Override
    public void onItemClick(AdapterView<?> adapterView, View item, int position, long id) {

        log("onItemClick()", "v");
        
        if (mActionMode != null) //in multi-select mode. click = check
        {
            //boolean checked = item.isSelected().isActivated();
            SparseBooleanArray checkedArray = ((ListView) adapterView).getCheckedItemPositions();
            boolean checked = checkedArray.get(position);
            
            int count = this.getCheckedItemCount();
            mActionMode.setTitle(String.valueOf(count) + " " + getResources().getString(R.string.selected));
            //mode.setSubtitle("Subtitle");
            
            String str = "Position " + String.valueOf(position) + " ";
            //RelativeLayout item = (RelativeLayout) mListView.getChildAt(position);
            //item.setActivated(checked);
            if (checked)
            {
                //Object item = mListView.getItemAtPosition(position);
                
                //item.setBackgroundColor("android:attr/activatedBackgroundColor");
                str += "checked";
                item.setBackgroundColor(getResources().getColor(R.color.blue));
            }
            else
            {
                str += "unchecked";
                //item.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                item.setBackgroundColor(getResources().getColor(R.color.white));
            }
            log(str, "i");
            
            if (count < 1)
            {
                mActionMode.finish();   
            }
            else
            {
                mActionMode.invalidate();   
            }
            
            
        }
        else //not in selection mode, play clicked item
        {
            Cursor cursor = (Cursor)adapterView.getItemAtPosition(position);
            //final String url = cursor.getString(cursor.getColumnIndexOrThrow(DbContractRadio.EntryStation.COLUMN_NAME_URL));
            final int preset = Integer.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER)));
            String str= "list item clicked, play preset " + preset + ". view position:";
            str += Integer.toString(position);
            str += ", row id:";
            str += Long.toString(id);
            log(str, "v");
            mListener.play(preset);
        }
        
    }*/
    
    @SuppressLint("NewApi")
    public void highlightPosition(int position)
    {
        View view = (View) mListView.getChildAt(position);
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackground(getResources().getDrawable(R.drawable.list_item_background_playing));
        }
        else
        {
            view.setBackgroundDrawable(getResources().getDrawable(R.drawable.list_item_background_playing));    
        }
    }
    
    @Override
    public void onListItemClick(ListView listView, View view, int position, long id)
    {
        log("onListItemClick()", "v");
        Cursor cursor = (Cursor)listView.getItemAtPosition(position);
        //final String url = cursor.getString(cursor.getColumnIndexOrThrow(DbContractRadio.EntryStation.COLUMN_NAME_URL));
        final int preset = Integer.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER)));
        String str= "list item clicked, play preset " + preset + ". view position:";
        str += Integer.toString(position);
        str += ", row id:";
        str += Long.toString(id);
        log(str, "v");
        //TODO decide whether to do this, or wait for details update intent to refresh the cursorloader and listfragment 
        //(when highlighting immediately, two could be highlighted at once if old highlight isn't cleared first)
        //highlightPosition(position);
        mListener.play(preset);
        
        /*
        super.onListItemClick(listView, view, position, id);
        
        SparseBooleanArray checked = listView.getCheckedItemPositions();
        boolean hasCheckedElement = false;
        for (int i = 0; i < checked.size() && !hasCheckedElement; i++) {
            hasCheckedElement = checked.valueAt(i);
        }

        if (hasCheckedElement) {
            if (mActionMode == null) {
                mActionMode = getSherlockActivity().startActionMode(this);
                mActionMode.invalidate();
            } else {
                mActionMode.invalidate();
            }
        } else {
            if (mActionMode != null) {
                mActionMode.finish();
            }
        }
        
        Cursor cursor = (Cursor)listView.getItemAtPosition(position);
        //final String url = cursor.getString(cursor.getColumnIndexOrThrow(DbContractRadio.EntryStation.COLUMN_NAME_URL));
        final int preset = Integer.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER)));
        String str= "list item clicked, play preset " + preset + ". view position:";
        str += Integer.toString(position);
        str += ", row id:";
        str += Long.toString(id);
        log(str, "v");
        mListener.play(preset);
        */
    }
    



    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        //async task? possibly responsible for slow response times
        log("onCreateLoader()", "v");
        String[] projection = {
                DbContractRadio.EntryStation._ID,
                DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER,
                DbContractRadio.EntryStation.COLUMN_NAME_TITLE,
                DbContractRadio.EntryStation.COLUMN_NAME_URL
        };
        
        String sortOrder = DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER + " ASC";
        Uri uri = ContentProviderRadio.CONTENT_URI_STATIONS;
        return new CursorLoader(this.getActivity().getApplicationContext(), uri, projection, null, null, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        log("onLoadFinished()", "v");
        switch(loader.getId())
        {
            case ActivityMain.LOADER_STATIONS:
                mAdapter.swapCursor((Cursor)cursor);
                this.setEmptyText(getResources().getString(R.string.empty_stations));
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        log("onLoaderReset()", "v");
        switch(loader.getId())
        {
            case ActivityMain.LOADER_STATIONS:
                mAdapter.swapCursor(null);
                break;
        }
    }
    
    //called from onContextItemSelected and onContextItemSelected, depending on android version
    public void delete(final long[] ids)
    {
        //delete confirmation dialog, set message, title, and icon
        String title = getResources().getString(R.string.delete_station_confirmation_title);
        String message = getResources().getString(R.string.delete_station_confirmation_message);
        if (ids.length > 1)
        {
            title = getResources().getString(R.string.delete_stations_confirmation_title);
            message = getResources().getString(R.string.delete_stations_confirmation_message);
        }
        AlertDialog deleteStationsDialog = new AlertDialog.Builder(this.getContext())
        .setTitle(title) 
        .setMessage(message) 

        .setPositiveButton(getResources().getString(R.string.delete_station), new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) { 
                String[] values = new String[ids.length];
                String where = DbContractRadio.EntryStation._ID + " in (";
                //String[] values = new String[1];
                //values[0] = String.valueOf(selected[0]);
                for (int i=0; i<ids.length; i++)
                {
                    where += "?, ";
                    values[i] = String.valueOf(ids[i]);
                }
                where += "'') ";
                //where= "_id in (?, ?, '')"
                int deletedCount = getActivity().getContentResolver().delete(ContentProviderRadio.CONTENT_URI_STATIONS, where, values);//(ContentProviderRadio.CONTENT_URI_STATIONS, selected);
                log("deleted " + String.valueOf(deletedCount), "v");
                //TODO handle case where delete affects (above or includes) currently playing station
                mListener.updateDetails();
                dialog.dismiss();
            }   

        })

        .setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        })
        .create();
        deleteStationsDialog.show();
    }

    //called from multichoicemodelistenerstation
    public void deleteSelected()
    {
        log("deleting selected", "i");
        long[] selected = mListView.getCheckedItemIds();
        delete(selected);
        
    }
    
    public void edit(int position)
    {
        Cursor cursor = (Cursor)mListView.getItemAtPosition(position);
        String title = cursor.getString(cursor.getColumnIndexOrThrow(DbContractRadio.EntryStation.COLUMN_NAME_TITLE));
        String url = cursor.getString(cursor.getColumnIndexOrThrow(DbContractRadio.EntryStation.COLUMN_NAME_URL));
        url += "";
        LayoutInflater inflater = LayoutInflater.from(mContext);
        final View editView = inflater.inflate(R.layout.dialog_station_details, null);
        final long id = cursor.getLong(cursor.getColumnIndexOrThrow(DbContractRadio.EntryStation._ID));
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
        builder.setTitle(R.string.station_details_title);
        builder.show();
    }
    
    public void editSelected()
    {
        log("editing selected", "i");
        int position = getSelectedPosition();
        if (position == -1) //should never happen
        {
            throw new NoSuchElementException("Selected element not found.");
        }
        edit(position);
        
    }
    
    private void editStation(final long id, View view)
    {
        log("id:" + String.valueOf(id), "i"); 
        // User touched the dialog's positive button
        log("edit station confirmed", "i");
        EditText titleView = (EditText)view.findViewById(R.id.station_title);
        EditText urlView = (EditText)view.findViewById(R.id.station_url);
        
        //int preset = 1;
        String title = titleView.getText().toString().trim();
        String url = urlView.getText().toString().trim();
        
        boolean valid = ServiceRadioPlayer.validateUrl(url);
        if (valid)
        {
            ContentValues values = new ContentValues();
            //values.put(DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER, preset);
            values.put(DbContractRadio.EntryStation.COLUMN_NAME_TITLE, title);
            values.put(DbContractRadio.EntryStation.COLUMN_NAME_URL, url);
            //CursorLoader var = getLoaderManager().getLoader(ActivityMain.LOADER_STATIONS);
            //TODO see if there is a better way to do this, like addId or something
            Uri uri = Uri.parse(ContentProviderRadio.CONTENT_URI_STATIONS.toString() + "/" + String.valueOf(id));
            int updatedCount = this.getActivity().getContentResolver().update(uri, values, null, null);
            log("updated " + updatedCount + " rows.", "v");
            //TODO handle case where edited currently playing station
            mListener.updateDetails();
        }
        else
        {
            //code duplication in ActivityMain
            log("URL " + url + " not valid", "v");
            LayoutInflater inflater = LayoutInflater.from(mContext);
            final View editView = inflater.inflate(R.layout.dialog_station_details, null);
            titleView = ((EditText)editView.findViewById(R.id.station_title));
            titleView.setText(title);
            urlView = ((EditText)editView.findViewById(R.id.station_url));
            urlView.setText(url);
            urlView.requestFocus();
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
            builder.setTitle("URL appears invalid. Try again");
            builder.show();
        }
        
        
        
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
    

    

    
    

    /*
    private int getCheckedItemCount()
    {
        int count = 0;
        if (mListView != null)
        {
            SparseBooleanArray checked = mListView.getCheckedItemPositions();
            //boolean hasCheckedElement = false;
            
            for (int i = 0; i < checked.size(); i++) {
               // hasCheckedElement |= checked.valueAt(i);
                if (checked.valueAt(i))
                {
                    count++;    
                }
                
            }
    
        }
        
        return count;
    }
*/


    
    public void log(String text, String level)
    {
        mLogger.log(this.getActivity(), "FragmentStations:\t\t"+text, level);
    }
    

}
