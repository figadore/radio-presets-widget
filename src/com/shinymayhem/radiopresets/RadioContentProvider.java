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

import com.shinymayhem.radiopresets.RadioDbContract.StationsDbHelper;

public class RadioContentProvider extends ContentProvider {

	protected Logger mLogger = new Logger();
	
	private static final String AUTHORITY = "com.shinymayhem.radiopresets.contentprovider";
	private static final int SEGMENT_STATION_ID = 2;
	private static final int SEGMENT_STATIONS = 1;
	private static final String SEGMENT_STATIONS_BASE = "stations";
	public static final Uri CONTENT_URI_STATIONS = Uri.parse("content://" + AUTHORITY
		      + "/" + SEGMENT_STATIONS_BASE);
	private StationsDbHelper mStationsHelper; 
	
	private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	static
	{
		sUriMatcher.addURI(AUTHORITY, SEGMENT_STATIONS_BASE, SEGMENT_STATIONS);
		sUriMatcher.addURI(AUTHORITY, SEGMENT_STATIONS_BASE+"/#", SEGMENT_STATION_ID);
	}
	

	@Override
	public boolean onCreate() {
		mStationsHelper = new StationsDbHelper(getContext());
		return true;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		
		String table = null;
		SQLiteDatabase db;
		switch (sUriMatcher.match(uri))
		{
		case SEGMENT_STATIONS:
			table = RadioDbContract.StationEntry.TABLE_NAME;
			db = mStationsHelper.getWritableDatabase();
			break;
		case SEGMENT_STATION_ID:
			table = RadioDbContract.StationEntry.TABLE_NAME;
			db = mStationsHelper.getWritableDatabase();
			long id = ContentUris.parseId(uri);
			selection = addColumn(RadioDbContract.StationEntry._ID, selection);
			selectionArgs = addArg(id, selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown delete URI:" + uri);
		}
		
		int deletedCount = db.delete(table, selection, selectionArgs);
		//notify content resolver of data change
		getContext().getContentResolver().notifyChange(uri, null);
		log("delete uri:" + uri + ". " + String.valueOf(deletedCount) + " deleted", "i");
		return deletedCount;
		
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		
		String table = null;
		SQLiteDatabase db;
		switch (sUriMatcher.match(uri))
		{
		case SEGMENT_STATIONS:
			table = RadioDbContract.StationEntry.TABLE_NAME;
			db = mStationsHelper.getWritableDatabase();
			break;
		default:
			throw new IllegalArgumentException("Unknown insert URI:" + uri);
		}
		
		long id = db.insert(table, null, values);
		//notify content resolver of data change
		getContext().getContentResolver().notifyChange(uri, null);
		log("insert uri:" + uri + ". id of insert:" + String.valueOf(id), "i");
		return Uri.parse(SEGMENT_STATIONS_BASE + "/" + id);
		
		
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,	String sortOrder) {
		String groupBy = null;
		String having = null;
		String table = null;
		SQLiteDatabase db;
		switch (sUriMatcher.match(uri))
		{
		case SEGMENT_STATIONS: 
			table = RadioDbContract.StationEntry.TABLE_NAME;
			db = mStationsHelper.getReadableDatabase();
			break;
		case SEGMENT_STATION_ID:
			table = RadioDbContract.StationEntry.TABLE_NAME;
			db = mStationsHelper.getReadableDatabase();
			long id = ContentUris.parseId(uri);
			selection = addColumn(RadioDbContract.StationEntry._ID, selection);
			selectionArgs = addArg(id, selectionArgs);
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
				Integer.toString(MainActivity.BUTTON_LIMIT)
			);
		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		log("query uri:" + uri, "i");
		return cursor;
	}
	

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		String table = null;
		SQLiteDatabase db;
		switch (sUriMatcher.match(uri))
		{
		case SEGMENT_STATIONS:
			table = RadioDbContract.StationEntry.TABLE_NAME;
			db = mStationsHelper.getWritableDatabase();
			break;
		case SEGMENT_STATION_ID:
			table = RadioDbContract.StationEntry.TABLE_NAME;
			db = mStationsHelper.getWritableDatabase();
			long id = ContentUris.parseId(uri);
			selection = addColumn(RadioDbContract.StationEntry._ID, selection);
			selectionArgs = addArg(id, selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown query URI:" + uri);
		}
		int updatedCount = db.update(table, values, selection, selectionArgs);
		//notify content resolver of data change
		getContext().getContentResolver().notifyChange(uri, null);
		log("update uri:" + uri + ". " + String.valueOf(updatedCount) + " updated", "i");
		return updatedCount;
		
	}
	
	
	//add id to selection string
	private String addColumn(String column, String selection)
	{
		if (selection == null || selection.isEmpty())
		{
			return column + " = ? ";	
		}
		return selection + " and " + column + " = ?";
	}
	
	//add id to selection args
	//TODO templatize if needed for other types
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
		mLogger.log(getContext(), "RadioContentProvider", "RadioContentProvider:\t\t"+text, level);
	}
	

}
