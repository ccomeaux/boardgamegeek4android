package com.boardgamegeek.ui.loader;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class PaginatedData<T> {
	private List<T> data;
	private String errorMessage;
	private int totalCount;
	private int currentPage;
	private int pageSize;

	public PaginatedData(List<T> data, int totalCount, int page, int pageSize) {
		this.data = data;
		if (this.data == null) {
			this.data = new ArrayList<>();
		}
		errorMessage = "";
		this.totalCount = totalCount;
		currentPage = page;
		this.pageSize = pageSize;
	}

	public PaginatedData(String errorMessage) {
		data = new ArrayList<>();
		updateErrorMessage(errorMessage);
	}

	public PaginatedData(Exception e) {
		updateErrorMessage(e.getMessage());
	}

	public PaginatedData(PaginatedData<T> data) {
		if (data.data == null) {
			this.data = new ArrayList<>();
		} else {
			this.data = new ArrayList<>(data.data);
		}
		this.errorMessage = data.errorMessage;
		this.totalCount = data.totalCount;
		this.currentPage = data.currentPage;
		this.pageSize = data.pageSize;
	}

	protected void updateErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
		totalCount = 0;
		currentPage = 0;
	}

	public void addAll(List<T> threads) {
		data.addAll(threads);
		currentPage++;
	}

	public List<T> getData() {
		return data;
	}

	public int getTotalCount() {
		return totalCount;
	}

	public int getCurrentPage() {
		return currentPage;
	}

	public int getNextPage() {
		return currentPage + 1;
	}

	public int getPageSize() {
		return pageSize;
	}

	public boolean hasMoreResults() {
		return currentPage * getPageSize() < totalCount;
	}

	public boolean hasError() {
		return !TextUtils.isEmpty(errorMessage);
	}

	public String getErrorMessage() {
		return errorMessage;
	}
}
