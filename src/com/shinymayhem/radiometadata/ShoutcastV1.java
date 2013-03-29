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
package com.shinymayhem.radiometadata;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

public class ShoutcastV1 implements Parser {
    public static final boolean LOCAL_LOGV = true;
    public static final boolean LOCAL_LOGD = true;
    private static final String TAG = "ShoutcastV1";
    //protected ActivityLogger mLogger = new ActivityLogger();
    
    @Override
    public boolean parsesUrl(String url) {
        
        try {
            URL metadataUrl = new URL(url + "/7.html");
            HttpURLConnection urlc = (HttpURLConnection) metadataUrl.openConnection();
            urlc.setRequestProperty("User-Agent", "Mozilla/5.0");
            urlc.setRequestProperty("Connection", "close");
            urlc.setConnectTimeout(30 * 1000); // Thirty seconds timeout in milliseconds
            urlc.connect();
            if (urlc.getResponseCode() == 200) { // Good response
                return true;
            }
        } catch (IOException e) {
            //fail silently
        }
//      if (url.equals("http://streamplus17.leonex.de:39060"))
//      {
//          return true;
//      }
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
            map.put(Parser.KEY_SONG, info); //default song to whole info string if no dash
            String artist = info.substring(0, info.indexOf("-"));
            String song = info.substring(info.indexOf("-") + 1);
            map.put(Parser.KEY_ARTIST, artist.trim());
            map.put(Parser.KEY_SONG, song.trim());
        }
        catch (StringIndexOutOfBoundsException e)
        {
            //no "-" found in info string
            
            if (LOCAL_LOGD) Log.d(TAG, "No dash found in metadata string");
        } catch (MalformedURLException e) {
            Log.i(TAG, "Malformed URL: " + url);
        } catch (IOException e) {
            Log.i(TAG, "IO Exception: " + url);
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
        if (LOCAL_LOGD) Log.d(TAG, "7.html:" + response);
        
        String info = "";
        Pattern pattern = Pattern.compile("^.*<body>(.*)</body>.*$");
        Matcher matcher;
        matcher = pattern.matcher(response);
        if (matcher.find())
        {
            String content = matcher.group(1).trim();
            String[] fields = content.split(",", 7);
            info = fields[fields.length-1];
            if (LOCAL_LOGV) Log.v(TAG, "content:" + content); 
        }
        else
        {
            if (LOCAL_LOGV) Log.v(TAG, "body pattern not found");
        }
        return info;
    }
    
    
    
}
