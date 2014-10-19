package com.boardgamegeek.ui.widget;

import java.util.ArrayList;
import java.util.List;

import retrofit.RetrofitError;
import android.text.TextUtils;

public class PaginatedData<T> {
	private List<T> mData;
	private String mErrorMessage;
	private int mTotalCount;
	private int mCurrentPage;
	private int mPageSize;

	public PaginatedData(List<T> data, int totalCount, int page, int pageSize) {
		mData = data;
		if (mData == null) {
			mData = new ArrayList<T>();
		}
		mErrorMessage = "";
		mTotalCount = totalCount;
		mCurrentPage = page;
		mPageSize = pageSize;
	}

	public PaginatedData(String errorMessage) {
		mData = new ArrayList<T>();
		updateErrorMessage(errorMessage);
	}

	public PaginatedData(Exception e) {
		updateErrorMessage(e.getMessage());
		if (e instanceof RetrofitError) {
			RetrofitError re = (RetrofitError) e;
			if (re.isNetworkError() && re.getResponse() == null) {
				updateErrorMessage("Looks like you're offline.");
			}
		}
	}

	public PaginatedData(PaginatedData<T> data) {
		if (data.mData == null) {
			this.mData = new ArrayList<T>();
		} else {
			this.mData = new ArrayList<T>(data.mData);
		}
		this.mErrorMessage = data.mErrorMessage;
		this.mTotalCount = data.mTotalCount;
		this.mCurrentPage = data.mCurrentPage;
	}

	protected void updateErrorMessage(String errorMessage) {
		mErrorMessage = errorMessage;
		mTotalCount = 0;
		mCurrentPage = 0;
	}

	public void addAll(List<T> threads) {
		mData.addAll(threads);
		mCurrentPage++;
	}

	public List<T> getData() {
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

	public int getPageSize() {
		return mPageSize;
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
