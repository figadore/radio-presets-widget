package com.shinymayhem.radiopresets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.shinymayhem.radiometadata.Parser;
import com.shinymayhem.radiometadata.ShoutcastV1;

public class MetadataParser {
	
	private final List<Parser> parsers = new ArrayList<Parser>(Arrays.asList(
			new ShoutcastV1()
	));
	protected Parser mParser;
	protected String mUrl;
	
	public boolean setUrl(String url)
	{
		boolean parses = false;
		for (Parser parser : parsers)
		{
			if (parser.parsesUrl(url))
			{
				//TODO
				mUrl = url;
				mParser = parser;
				parses = true;
				break;
			}
		}
		return parses;
	}
	
	public String getArtist()
	{
		return mParser.getArtist(mUrl);
	}
	
	public String getSong()
	{
		return mParser.getSong(mUrl);
	}
}
