package com.boardgamegeek.ui.loader;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class PaginatedData<T> {
	private List<T> items;
	private int totalItemCount;
	private int currentPageNumber;
	private int pageSize;
	private String errorMessage;

	public PaginatedData(List<T> items, int totalItemCount, int currentPageNumber, int pageSize) {
		this.items = items;
		if (this.items == null) {
			this.items = new ArrayList<>();
		}
		this.totalItemCount = totalItemCount;
		this.currentPageNumber = currentPageNumber;
		this.pageSize = pageSize;
		errorMessage = "";
	}

	public PaginatedData(Exception e) {
		errorMessage = e.getLocalizedMessage();
	}

	public PaginatedData(PaginatedData<T> data) {
		if (data.items == null) {
			this.items = new ArrayList<>();
		} else {
			this.items = new ArrayList<>(data.items);
		}
		this.totalItemCount = data.totalItemCount;
		this.currentPageNumber = data.currentPageNumber;
		this.pageSize = data.pageSize;
		this.errorMessage = data.errorMessage;
	}

	public void addPage(List<T> items) {
		this.items.addAll(items);
		currentPageNumber++;
	}

	public void clear() {
		items.clear();
		currentPageNumber = 0;
		totalItemCount = 0;
	}

	public List<T> getItems() {
		return items;
	}

	public int getCurrentPageNumber() {
		return currentPageNumber;
	}

	public int getNextPageNumber() {
		return currentPageNumber + 1;
	}

	public boolean hasMoreResults() {
		return currentPageNumber * pageSize < totalItemCount;
	}

	public boolean hasError() {
		return !TextUtils.isEmpty(errorMessage);
	}

	public String getErrorMessage() {
		return errorMessage;
	}
}
