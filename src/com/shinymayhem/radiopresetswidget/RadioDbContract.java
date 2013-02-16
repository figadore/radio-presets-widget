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

package com.shinymayhem.radiopresetswidget;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class RadioDbContract {
	
	
	private static final String TEXT_TYPE = " TEXT";
	private static final String COMMA_SEP = ",";
	private static final String SQL_CREATE_STATIONS =
	    "CREATE TABLE " + RadioDbContract.StationEntry.TABLE_NAME + " (" +
		RadioDbContract.StationEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
		RadioDbContract.StationEntry.COLUMN_NAME_PRESET_NUMBER + " INTEGER" + COMMA_SEP +
	    RadioDbContract.StationEntry.COLUMN_NAME_TITLE + TEXT_TYPE + COMMA_SEP +
	    RadioDbContract.StationEntry.COLUMN_NAME_URL + TEXT_TYPE +
	    " )";
	//TODO insert sample stations?

	//private static final String SQL_DELETE_STATIONS = "DROP TABLE IF EXISTS " + RadioDbContract.StationEntry.TABLE_NAME;
	
	private RadioDbContract() {}
	
	public static abstract class StationEntry implements BaseColumns {
		public static final String TABLE_NAME = "stations";
	    public static final String COLUMN_NAME_PRESET_NUMBER = "preset_number";
	    public static final String COLUMN_NAME_TITLE = "title";
	    public static final String COLUMN_NAME_URL = "url";
	    
	}
	
	public static class StationsDbHelper extends SQLiteOpenHelper {

		public static final String DATABASE_NAME = "Radio.db";
		public static final int DATABASE_VERSION = 1;
		
		public StationsDbHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(SQL_CREATE_STATIONS);

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	        // discard the data and start over
	        //db.execSQL(SQL_DELETE_STATIONS);
	        //onCreate(db);

		}

	}

	
}
