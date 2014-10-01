package com.boardgamegeek.model;

public class GeeklistEntry {
	public int id;
	public int thumbs;
	public String title;
	public int entries;
	public String creator;
	public String link;

	@Override
	public String toString() {
		return "" + id + ": " + title;
	}
}
