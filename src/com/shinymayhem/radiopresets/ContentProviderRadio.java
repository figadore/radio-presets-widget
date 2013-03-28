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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.shinymayhem.radiopresets.DbContractRadio.DbHelperRadio;
/** Provides CRUD access to db through URIs
 * 
 * @author Reese Wilson
 *
 */
public class ContentProviderRadio extends ContentProvider {

    protected ActivityLogger mLogger = new ActivityLogger();
    private DbHelperRadio mStationsHelper; 
    
    private static final String AUTHORITY = "com.shinymayhem.radiopresets.contentprovider";
    private static final int URI_STATIONS = 1;
    private static final int URI_STATION_ID = 2;
    private static final int URI_PRESET = 3;
    private static final int URI_PRESET_MAX = 4;
    private static final int URI_LIKES = 5;
    private static final int URI_DISLIKES = 6;
    private static final int URI_LIKE_ID = 7;
    private static final int URI_DISLIKE_ID = 8;
    
    //segments
    private static final String SEGMENT_STATIONS_BASE = "stations";
    private static final String SEGMENT_PRESETS_BASE = "presets";
    private static final String SEGMENT_PRESET_MAX = SEGMENT_PRESETS_BASE + "/max";
    private static final String SEGMENT_LIKES_BASE = "likes";
    private static final String SEGMENT_DISLIKES_BASE = "dislikes";
    
    //make each segment type available to other classes
    //content://com.shinymayhem.radiopresets.contentprovider/stations
    public static final Uri CONTENT_URI_STATIONS = Uri.parse("content://" + AUTHORITY + "/" + SEGMENT_STATIONS_BASE);
    //content://com.shinymayhem.radiopresets.contentprovider/presets
    public static final Uri CONTENT_URI_PRESETS = Uri.parse("content://" + AUTHORITY + "/" + SEGMENT_PRESETS_BASE);
    //content://com.shinymayhem.radiopresets.contentprovider/presets/max
    public static final Uri CONTENT_URI_PRESETS_MAX = Uri.parse("content://" + AUTHORITY + "/" + SEGMENT_PRESET_MAX);
    //content://com.shinymayhem.radiopresets.contentprovider/likes
    public static final Uri CONTENT_URI_LIKES = Uri.parse("content://" + AUTHORITY + "/" + SEGMENT_LIKES_BASE);
    //content://com.shinymayhem.radiopresets.contentprovider/dislikes
    public static final Uri CONTENT_URI_DISLIKES = Uri.parse("content://" + AUTHORITY + "/" + SEGMENT_DISLIKES_BASE);
    
    
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static
    {
        sUriMatcher.addURI(AUTHORITY, SEGMENT_STATIONS_BASE, URI_STATIONS);
        sUriMatcher.addURI(AUTHORITY, SEGMENT_STATIONS_BASE+"/#", URI_STATION_ID);
        sUriMatcher.addURI(AUTHORITY, SEGMENT_PRESETS_BASE+"/#", URI_PRESET);
        sUriMatcher.addURI(AUTHORITY, SEGMENT_PRESET_MAX, URI_PRESET_MAX);
        sUriMatcher.addURI(AUTHORITY, SEGMENT_LIKES_BASE, URI_LIKES);
        sUriMatcher.addURI(AUTHORITY, SEGMENT_DISLIKES_BASE, URI_DISLIKES);
        sUriMatcher.addURI(AUTHORITY, SEGMENT_LIKES_BASE+"/#", URI_LIKE_ID);
        sUriMatcher.addURI(AUTHORITY, SEGMENT_DISLIKES_BASE+"/#", URI_DISLIKE_ID);
    }
    

    @Override
    public boolean onCreate() {
        mStationsHelper = new DbHelperRadio(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String groupBy = null;
        String having = null;
        String table = null;
        String limit = null;
        SQLiteDatabase db;
        switch (sUriMatcher.match(uri))
        {
        case URI_STATIONS: 
            table = DbContractRadio.EntryStation.TABLE_NAME;
            db = mStationsHelper.getReadableDatabase();
            break;
        case URI_STATION_ID:
            table = DbContractRadio.EntryStation.TABLE_NAME;
            db = mStationsHelper.getReadableDatabase();
            long id = ContentUris.parseId(uri);
            selection = addColumn(DbContractRadio.EntryStation._ID, selection);
            selectionArgs = addArg(id, selectionArgs);
            break;
        case URI_PRESET:
            table = DbContractRadio.EntryStation.TABLE_NAME;
            db = mStationsHelper.getReadableDatabase();
            long presetNumber = ContentUris.parseId(uri);
            selection = addColumn(DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER, selection);
            selectionArgs = addArg(presetNumber, selectionArgs);
            break;
        case URI_PRESET_MAX:
            table = DbContractRadio.EntryStation.TABLE_NAME;
            db = mStationsHelper.getReadableDatabase();
            //override any projection/selection arguments to get the max
            String[] newProjection = {"max(" + DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER + ") as " + DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER};
            //String newSelection = DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER + "= max(" + DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER + ")";
            selection = null;
            selectionArgs = null;
            projection = newProjection;
            break;
        case URI_LIKES:
            table = DbContractRadio.EntryLike.TABLE_NAME;
            db = mStationsHelper.getReadableDatabase();
            break;
        case URI_DISLIKES:
            table = DbContractRadio.EntryDislike.TABLE_NAME;
            db = mStationsHelper.getReadableDatabase();
            break;
        default:
            throw new IllegalArgumentException("Unknown query URI:" + uri);
        }
        
        Cursor cursor = db.query(
                table, 
                projection, 
                selection, 
                selectionArgs, 
                groupBy,
                having,
                sortOrder, 
                limit
                //Integer.toString(ActivityMain.BUTTON_LIMIT)
            );
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        //log("query uri:" + uri, "v");
        return cursor;
    }
    
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        
        String table = null;
        SQLiteDatabase db;
        boolean collapsePresets = false;
        long id;
        switch (sUriMatcher.match(uri))
        {
        case URI_STATIONS:
            table = DbContractRadio.EntryStation.TABLE_NAME;
            db = mStationsHelper.getWritableDatabase();
            collapsePresets = true;
            break;
        case URI_STATION_ID:
            table = DbContractRadio.EntryStation.TABLE_NAME;
            db = mStationsHelper.getWritableDatabase();
            id = ContentUris.parseId(uri);
            selection = addColumn(DbContractRadio.EntryStation._ID, selection);
            selectionArgs = addArg(id, selectionArgs);
            collapsePresets = true;
            break;
        case URI_LIKES:
            table = DbContractRadio.EntryLike.TABLE_NAME;
            db = mStationsHelper.getWritableDatabase();
            break;
        case URI_LIKE_ID:
            table = DbContractRadio.EntryLike.TABLE_NAME;
            db = mStationsHelper.getWritableDatabase();
            id = ContentUris.parseId(uri);
            selection = addColumn(DbContractRadio.EntryLike._ID, selection);
            selectionArgs = addArg(id, selectionArgs);
            break;
        case URI_DISLIKES:
            table = DbContractRadio.EntryDislike.TABLE_NAME;
            db = mStationsHelper.getWritableDatabase();
            break;
        case URI_DISLIKE_ID:
            table = DbContractRadio.EntryDislike.TABLE_NAME;
            db = mStationsHelper.getWritableDatabase();
            id = ContentUris.parseId(uri);
            selection = addColumn(DbContractRadio.EntryDislike._ID, selection);
            selectionArgs = addArg(id, selectionArgs);
            break;
        default:
            throw new IllegalArgumentException("Unknown delete URI:" + uri);
        }

        int deletedCount = db.delete(table, selection, selectionArgs);
        if (collapsePresets)
        {
            this.collapsePresetNumbers();
        }
        //notify content resolver of data change
        getContext().getContentResolver().notifyChange(uri, null);
        log("delete uri:" + uri + ". " + String.valueOf(deletedCount) + " deleted", "v");
        return deletedCount;
        
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        
        String table = null;
        SQLiteDatabase db;
        switch (sUriMatcher.match(uri))
        {
        case URI_STATIONS:
            table = DbContractRadio.EntryStation.TABLE_NAME;
            //if preset isset, make room, otherwise, append
            if (values.containsKey(DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER))
            {
                makeRoomForPreset(values.getAsInteger(DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER), 0);
            }
            else
            {
                int preset = this.getMaxPresetNumber() + 1;
                values.put(DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER, preset);
            }
            db = mStationsHelper.getWritableDatabase();
            
            break;
        case URI_LIKES:
            table = DbContractRadio.EntryLike.TABLE_NAME;
            String artist = values.getAsString(DbContractRadio.EntryLike.COLUMN_NAME_ARTIST);
            String song = values.getAsString(DbContractRadio.EntryLike.COLUMN_NAME_SONG);
            long likeId = this.likeExists(artist, song);
            if (likeId > 0)
            {
                String selection = DbContractRadio.EntryLike.COLUMN_NAME_ARTIST + " = ? and " + DbContractRadio.EntryLike.COLUMN_NAME_SONG + " = ? ";
                String[] selectionArgs = {String.valueOf(artist), String.valueOf(song)};
                this.update(uri, values, selection, selectionArgs);
                return Uri.parse(SEGMENT_LIKES_BASE + "/" + String.valueOf(likeId));
            }
            db = mStationsHelper.getWritableDatabase();
            break;
        case URI_DISLIKES:
            table = DbContractRadio.EntryDislike.TABLE_NAME;
            String dislikedArtist = values.getAsString(DbContractRadio.EntryDislike.COLUMN_NAME_ARTIST);
            String dislikedSong = values.getAsString(DbContractRadio.EntryDislike.COLUMN_NAME_SONG);
            long dislikeId = this.dislikeExists(dislikedArtist, dislikedSong);
            if (dislikeId > 0)
            {
                String selection = DbContractRadio.EntryDislike.COLUMN_NAME_ARTIST + " = ? and " + DbContractRadio.EntryDislike.COLUMN_NAME_SONG + " = ? ";
                String[] selectionArgs = {String.valueOf(dislikedArtist), String.valueOf(dislikedSong)};
                this.update(uri, values, selection, selectionArgs);
                return Uri.parse(SEGMENT_DISLIKES_BASE + "/" + String.valueOf(dislikeId));
            }
            db = mStationsHelper.getWritableDatabase();
            break;
        default:
            throw new IllegalArgumentException("Unknown insert URI:" + uri);
        }

        long id = db.insert(table, null, values);
        //notify content resolver of data change
        getContext().getContentResolver().notifyChange(uri, null);
        log("insert uri:" + uri + ". id of insert:" + String.valueOf(id), "v");
        return Uri.parse(SEGMENT_STATIONS_BASE + "/" + id);
        
        
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        String table = null;
        SQLiteDatabase db;
        long id;
        switch (sUriMatcher.match(uri))
        {
        case URI_STATIONS:
            table = DbContractRadio.EntryStation.TABLE_NAME;
            db = mStationsHelper.getWritableDatabase();
            break;
        case URI_STATION_ID:
            table = DbContractRadio.EntryStation.TABLE_NAME;
            db = mStationsHelper.getWritableDatabase();
            id = ContentUris.parseId(uri);
            selection = addColumn(DbContractRadio.EntryStation._ID, selection);
            selectionArgs = addArg(id, selectionArgs);
            if (values.containsKey(DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER)) //if preset specified, could be updated
            {
                makeRoomForPreset(values.getAsInteger(DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER), (int)id);
            }
            break;
        case URI_LIKES:
            table = DbContractRadio.EntryLike.TABLE_NAME;
            db = mStationsHelper.getWritableDatabase();
            break;
        case URI_LIKE_ID:
            table = DbContractRadio.EntryLike.TABLE_NAME;
            db = mStationsHelper.getWritableDatabase();
            id = ContentUris.parseId(uri);
            selection = addColumn(DbContractRadio.EntryLike._ID, selection);
            selectionArgs = addArg(id, selectionArgs);
            break;
        case URI_DISLIKES:
            table = DbContractRadio.EntryDislike.TABLE_NAME;
            db = mStationsHelper.getWritableDatabase();
            break;
        case URI_DISLIKE_ID:
            table = DbContractRadio.EntryDislike.TABLE_NAME;
            db = mStationsHelper.getWritableDatabase();
            id = ContentUris.parseId(uri);
            selection = addColumn(DbContractRadio.EntryDislike._ID, selection);
            selectionArgs = addArg(id, selectionArgs);
            break;
        default:
            throw new IllegalArgumentException("Unknown query URI:" + uri);
        }
        
        int updatedCount = db.update(table, values, selection, selectionArgs);
        //notify content resolver of data change
        getContext().getContentResolver().notifyChange(uri, null);
        log("update uri:" + uri + ". " + String.valueOf(updatedCount) + " updated", "v");
        return updatedCount;
        
    }
    
    //not used because not supported until api 11
    /*
    @Override 
    public Bundle call(String method, String arg, Bundle extras) 
    {
        Bundle values = new Bundle();
        if (method == "getMaxPresetNumber")
        {
            values.putInt(DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER, this.getMaxPresetNumber());
            return values;
        }
        else
        {
            return super.call(method, arg, extras);
        }
        
    }*/
    
    private long likeExists(String artist, String song)
    {
        Uri uri = CONTENT_URI_LIKES;
        String[] projection = {DbContractRadio.EntryLike._ID};
        String selection = DbContractRadio.EntryLike.COLUMN_NAME_ARTIST + " = ? and " + DbContractRadio.EntryLike.COLUMN_NAME_SONG + " = ? ";
        String[] selectionArgs = {String.valueOf(artist), String.valueOf(song)};
        String sortOrder = null;
        Cursor cursor = this.query(uri, projection, selection, selectionArgs, sortOrder);
        long id = 0;
        if (cursor.getCount() > 0)
        {
            cursor.moveToFirst();
            id = cursor.getLong(cursor.getColumnIndex(DbContractRadio.EntryLike._ID));
        }
        cursor.close();
        return id;
    }
    
    
    private long dislikeExists(String artist, String song)
    {
        Uri uri = CONTENT_URI_DISLIKES;
        String[] projection = {DbContractRadio.EntryDislike._ID};
        String selection = DbContractRadio.EntryDislike.COLUMN_NAME_ARTIST + " = ? and " + DbContractRadio.EntryDislike.COLUMN_NAME_SONG + " = ? ";
        String[] selectionArgs = {String.valueOf(artist), String.valueOf(song)};
        String sortOrder = null;
        Cursor cursor = this.query(uri, projection, selection, selectionArgs, sortOrder);
        long id = 0;
        if (cursor.getCount() > 0)
        {
            cursor.moveToFirst();
            id = cursor.getLong(cursor.getColumnIndex(DbContractRadio.EntryDislike._ID));
        }
        cursor.close();
        return id;
    }
    
    private int getMaxPresetNumber()
    {
        Uri uri = CONTENT_URI_PRESETS_MAX;
        String[] projection = null;
        String selection = null;
        String[] selectionArgs = null;
        String sortOrder = null;
        Cursor cursor = this.query(uri, projection, selection, selectionArgs, sortOrder);
        long preset = 0;
        if (cursor.getCount() > 0)      
        {
            cursor.moveToFirst();
            preset = cursor.getLong(cursor.getColumnIndexOrThrow(DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER));  
        }
        
        cursor.close();
        return (int)preset;
    }
    
    /**
     * Sets all presets to increment, starting at 1, keeping the existing order. 
     * e.g. presets 1, 4 and 6 exist, collapses to 1, 2 and 3. 
     */
    private void collapsePresetNumbers()
    {
        //SQLiteDatabase db = mStationsHelper.getReadableDatabase();    
        log("collapsePresetNumbers()", "v");
        Uri uri = CONTENT_URI_STATIONS;
        String[] projection = {DbContractRadio.EntryStation._ID, DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER};
        String selection = null;
        String[] selectionArgs = null;
        String sortOrder = DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER;
        Cursor cursor = this.query(uri, projection, selection, selectionArgs, sortOrder);
        if (cursor.getCount() > 0)
        {
            cursor.moveToFirst();
            for (int i = 0; i<cursor.getCount(); i++)
            {
                
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(DbContractRadio.EntryStation._ID));
                long preset = cursor.getLong(cursor.getColumnIndexOrThrow(DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER));
                uri = Uri.parse(CONTENT_URI_STATIONS.toString() + "/" + String.valueOf(id));
                ContentValues values = new ContentValues();
                values.put(DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER, i+1);
                log("setting preset number for " + String.valueOf(id) + " from " + String.valueOf(preset) + " to " + String.valueOf(i+1), "v");
                this.update(uri, values, selection, selectionArgs);
                cursor.moveToNext();
            }
        }
        cursor.close();
        
    }
    
    /**
     * Increments all preset numbers equal to or above the desired new preset number (if any). If update() called and preset number is the same, nothing is done
     * @param preset 
     * @param id row id of station 
     * @return Count of rows updated by this operation
     */
    private int makeRoomForPreset(int preset, int id)
    {
        log("making room for preset:" + preset, "v");
        //check for existing entry with same preset but different id (because update() could be called with same id)
        Uri uri = CONTENT_URI_STATIONS;
        String[] projection = {DbContractRadio.EntryStation._ID};
        String selection = DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER + " = ? and not " + DbContractRadio.EntryStation._ID + " = ? ";
        String[] selectionArgs = {String.valueOf(preset), String.valueOf(id)};
        String sortOrder = null;
        Cursor cursor = this.query(uri, projection, selection, selectionArgs, sortOrder);
        int updatedCount = 0;
        //if existing entry exists, increment all presets equal to or above current
        if (cursor.getCount() > 0)
        {
            log("preset exists, increment it and all above", "v");
            SQLiteDatabase db;
            db = mStationsHelper.getWritableDatabase();
            String column = DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER;
            /*db.beginTransaction();
            db.execSQL(sql);*/
            
            //get list of stations that will be updates
            String[] newSelectionArgs = {String.valueOf(preset)};
            String sql = "select " + DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER +
                    " from " + DbContractRadio.EntryStation.TABLE_NAME + 
                    " where " + column + ">= ? ";
            Cursor newCursor = db.rawQuery(sql, newSelectionArgs);
            updatedCount = newCursor.getCount();
            log("incrementing " + String.valueOf(updatedCount) + " presets", "v");
                    //(int)newCursor.getLong(newCursor.getColumnIndexOrThrow(DbContractRadio.EntryStation.COLUMN_NAME_PRESET_NUMBER));
            newCursor.close();
            if (updatedCount > 0)
            {
                sql = "update " + DbContractRadio.EntryStation.TABLE_NAME + " " +
                        " set " + column + " = " + column + " + 1 " +
                        " where " + column + ">= " + String.valueOf(preset); //TODO sanitize
                db.execSQL(sql);    
            }
            
            //db.rawQuery("select changes()", null);
            //db.endTransaction();
            //String[] newSelectionArgs = {String.valueOf(preset)};
            //Cursor newCursor = db.rawQuery(sql, newSelectionArgs);
            /**/
            /*
            ContentValues values = new ContentValues();
            values.put(column, column + " + 1 ");
            String whereClause = column + " >= ? ";
            String[] whereArgs = {String.valueOf(preset)}; 
            updatedCount = db.update(DbContractRadio.EntryStation.TABLE_NAME, values, whereClause, whereArgs);
            */
            //newCursor.moveToFirst();
            
            //TODO find out if these are needed, or what contentprovider closes automatically and shouldn't be closed
            
            db.close();
            //return number of updated stations
            
        }
        else
        {
            log("preset doesn't exists yet, ok to add", "v");
        }
        cursor.close();
        return updatedCount;
    }
    
    
    /**
     * add id (or other column) to selection string
     * @param column name of column to add
     * @param selection current selection string
     * @return new selection string
     */
    private String addColumn(String column, String selection)
    {
        if (selection == null || selection.isEmpty())
        {
            return column + " = ? ";    
        }
        return selection + " and " + column + " = ?";
    }
    
    /**
     * add id (or other column) to selection args
     * @param id
     * @param selectionArgs
     * @return new string[] selection args
     */
    
    private String[] addArg(long id, String[] selectionArgs)
    {
        List<String> args = new ArrayList<String>();
        if (selectionArgs != null)
        {
            Collections.addAll(args, selectionArgs);    
        }
        args.add(String.valueOf(id));
        //selectionArgs = (String[])(args.toArray());
        selectionArgs = args.toArray(new String[0]);
        return selectionArgs;
    }
    
    @Override
    public String getType(Uri uri) {
        //TODO check if this is right
        log("getType called", "e");
        throw new UnsupportedOperationException("getType not supported. if this is seen, return null instead?");
    }

    private void log(String text, String level)
    {
        mLogger.log(getContext(), "ContentProviderRadio", "ContentProviderRadio:\t\t"+text, level);
    }
    

}
