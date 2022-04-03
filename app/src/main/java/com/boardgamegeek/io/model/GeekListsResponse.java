package com.boardgamegeek.io.model;

import java.util.List;

public class GeekListsResponse {
	public static final int PAGE_SIZE = 12;
	public static final int TOTAL_COUNT = 60; // arbitrary limit to 5 pages
	public List<GeekListEntry> lists;
}
