package com.boardgamegeek.io;

import android.text.TextUtils;

import com.boardgamegeek.model.CollectionItem;

import java.util.List;

public class CollectionResponse {
	private String error;
	private List<CollectionItem> items;

	public CollectionResponse(String error) {
		this.error = error;
	}

	public CollectionResponse(String format, Object... args) {
		this.error = String.format(format, args);
	}

	public CollectionResponse(List<CollectionItem> items) {
		this.items = items;
	}

	public boolean hasError() {
		return !TextUtils.isEmpty(error);
	}

	public String getError() {
		return error;
	}

	public List<CollectionItem> getItems() {
		return items;
	}

	public int getNumberOfItems() {
		return items == null ? 0 : items.size();
	}
}