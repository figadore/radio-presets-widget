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
