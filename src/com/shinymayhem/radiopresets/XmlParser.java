package com.shinymayhem.radiopresets;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.util.Log;
import android.util.Xml;

public class XmlParser {
	//TODO FIXME NOTE: this class was only partially written, never tested or used. http://developer.android.com/training/basics/network-ops/xml.html
	protected ActivityLogger mLogger = new ActivityLogger();
	protected Context mContext;
	
	public XmlParser(Context context)
	{
		mContext = context;
	}

	public String getStringFromTag(String tag, String in) throws IOException, XmlPullParserException
	{
		InputStream stream = new ByteArrayInputStream(in.getBytes());
		return this.parse(tag, stream);
	}
	
	public String parse(String tag, InputStream in) throws IOException, XmlPullParserException 
	{
		List<String> entries = new ArrayList<String>();
		try {
			XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            parser.require(XmlPullParser.START_TAG, null, "feed");
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String name = parser.getName();
                // Starts by looking for the entry tag
                if (name.equals("entry")) {
                    entries.add(readEntry(parser));
                } else {
                    skip(parser);
                }
            }  
		}
		finally
		{
			in.close();
		}
		return "";
	}
	

	private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
	    if (parser.getEventType() != XmlPullParser.START_TAG) {
	        throw new IllegalStateException();
	    }
	    int depth = 1;
	    log("skipping " + readText(parser), "v");
	    while (depth != 0) {
	        switch (parser.next()) {
	        case XmlPullParser.END_TAG:
	            depth--;
	            break;
	        case XmlPullParser.START_TAG:
	            depth++;
	            break;
	        }
	    }
	 }
	
	private String readEntry(XmlPullParser parser) throws IOException, XmlPullParserException
	{
		return readText(parser);
		//return "";
	}
	
	// For the tags title and summary, extracts their text values.
	private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
	    String result = "";
	    if (parser.next() == XmlPullParser.TEXT) {
	        result = parser.getText();
	        parser.nextTag();
	    }
	    return result;
	}
	
	private void log(String text, String level)
	{
		Log.d("XmlParser", text);
		//mLogger.log(mContext, text, level);
	}
	
	
	
}
