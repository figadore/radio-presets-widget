package com.shinymayhem.radiometadata;


public class ShoutcastV1 implements Parser {

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
	public String getArtist(String url) {
		// TODO Auto-generated method stub
		return "shoutcast artist";
		/*String str = "";
		URL url;
		try
		{
			url = new URL(urls[0] + "/7.html");
			URLConnection con2 = url.openConnection();
			con2.setRequestProperty("User-Agent", "Mozilla/5.0"); // This bugger right here saved the day!

			Reader r = new InputStreamReader(con2.getInputStream());
			StringBuilder buf = new StringBuilder();

			while (true) {
			    int ch = r.read();

			    if (ch < 0)
			        break;

			    buf.append((char) ch);
			}

			str = buf.toString();
			log("html:" + str, "d");
			
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		XmlParser parser = new XmlParser(ServiceRadioPlayer.this);
		try {
			String result = parser.getStringFromTag("body", str);
			log("result:" + result, "i");
		} catch (IOException e) {
			log("IOException", "e");
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			log("XmlPullParserException", "e");
			e.printStackTrace();
		}
		return str;*/
		/*
		 Map<String, String> metadata = new HashMap<String, String>();
		 String[] metaParts = result.split(",");
	        Pattern p = Pattern.compile("^([a-zA-Z]+)=\\'(.*)\\'$"); //match pattern <characters>='<any>'
	        Matcher m;
	        for (int i = 0; i < metaParts.length; i++) {
	            m = p.matcher(metaParts[i]);
	            if (m.find()) {
	            	String key = ((String) m.group(1)).trim();
	            	String value = ((String) m.group(2)).trim();
	                metadata.put(key, value);
	            }
	        }*/
		/*try {
			String artist = result.getArtist();
			log("artist:" + artist, "d");
			String streamTitle = result.getStreamTitle();
			log("stream title:" + streamTitle, "v");
			String song = result.getTitle();
			log("song:" + song, "d");
			if (!artist.equals(mArtist) || !song.equals(mSong)) //only update visible metadata if different
			{
				mArtist = artist;
				mSong = song;
				updateDetails();	
			}
			
		} catch (StringIndexOutOfBoundsException e)
		{
			log("no metadata available", "d");
			mArtist = "";
			mSong = "";
			updateDetails();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
	}

	@Override
	public String getSong(String url) {
		// TODO Auto-generated method stub
		return "shoutcast song";
	}

}
