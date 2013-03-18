package com.shinymayhem.radiometadata;

public interface Parser {
	public boolean parsesUrl(String url);
	public String getArtist(String url);
	public String getSong(String url);
}
