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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpException;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;
import android.webkit.URLUtil;

public class ServiceAudioFormat extends IntentService {
    
    private enum AudioType {MP3, M3U, PLS, XSPF, AAC, AACP, UNKNOWN};
    protected ActivityLogger mLogger = new ActivityLogger();
    protected InputStream mStream;
    
    public ServiceAudioFormat() {
        super("ServiceAudioFormatName");
    }
    
    @Override
    protected void onHandleIntent(Intent intent)
    {
        log("onHandleIntent()", "v");
        String url = intent.getStringExtra(ServiceRadioPlayer.EXTRA_URL);
        Intent updateIntent = new Intent(this, ServiceRadioPlayer.class);
        try {
            AudioType type = this.processUrl(url);
            String newUrl = this.getUrlByType(type, url);
            updateIntent.setAction(ServiceRadioPlayer.ACTION_PLAY_STREAM);
            updateIntent.putExtra(ServiceRadioPlayer.EXTRA_URL, newUrl);
            boolean updateUrl;
            //update player url if it was a playlist and the audio stream url was resolved
            //don't update if url was already a streaming media url 
            //TODO handle the case where metadata is in the playlist 
            switch (type)
            {
                case M3U:
                case PLS:
                case XSPF:
                    updateUrl = true;
                    break;
                case AAC:
                case AACP:
                    updateIntent.setAction(ServiceRadioPlayer.ACTION_UNSUPPORTED_FORMAT_ERROR);
                default:
                    updateUrl = false;
            }
            updateIntent.putExtra(ServiceRadioPlayer.EXTRA_FORMAT, type.toString());
            updateIntent.putExtra(ServiceRadioPlayer.EXTRA_UPDATE_URL, updateUrl);
        } catch (StreamHttpException e) {
            updateIntent.setAction(ServiceRadioPlayer.ACTION_STREAM_ERROR);
            updateIntent.putExtra(ServiceRadioPlayer.EXTRA_RESPONSE_CODE, e.getResponseCode());
            updateIntent.putExtra(ServiceRadioPlayer.EXTRA_RESPONSE_MESSAGE, e.getResponseMessage());
        } catch (MalformedURLException e) {
            updateIntent.setAction(ServiceRadioPlayer.ACTION_FORMAT_ERROR);
            updateIntent.putExtra(ServiceRadioPlayer.EXTRA_ERROR_MESSAGE, getResources().getString(R.string.error_url));
        } catch (IOException e) {
            updateIntent.setAction(ServiceRadioPlayer.ACTION_FORMAT_ERROR);
            updateIntent.putExtra(ServiceRadioPlayer.EXTRA_ERROR_MESSAGE, getResources().getString(R.string.error_unknown));
            log("IOException", "d");
            e.printStackTrace();
        }
        
        startService(updateIntent);
        stopSelf();
        
    }

    protected void handleHttpResponse(int responseCode, String message) throws IOException, StreamHttpException
    {
        //String message = con.getResponseMessage();
        log("Server response code:" + String.valueOf(responseCode) + ", message:" +  message, "v");
        if (responseCode < 200 || responseCode >= 300)
        {
            throw new StreamHttpException(responseCode, message);   
        }
        
    }
    
    protected AudioType processUrl(String url) throws StreamHttpException, IOException {
        //TODO make recursive, if playlist within playlist
        //TODO look into checking response headers anyway
        //check url first, it is fastest, no network connections needed
        AudioType type = getTypeFromString(url);
        //now try to handle cases where type could not be determined by url
        if (type.equals(AudioType.UNKNOWN))
        {
            //URL streamUrl;
            /*streamUrl = new URL(url);

            HttpURLConnection con;
            con = (HttpURLConnection)streamUrl.openConnection();
            con.setRequestProperty("Connection", "close");
            con.connect();
            */
            
            Map<String, List<String>> headers = this.getInputStream(url); //con.getInputStream();
            
            //InputStream stream = con.getInputStream();
            if (headers.containsKey("Content-Type")) {
                // Headers are sent via HTTP
                List<String> contentTypes = headers.get("Content-Type");
                for (String contentType : contentTypes)
                {
                    log("contentType:" + contentType, "d");
                    type = this.getTypeFromContentType(contentType);
                    if (!type.equals(AudioType.UNKNOWN)) //found type
                    {
                        break;
                    }
                }
                
            }
            else
            {
                log("no content type", "d");
            }
            //if it still can't find it, try one last time in the disposition
            if (type.equals(AudioType.UNKNOWN) && headers.containsKey("Content-Disposition"))
            {
                List<String> dispositions = headers.get("Content-Disposition");
                for (String disposition : dispositions)
                {
                    log("disposition:" + disposition, "d");
                    type = this.getTypeFromString(disposition);
                    if (!type.equals(AudioType.UNKNOWN)) //found type
                    {
                        break;
                    }
                }
            }
            mStream.close();

            
        }
        return type;
    }
    
    private AudioType getTypeFromString(String string)
    {
        AudioType type = AudioType.UNKNOWN;
        if (string.contains(".mp3"))
        {
            type = AudioType.MP3;
        }
        else if (string.contains(".m3u"))
        {
            type = AudioType.M3U;
        }
        else if (string.contains(".pls"))
        {
            type = AudioType.PLS;
        }
        else if (string.contains(".xspf"))
        {
            type = AudioType.XSPF;
        }
        else if (string.contains(".aac"))
        {
            type = AudioType.AAC;
        }
        else if (string.contains(".aacp"))
        {
            type = AudioType.AACP;
        }
        return type;
    }
    
    private AudioType getTypeFromContentType(String contentType)
    {
        AudioType type = AudioType.UNKNOWN;
        if (contentType.contains("audio/x-scpls") || contentType.contains("audio/x-scpls"))
        {
            type = AudioType.PLS;
        }
        else if (contentType.contains("audio/mpegurl") || contentType.contains("audio/x-mpegurl"))
        {
            type = AudioType.M3U;
        }
        else if (contentType.contains("application/xspf+xml") || contentType.contains("audio/x-mpegurl"))
        {
            type = AudioType.XSPF;
        }
        else if (contentType.equals("audio/mpeg"))
        {
            type = AudioType.MP3;
        }
        else if (contentType.equals("audio/aac"))
        {
            type = AudioType.AAC;
        }
        else if (contentType.equals("audio/aacp"))
        {
            type = AudioType.AACP;
        }       
        return type;
    }
    
    private String getUrlByType(AudioType type, String url) throws IOException, StreamHttpException
    {
        log("getting url by type:" + type.toString(), "d");
        String newUrl = url;
        switch(type)
        {
        case UNKNOWN:
            break;
        case AAC:
            break;
        case AACP:
            break;
        case M3U:
            newUrl = this.getUrlFromM3u(url);
            break;
        case MP3:
            //already a stream
            break;
        case PLS:
            newUrl = this.getUrlFromPls(url);
            break;
        case XSPF:
            newUrl = this.getUrlFromXspf(url);
            break;
        default:
            break;
            
        }
        return newUrl;
    }
    
    private String getUrlFromM3u(String url) throws IOException, StreamHttpException
    {
        log("getUrlFromM3u()", "d");
        if (mStream == null)
        {
            getInputStream(url);
        }
        String newUrl = url;
        BufferedReader reader = new BufferedReader(new InputStreamReader(mStream));
        String line;
        while ((line = reader.readLine()) != null)
        {
            log("read line:" + line, "d");
            if (!line.startsWith("#") && URLUtil.isHttpUrl(line) || URLUtil.isHttpsUrl(line))
            {
                newUrl = line;
                break;
            }
        }
        return newUrl;
    }
    
    private String getUrlFromPls(String url) throws IOException, StreamHttpException
    {
        log("getUrlFromPls()", "d");
        if (mStream == null)
        {
            getInputStream(url);
        }
        String newUrl = url;
        BufferedReader reader = new BufferedReader(new InputStreamReader(mStream));
        String line;
        while ((line = reader.readLine()) != null)
        {
            log("read line:" + line, "d");
            if (line.startsWith("File"))
            {
                newUrl = line.substring(line.indexOf("=") + 1);
                break;
            }
        }
        return newUrl;
    }
    
    private String getUrlFromXspf(String url) throws IOException, StreamHttpException
    {
        log("getUrlFromXspf()", "d");
        if (mStream == null)
        {
            getInputStream(url);
        }
        String newUrl = url;
        BufferedReader reader = new BufferedReader(new InputStreamReader(mStream));
        String line;
        while ((line = reader.readLine()) != null)
        {
            log("read line:" + line, "d");
        }
        return newUrl;
    }
    
    private Map<String, List<String>> getInputStream(String url) throws IOException, StreamHttpException
    {
        /*
        URL streamUrl = new URL(url);
        StreamURLConnection con;
        con = (StreamURLConnection)streamUrl.openConnection();
        con.setRequestProperty("Connection", "close");
        con.connect();
        int responseCode = con.getResponseCode(); 
        if (responseCode < 200 || responseCode >= 300)
        {
            this.handleHttpError(con);
        }*/
        URL streamUrl = new URL(url);
        HttpURLConnection con;
        con = (HttpURLConnection)streamUrl.openConnection();
        con.setRequestProperty("Connection", "close");
        con.connect();
        Map<String, List<String>> headers = con.getHeaderFields();
        String statusLine = headers.get(null).get(0); //con.getHeaderField(0);
        //String statusLine2 = con.getHeaderField(0);
        String message = statusLine;
        int responseCode = -1;
        if (statusLine.startsWith("HTTP/1.") || statusLine.startsWith("ICY")) {
            int codePos = statusLine.indexOf(' ');
            if (codePos > 0) {

                int phrasePos = statusLine.indexOf(' ', codePos+1);
                if (phrasePos > 0 && phrasePos < statusLine.length()) {
                    message = statusLine.substring(phrasePos+1);
                }

                if (phrasePos < 0)
                    phrasePos = statusLine.length();

                try {
                    responseCode = Integer.parseInt(statusLine.substring(codePos+1, phrasePos));
                } catch (NumberFormatException e) { }
            }
        }
         
        this.handleHttpResponse(responseCode, message);
        mStream = con.getInputStream();
        return headers;
    }
    
    private void log(String text, String level)
    {
        mLogger.log(this, text, level);
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }
    
    private class StreamHttpException extends HttpException
    {
        private static final long serialVersionUID = 2499906481918509156L;
        private int mResponseCode;
        private String mResponseMessage;
        public StreamHttpException(int responseCode, String responseMessage)
        {
            this.mResponseCode=responseCode;
            this.mResponseMessage=responseMessage;
        }
        public int getResponseCode()
        {
            return mResponseCode;
        }
        public String getResponseMessage()
        {
            return mResponseMessage;
        }
    }
    
}
