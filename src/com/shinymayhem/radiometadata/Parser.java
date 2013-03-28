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
     * @param url Streaming media url
     * @return  Whether the parser reads metadata at the specified URL
     */
    public boolean parsesUrl(String url);
    
    /**
     * Get a map of metadata values
     * 
     * @param url
     * @return  hashmap of string keys, as defined in this interface, and their values
     */
    public HashMap<String, String> getMetadata(String url);
}
