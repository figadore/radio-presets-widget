package com.shinymayhem.radiometadata;

import java.util.HashMap;
/**
 * 
 * @author Reese Wilson
 * Interface that all internet radio station metadata parsers should implement.
 *
 */
public interface Parser {
	public static final String KEY_ARTIST = "artist";
	public static final String KEY_SONG = "song";
	
	/**
	 * Check whether the Parser should handle metadata for the URL
	 * 
	 * @param url
	 * @return	Whether the parser reads metadata at the specified URL
	 */
	public boolean parsesUrl(String url);
	
	/**
	 * Get a map of metadata values
	 * 
	 * @param url
	 * @return	hashmap of string keys, as defined in this interface, and their values
	 */
	public HashMap<String, String> getMetadata(String url);
}
