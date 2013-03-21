package com.shinymayhem.radiometadata;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

public class JazzRadio implements Parser {
	
	private final HashMap<String, String> metadataUrls;
	{
		metadataUrls = new HashMap<String, String>();
		metadataUrls.put("http://jazz-wr14.ice.infomaniak.ch/jazz-wr14-128.mp3", "http://www.jazzradio.fr/winradio/prog18.xml");
		metadataUrls.put("http://jazz-wr04.ice.infomaniak.ch/jazz-wr04-128.mp3", "http://www.jazzradio.fr/winradio/prog15.xml");
	}

	@Override
	public boolean parsesUrl(String url) {
		if (metadataUrls.containsKey(url))
		{
			return true;
		}
		return false;
	}
	
	private InputStream getXml(String url) throws MalformedURLException, IOException, IllegalArgumentException
	{
		URL metadataUrl;
		if (!metadataUrls.containsKey(url))
		{
			throw new IllegalArgumentException("Url does not exist in the list of parseable metadata URLs");
		}
		metadataUrl = new URL(metadataUrls.get(url));
		HttpURLConnection conn = (HttpURLConnection) metadataUrl.openConnection();
	    conn.setReadTimeout(10000 /* milliseconds */);
	    conn.setConnectTimeout(15000 /* milliseconds */);
	    conn.setRequestMethod("GET");
	    conn.setDoInput(true);
	    // Starts the query
	    conn.connect();
	    return conn.getInputStream();
	}

	@Override
	public HashMap<String, String> getMetadata(String url) {
		HashMap<String, String> map = new HashMap<String, String>();
		try {
			InputStream stream = this.getXml(url);
			XmlParser xmlParser = new XmlParser();
			
			String[] tagArray = {"chanteur", "chanson"};
			List<String> tags = Arrays.asList(tagArray);
			
			HashMap<String, String> tagMap = xmlParser.getFirstStringsForTags("prog", tags, stream);
			
			//put values in map if the keys exist
			if (tagMap.containsKey("chanteur"))
			{
				map.put(KEY_ARTIST, tagMap.get("chanteur"));
			}
			if (tagMap.containsKey("chanson"))
			{
				map.put(KEY_SONG, tagMap.get("chanson"));
			}
		} catch (MalformedURLException e) {
			//fail silently
			e.printStackTrace();
		} catch (IOException e) {
			//fail silently
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			//fail silently
			e.printStackTrace();
		}
		return map;
	}

}
