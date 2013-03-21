package com.shinymayhem.radiometadata;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Xml;

public class XmlParser {
	//TODO FIXME NOTE: this class was only partially written, never tested or used. http://developer.android.com/training/basics/network-ops/xml.html
	//protected ActivityLogger mLogger = new ActivityLogger();

	/**
	 * Get a list of the contents of all instances of the specified tags list
	 * @param root Root of XML
	 * @param tags List of tags that are being searched for
	 * @param in XML string that is being searched
	 * @return list of contents within all instances of the specified tags
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	public HashMap<String, String> getFirstStringsForTags(String root, List<String> tags, String in) throws IOException, XmlPullParserException
	{
		InputStream stream = new ByteArrayInputStream(in.getBytes());
		return this.getFirstStringsForTags(root, tags, stream);
	}
	
	/**
	 * Get a list of the contents of all instances of the specified tags list
	 * @param root Root of XML
	 * @param tags List of tags that are being searched for
	 * @param in XML InputStream that is being searched
	 * @return list of contents within all instances of the specified tags
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	public HashMap<String, String> getFirstStringsForTags(String root, List<String> tags, InputStream in) throws IOException, XmlPullParserException
	{
		HashMap<String, String> results = new HashMap<String, String>();
		int find = tags.size();
		int found = 0;
		try {
			//initialize XML parser
			XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            
            //make sure we are starting with a start tag
            parser.require(XmlPullParser.START_TAG, null, root);
            //loop through each tag in the document, returning when we each tag has a value
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String name = parser.getName();
                //if this is one of the tags we were looking for, add it if it doesn't exist yet
                if (tags.contains(name) && !results.containsKey(name)) {
                	String text = readText(parser); 
                    results.put(name, text);
                    found++;
                } 
                if (found == find) //we are done looking
                {
                	break;
                }
            }  
		}
		finally
		{
			in.close();
		}
		return results;
	}
	
	private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
	    String result = "";
	    if (parser.next() == XmlPullParser.TEXT) {
	        result = parser.getText();
	        parser.nextTag();
	    }
	    return result;
	}
	
//	not used
//	/**
//	 * Get a list of the contents of all instances of the specified tags list
//	 * @param root Root of XML
//	 * @param tags List of tags that are being searched for
//	 * @param in XML string that is being searched
//	 * @return map of contents within last instances of the specified tags
//	 * @throws IOException
//	 * @throws XmlPullParserException
//	 */
//	public HashMap<String, String> getLastStringsForTags(String root, List<String> tags, String in) throws IOException, XmlPullParserException
//	{
//		InputStream stream = new ByteArrayInputStream(in.getBytes());
//		return this.getLastStringsForTags(root, tags, stream);
//	}
//	
//	/**
//	 * Get a list of the contents of all instances of the specified tags list
//	 * @param root Root of XML
//	 * @param tags List of tags that are being searched for
//	 * @param in XML InputStream that is being searched
//	 * @return map of contents within last instances of the specified tags
//	 * @throws IOException
//	 * @throws XmlPullParserException
//	 */
//	public HashMap<String, String> getLastStringsForTags(String root, List<String> tags, InputStream in) throws IOException, XmlPullParserException
//	{
//		HashMap<String, String> results = new HashMap<String, String>();
//		try {
//			XmlPullParser parser = Xml.newPullParser();
//            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
//            parser.setInput(in, null);
//            parser.nextTag();
//            
//            parser.require(XmlPullParser.START_TAG, null, root);
//            while (parser.next() != XmlPullParser.END_DOCUMENT) {
//                if (parser.getEventType() != XmlPullParser.START_TAG) {
//                    continue;
//                }
//                String name = parser.getName();
//                // Starts by looking for the specified tag
//                if (tags.contains(name)) {
//                	String text = readText(parser); 
//                    results.put(name, text);
//                } else {
//                    //skip(parser);
//                }
//            }  
//		}
//		finally
//		{
//			in.close();
//		}
//		return results;
//	}
//	
//	/**
//	 * Get a list of the contents of all instances of the specified tag
//	 * @param root Root of XML
//	 * @param tag Tag that is being searched for
//	 * @param in XML string that is being searched
//	 * @return list of contents within all instances of the specified tag
//	 * @throws IOException
//	 * @throws XmlPullParserException
//	 */
//	public List<String> getStringsForTag(String root, String tag, String in) throws IOException, XmlPullParserException
//	{
//		InputStream stream = new ByteArrayInputStream(in.getBytes());
//		return this.getStringsForTag(root, tag, stream);
//	}
//	
//	/**
//	 * Get a list of the contents of all instances of the specified tag
//	 * @param root Root of XML
//	 * @param tag Tag that is being searched for
//	 * @param in XML InputStream that is being searched
//	 * @return list of contents within all instances of the specified tag
//	 * @throws IOException
//	 * @throws XmlPullParserException
//	 */
//	public List<String> getStringsForTag(String root, String tag, InputStream in) throws IOException, XmlPullParserException 
//	{
//		List<String> results = new ArrayList<String>();
//		try {
//			XmlPullParser parser = Xml.newPullParser();
//            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
//            parser.setInput(in, null);
//            parser.nextTag();
//            
//            parser.require(XmlPullParser.START_TAG, null, root);
//            while (parser.next() != XmlPullParser.END_DOCUMENT) {
//                if (parser.getEventType() != XmlPullParser.START_TAG) {
//                    continue;
//                }
//                String name = parser.getName();
//                // Starts by looking for the specified tag
//                if (name.equals(tag)) {
//                	String text = readText(parser); 
//                    results.add(text);
//                } else {
//                    //skip(parser);
//                }
//            }  
//		}
//		finally
//		{
//			in.close();
//		}
//		return results;
//	}
//	
//
//	private void skipCurrentTag(XmlPullParser parser) throws XmlPullParserException, IOException {
//	    if (parser.getEventType() != XmlPullParser.START_TAG) {
//	        throw new IllegalStateException();
//	    }
//	    int depth = 1;
//	    log("skipping " + parser.getName(), "v");
//	    while (depth != 0) {
//	    	int result = parser.next();
//	    	String name = parser.getName();
//	    	log ("subskipping " + name, "d");
//	        switch (result) {
//	        case XmlPullParser.END_TAG:
//	            depth--;
//	            break;
//	        case XmlPullParser.START_TAG:
//	            depth++;
//	            break;
//	        default:
//	            log("not start or end tag", "d");	
//	        }
//	    }
//	}

//	private void log(String text, String level)
//	{
//		Log.d("XmlParser", text);
//		//mLogger.log(mContext, text, level);
//	}
	
	
	
}
