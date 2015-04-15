package com.boardgamegeek.ui.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.ui.loader.PaginatedData;

public abstract class PaginatedArrayAdapter<T> extends ArrayAdapter<T> {
	private static final int VIEW_TYPE_ITEM = 0;
	private static final int VIEW_TYPE_LOADING = 1;
	private int mResource;
	private String mErrorMessage;
	private int mTotalCount;
	private int mCurrentPage;
	private int mPageSize;

	public PaginatedArrayAdapter(Context context, int resource, PaginatedData<T> data) {
		super(context, resource, data.getData());
		mResource = resource;
		mErrorMessage = data.getErrorMessage();
		mTotalCount = data.getTotalCount();
		mCurrentPage = data.getCurrentPage();
		mPageSize = data.getPageSize();
	}

	public void update(PaginatedData<T> data) {
		clear();
		mCurrentPage = data.getCurrentPage();
		for (T datum : data.getData()) {
			add(datum);
		}
	}

	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	@Override
	public boolean isEnabled(int position) {
		return getItemViewType(position) == VIEW_TYPE_ITEM;
	}

	@Override
	public int getViewTypeCount() {
		return 2;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public int getCount() {
		return super.getCount()
			+ (((isLoaderLoading() && super.getCount() == 0) || hasMoreResults() || hasError()) ? 1 : 0);
	}

	protected abstract boolean isLoaderLoading();

	@Override
	public int getItemViewType(int position) {
		return (position >= super.getCount()) ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
	}

	@Override
	public T getItem(int position) {
		return (getItemViewType(position) == VIEW_TYPE_ITEM) ? super.getItem(position) : null;
	}

	@Override
	public long getItemId(int position) {
		return (getItemViewType(position) == VIEW_TYPE_ITEM) ? super.getItemId(position) : -1;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (getItemViewType(position) == VIEW_TYPE_LOADING) {
			if (convertView == null) {
				convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_status, parent, false);
			}

			if (hasError()) {
				convertView.findViewById(android.R.id.progress).setVisibility(View.GONE);
				((TextView) convertView.findViewById(android.R.id.text1)).setText(mErrorMessage);
			} else {
				convertView.findViewById(android.R.id.progress).setVisibility(View.VISIBLE);
				((TextView) convertView.findViewById(android.R.id.text1)).setText(R.string.loading);
			}

			return convertView;

		} else {
			T item = getItem(position);
			if (convertView == null) {
				convertView = LayoutInflater.from(getContext()).inflate(mResource, parent, false);
			}

			bind(convertView, item);
			return convertView;
		}
	}

	protected abstract void bind(View view, T item);

	public void setCurrentPage(int page) {
		mCurrentPage = page;
	}

	private boolean hasMoreResults() {
		return mCurrentPage * mPageSize < mTotalCount;
	}

	private boolean hasError() {
		return !TextUtils.isEmpty(mErrorMessage);
	}
}
