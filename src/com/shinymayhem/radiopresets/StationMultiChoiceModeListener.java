package com.shinymayhem.radiopresets;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ListView;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class StationMultiChoiceModeListener implements MultiChoiceModeListener {
	protected ActionMode mActionMode;
	protected int mSelectedCount;
	protected ListView mListView;
	StationsFragment mHost;
	
	StationMultiChoiceModeListener(StationsFragment host, ListView lv) {
	    this.mHost=host;
	    this.mListView=lv;
	  }
	
	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		mSelectedCount = mListView.getCheckedItemCount();
		//mSelectedCount=this.getCheckedItemCount();
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
		
		int count = mListView.getCheckedItemCount();
		mode.setTitle(String.valueOf(count) + " selected");
		//mode.setSubtitle("Subtitle");

		String str = "Position " + String.valueOf(position) + " ";
		//RelativeLayout item = (RelativeLayout) mListView.getChildAt(position);
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
		mHost.log(str, "i");
		mode.invalidate();
	}
	

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		boolean handled = false;
		switch(item.getItemId())
		{
			case R.id.delete_station:
				mHost.deleteSelected();
				handled = true;
				break;
			case R.id.edit_station:
				mHost.editSelected();
				handled = true;
				break;
			default:
				throw new IllegalArgumentException("Unknown action menu item");
		}
		if (handled)
		{
			//mode.finish();
			//mActionMode = null;
		}
		return handled;
	}
	


}