package com.radiopirate.android.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

/**
 * Get the Title of a stream using the Shoutcast Metadata Protocol From:
 * http://uniqueculture.net/2010/11/stream-metadata-plain-java/
 */
public class IcyStreamMeta {

    private static final String TAG = "RP - IcyStreamMeta";
    private URL streamUrl;
    private Map<String, String> metadata;
    private boolean isError;

    public IcyStreamMeta(URL streamUrl) {
        setStreamUrl(streamUrl);

        isError = false;
    }

    public URL getStreamUrl() {
        return streamUrl;
    }

    /**
     * Get stream title
     * 
     * @return String
     * @throws IOException
     */
    public String getStreamTitle() throws IOException {
        Map<String, String> data = getMetadata();

        if (data == null) {
            return "";
        }

        if (!data.containsKey("StreamTitle"))
            return "";

        return data.get("StreamTitle").trim();
    }

    /**
     * Get artist using stream's title
     * 
     * @return String
     * @throws IOException
     */
    public String getArtist() throws IOException, StringIndexOutOfBoundsException  {
        Map<String, String> data = getMetadata();
        if (data == null || !data.containsKey("StreamTitle"))
        {
            return "";
        }
        String streamTitle = data.get("StreamTitle");
        String title = streamTitle.substring(0, streamTitle.indexOf("-"));
        return title.trim();
    }

    /**
     * Get title using stream's title
     * 
     * @return String
     * @throws IOException
     */
    public String getTitle() throws IOException, StringIndexOutOfBoundsException {
        Map<String, String> data = getMetadata();

        if (data == null || !data.containsKey("StreamTitle"))
        {
            return "";
        }

        String streamTitle = data.get("StreamTitle");
        String artist = streamTitle.substring(streamTitle.indexOf("-") + 1);
        return artist.trim();
    }

    public void refreshMeta() throws IOException {
        retreiveMetadata();
    }

    private Map<String, String> getMetadata() throws IOException {
        if (metadata == null) {
            refreshMeta();
        }

        return metadata;
    }

    private void retreiveMetadata() throws IOException {
        URLConnection con = streamUrl.openConnection();
        con.setRequestProperty("Icy-MetaData", "1");
        con.setRequestProperty("Connection", "close");
        con.connect();

        int metaDataOffset = 0;
        Map<String, List<String>> headers = con.getHeaderFields();
        InputStream stream = con.getInputStream();

        if (headers.containsKey("icy-metaint")) {
            // Headers are sent via HTTP
            metaDataOffset = Integer.parseInt(headers.get("icy-metaint").get(0));
        }
        // else: There would be a way to get it from the stream content, but it is inefficient, and unneeded for our
        // streams

        // In case no data was sent
        if (metaDataOffset == 0) {
            isError = true;
            Log.e(TAG, "retreiveMetadata - no offset");
            return;
        }

        // Read metadata
        int b;
        int count = 0;
        int metaDataLength = 4080; // 4080 is the max length
        boolean inData = false;
        StringBuilder metaData = new StringBuilder();
        // Stream position should be either at the beginning or right after headers
        while ((b = stream.read()) != -1) {
            count++;

            // Length of the metadata
            if (count == metaDataOffset + 1) {
                metaDataLength = b * 16;
            }

            if (count > metaDataOffset + 1 && count < (metaDataOffset + metaDataLength)) {
                inData = true;
            } else {
                inData = false;
            }
            if (inData) {
                if (b != 0) {
                    metaData.append((char) b);
                }
            }
            if (count > (metaDataOffset + metaDataLength)) {
                break;
            }

        }

        // Set the data
        metadata = IcyStreamMeta.parseMetadata(metaData.toString());

        // Close
        stream.close();
    }

    public boolean isError() {
        return isError;
    }

    private void setStreamUrl(URL streamUrl) {
        this.metadata = null;
        this.streamUrl = streamUrl;
        this.isError = false;
    }

    public static Map<String, String> parseMetadata(String metaString) {
        Map<String, String> metadata = new HashMap();

        String[] metaParts = metaString.split(";");
        for (int i = 0; i < metaParts.length; i++) {
            String part = metaParts[i].substring(0, metaParts[i].indexOf("="));
            String songname = metaParts[i].substring(metaParts[i].indexOf("'")+1, metaParts[i].lastIndexOf("'"));
            metadata.put(part, songname);
        }

        return metadata;
    }
}
