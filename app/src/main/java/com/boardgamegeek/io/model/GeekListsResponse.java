package com.boardgamegeek.io.model;

import java.util.List;

public class GeekListsResponse {
	public static final int PAGE_SIZE = 48; // the hot list is limited to 48 results
	public static final int TOTAL_COUNT = 240; // arbitrary limit to 5 pages
	public List<GeekListEntry> lists;
}
