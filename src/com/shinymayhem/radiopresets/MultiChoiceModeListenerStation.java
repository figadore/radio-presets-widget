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

import android.annotation.TargetApi;
import android.os.Build;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ListView;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class MultiChoiceModeListenerStation implements MultiChoiceModeListener {
    private static final boolean LOCAL_LOGV = ActivityMain.LOCAL_LOGV;

    protected ActionMode mActionMode;
    protected int mSelectedCount;
    protected ListView mListView;
    FragmentStations mHost;
    
    MultiChoiceModeListenerStation(FragmentStations host, ListView lv) {
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

        //RelativeLayout item = (RelativeLayout) mListView.getChildAt(position);
            //item.setActivated(checked);
        if (LOCAL_LOGV) 
        {
            String str = "Position " + String.valueOf(position) + " ";
            
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
        }
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
