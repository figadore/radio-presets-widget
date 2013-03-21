package com.shinymayhem.radiopresets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import android.util.Log;

import com.shinymayhem.radiometadata.JazzRadio;
import com.shinymayhem.radiometadata.Parser;
import com.shinymayhem.radiometadata.ShoutcastV1;

public class MetadataParser {
	
	//TODO these should be ordered by most popular first
	private final List<Parser> parsers = new ArrayList<Parser>(Arrays.asList(
			new ShoutcastV1(),
			new JazzRadio()
	));
	protected Parser mParser;
	protected String mUrl;
	
	/**
	 * Iterate through known metadata parsers, checking if they will parse this url, then parsing
	 * Fails silently
	 * @param url
	 * @return hashmap with keys from Parser and their values. empty hashmap on error or no data found
	 */
	public HashMap<String, String> getMetadata(String url)
	{
		//TODO: check a cache first, with url as key
		boolean parses = false;
		HashMap<String, String> map;
		for (Parser parser : parsers)
		{
			if (parser.parsesUrl(url))
			{
				mUrl = url;
				mParser = parser;
				parses = true;
				break;
			}
		}
		if (parses)
		{
			map = mParser.getMetadata(url);
		}
		else
		{
			Log.d("MetadataParser", "No parsers available");
			map = new HashMap<String, String>();
		}
		return map;
	}
	
}
