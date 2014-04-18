package com.boardgamegeek.ui;

import java.util.ArrayList;
import java.util.List;

import android.text.TextUtils;

public class PaginatedData<D> {
	private List<D> mData;
	private String mErrorMessage;
	private int mTotalCount;
	private int mCurrentPage;

	public PaginatedData(List<D> threads, int totalCount, int page) {
		mData = threads;
		mErrorMessage = "";
		mTotalCount = totalCount;
		mCurrentPage = page;
	}

	public PaginatedData(String errorMessage) {
		mData = new ArrayList<D>();
		mErrorMessage = errorMessage;
		mTotalCount = 0;
		mCurrentPage = 0;
	}

	public PaginatedData(PaginatedData<D> data) {
		this.mData = new ArrayList<D>(data.mData);
		this.mErrorMessage = data.mErrorMessage;
		this.mTotalCount = data.mTotalCount;
		this.mCurrentPage = data.mCurrentPage;
	}

	public void addAll(List<D> threads) {
		mData.addAll(threads);
		mCurrentPage++;
	}

	public List<D> getData() {
		return mData;
	}

	public int getTotalCount() {
		return mTotalCount;
	}

	public int getCurrentPage() {
		return mCurrentPage;
	}

	public int getNextPage() {
		return mCurrentPage + 1;
	}

	private int getPageSize() {
		return 50;
	}

	public boolean hasMoreResults() {
		return mCurrentPage * getPageSize() < mTotalCount;
	}

	public boolean hasError() {
		return !TextUtils.isEmpty(mErrorMessage);
	}

	public String getErrorMessage() {
		return mErrorMessage;
	}
}
