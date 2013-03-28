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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

public class DbContractRadio {
    
    
    private static final String TEXT_TYPE = " TEXT";
    private static final String DATE_TYPE = " DATE";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_STATIONS =
        "CREATE TABLE " + DbContractRadio.EntryStation.TABLE_NAME + " (" +
        DbContractRadio.EntryStation._ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + COMMA_SEP +
        DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER + " INTEGER NOT NULL" + COMMA_SEP +
        DbContractRadio.EntryStation.COLUMN_NAME_TITLE + TEXT_TYPE + COMMA_SEP +
        DbContractRadio.EntryStation.COLUMN_NAME_URL + TEXT_TYPE + " NOT NULL" + COMMA_SEP +
        DbContractRadio.EntryStation.COLUMN_NAME_FORMAT + TEXT_TYPE +
        " )" +
        ";";
    private static final String SQL_CREATE_LIKES = 
            "CREATE TABLE " + DbContractRadio.EntryLike.TABLE_NAME + " (" +
            DbContractRadio.EntryLike._ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + COMMA_SEP +
            DbContractRadio.EntryLike.COLUMN_NAME_ARTIST + TEXT_TYPE + COMMA_SEP +
            DbContractRadio.EntryLike.COLUMN_NAME_SONG + TEXT_TYPE + COMMA_SEP +
            DbContractRadio.EntryLike.COLUMN_NAME_STATION_TITLE + TEXT_TYPE + COMMA_SEP +
            DbContractRadio.EntryLike.COLUMN_NAME_STATION_URL + TEXT_TYPE + COMMA_SEP +
            DbContractRadio.EntryLike.COLUMN_NAME_DATE_ADDED + DATE_TYPE + COMMA_SEP +
            DbContractRadio.EntryLike.COLUMN_NAME_STATUS + TEXT_TYPE + 
            " ) " +
            ";";
    private static final String SQL_CREATE_DISLIKES = 
            "CREATE TABLE " + DbContractRadio.EntryDislike.TABLE_NAME + " (" +
            DbContractRadio.EntryDislike._ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + COMMA_SEP +
            DbContractRadio.EntryDislike.COLUMN_NAME_ARTIST + TEXT_TYPE + COMMA_SEP +
            DbContractRadio.EntryDislike.COLUMN_NAME_SONG + TEXT_TYPE + COMMA_SEP +
            DbContractRadio.EntryDislike.COLUMN_NAME_STATION_TITLE + TEXT_TYPE + COMMA_SEP +
            DbContractRadio.EntryDislike.COLUMN_NAME_STATION_URL + TEXT_TYPE + COMMA_SEP +
            DbContractRadio.EntryDislike.COLUMN_NAME_DATE_ADDED + DATE_TYPE + COMMA_SEP +
            DbContractRadio.EntryDislike.COLUMN_NAME_STATUS + TEXT_TYPE + 
            " ) " +
            ";";
    
    private static final String ADD_SAMPLE_STATIONS = 
        "insert into " + DbContractRadio.EntryStation.TABLE_NAME + " (" +
            DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER + COMMA_SEP +
            DbContractRadio.EntryStation.COLUMN_NAME_TITLE + COMMA_SEP +
            DbContractRadio.EntryStation.COLUMN_NAME_URL +
            " )" + 
            "  " +
            "select 1, 'ElectroSwing Revolution', 'http://streamplus17.leonex.de:39060' union " +
            "select 2, 'Jazz Radio Electroswing', 'http://jazz-wr04.ice.infomaniak.ch/jazz-wr04-128.mp3' union " +
            "select 3, 'Bart&Baker', 'http://jazz-wr14.ice.infomaniak.ch/jazz-wr14-128.mp3' " + 
            ";";
    
    private static final String SQL_DELETE_STATIONS = "DROP TABLE IF EXISTS " + DbContractRadio.EntryStation.TABLE_NAME;
    
    private DbContractRadio() {}
    
    public static abstract class EntryStation implements BaseColumns {
        public static final String TABLE_NAME = "stations";
        public static final String COLUMN_NAME_PRESET_NUMBER = "preset_number";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_URL = "url";
        public static final String COLUMN_NAME_FORMAT = "format";
        
    }
    
    public static abstract class EntryLike implements BaseColumns {
        public static final String TABLE_NAME = "likes";
        public static final String COLUMN_NAME_ARTIST = "artist";
        public static final String COLUMN_NAME_SONG = "song";
        public static final String COLUMN_NAME_STATION_TITLE = "station_title";
        public static final String COLUMN_NAME_STATION_URL = "station_url";
        public static final String COLUMN_NAME_DATE_ADDED = "date_added";
        public static final String COLUMN_NAME_STATUS = "status";
    }
    
    public static abstract class EntryDislike implements BaseColumns {
        public static final String TABLE_NAME = "dislikes";
        public static final String COLUMN_NAME_ARTIST = "artist";
        public static final String COLUMN_NAME_SONG = "song";
        public static final String COLUMN_NAME_STATION_TITLE = "station_title";
        public static final String COLUMN_NAME_STATION_URL = "station_url";
        public static final String COLUMN_NAME_DATE_ADDED = "date_added";
        public static final String COLUMN_NAME_STATUS = "status";
    }
    
    public static class DbHelperRadio extends SQLiteOpenHelper {

        public static final String DATABASE_NAME = "Radio.db";
        public static final int DATABASE_VERSION = 4; //if this is changed, update onUpgrade() to not delete data
        
        public DbHelperRadio(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_STATIONS);
            db.execSQL(ADD_SAMPLE_STATIONS);
            db.execSQL(SQL_CREATE_LIKES);
            db.execSQL(SQL_CREATE_DISLIKES);
            Log.i(getClass().toString(), "Created database");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion == 3 && newVersion == 4)
            {
                String sql = "alter table " + DbContractRadio.EntryStation.TABLE_NAME + 
                        " add column " + DbContractRadio.EntryStation.COLUMN_NAME_FORMAT + TEXT_TYPE + ";";
                db.execSQL(sql);
                db.execSQL(SQL_CREATE_LIKES);
                db.execSQL(SQL_CREATE_DISLIKES);
            }
            else
            {
                // discard the data and start over
                db.execSQL(SQL_DELETE_STATIONS);
                onCreate(db);
            }

        }

    }
    
}
