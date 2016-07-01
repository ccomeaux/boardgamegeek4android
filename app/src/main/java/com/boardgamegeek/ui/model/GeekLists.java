package com.boardgamegeek.ui.model;

import com.boardgamegeek.model.GeekListEntry;
import com.boardgamegeek.model.GeekListsResponse;

public class GeekLists extends PaginatedData<GeekListEntry> {
	public GeekLists(GeekListsResponse response, int page) {
		super(response.getGeekListEntries(), response.getTotalCount(), page, GeekListsResponse.PAGE_SIZE);
	}

	public GeekLists(Exception e) {
		super(e);
	}
}
