package com.shinymayhem.radiometadata;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

public class ShoutcastV1 implements Parser {
	
	//protected ActivityLogger mLogger = new ActivityLogger();
	
	@Override
	public boolean parsesUrl(String url) {
		//TODO
		if (url.equals("http://streamplus17.leonex.de:39060"))
		{
			return true;
		}
		return false;
	}
	
	@Override
	public HashMap<String, String> getMetadata(String url)
	{
		
		String info;
		HashMap<String, String> map = new HashMap<String, String>();
		try
		{
			info = this.getInfo(url);
			String artist = info.substring(0, info.indexOf("-"));
			String song = info.substring(info.indexOf("-") + 1);
			map.put(Parser.KEY_ARTIST, artist);
			map.put(Parser.KEY_SONG, song);
		}
		catch (StringIndexOutOfBoundsException e)
		{
			//no "-" found in info string
			log("Not a valid metadata string", "d");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			log("Malformed URL", "d");
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log("IO Exception", "d");
			e.printStackTrace();
		}
		
		return map;
	}

	private String getInfo(String url) throws MalformedURLException, IOException
	{
		String response = "";
		
		URL metadataUrl;
		
		metadataUrl = new URL(url + "/7.html");
		URLConnection connection = metadataUrl.openConnection();
		connection.setRequestProperty("User-Agent", "Mozilla/5.0");

		Reader streamReader = new InputStreamReader(connection.getInputStream());
		StringBuilder stringBuffer = new StringBuilder();
		int ch;
		while (true) {
		    ch = streamReader.read();

		    if (ch < 0)
		        break;

		    stringBuffer.append((char) ch);
		}

		response = stringBuffer.toString();
		log("7.html:" + response, "d");
		
		String info = "";
		Pattern pattern = Pattern.compile("^.*<body>(.*)</body>.*$");
		Matcher matcher;
		matcher = pattern.matcher(response);
		if (matcher.find())
		{
			String content = matcher.group(1).trim();
			String[] fields = content.split(",");
			info = fields[fields.length-1];
			log("content:" + content, "d");	
		}
		else
		{
			log("body pattern not found", "d");
		}
		return info;
	}
	
	public void log(String text, String level)
	{
		Log.d("ShoutcastV1", text);
	}
	
	
}
