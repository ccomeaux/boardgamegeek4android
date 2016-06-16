package com.boardgamegeek.ui.adapter;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.ui.loader.PaginatedData;

import hugo.weaving.DebugLog;

public abstract class PaginatedArrayAdapter<T> extends ArrayAdapter<T> {
	private static final int VIEW_TYPE_ITEM = 0;
	private static final int VIEW_TYPE_LOADING = 1;
	@LayoutRes private final int layoutResourceId;
	private PaginatedData<T> data;

	@DebugLog
	public PaginatedArrayAdapter(Context context, @LayoutRes int layoutResourceId, PaginatedData<T> data) {
		super(context, layoutResourceId, data.getItems());
		this.layoutResourceId = layoutResourceId;
		this.data = data;
	}

	@DebugLog
	public void update(PaginatedData<T> data) {
		clear();
		data.getCurrentPageNumber();
		for (T datum : data.getItems()) {
			add(datum);
		}
	}

	@DebugLog
	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	@DebugLog
	@Override
	public boolean isEnabled(int position) {
		return getItemViewType(position) == VIEW_TYPE_ITEM;
	}

	@DebugLog
	@Override
	public int getViewTypeCount() {
		return 2;
	}

	@DebugLog
	@Override
	public boolean hasStableIds() {
		return true;
	}

	@DebugLog
	@Override
	public int getCount() {
		return super.getCount() + ((data.hasMoreResults() || hasError()) ? 1 : 0);
	}

	@DebugLog
	@Override
	public int getItemViewType(int position) {
		return (position >= super.getCount()) ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
	}

	@DebugLog
	@Override
	public T getItem(int position) {
		return (getItemViewType(position) == VIEW_TYPE_ITEM) ? super.getItem(position) : null;
	}

	@DebugLog
	@Override
	public long getItemId(int position) {
		return (getItemViewType(position) == VIEW_TYPE_ITEM) ? super.getItemId(position) : -1;
	}

	@DebugLog
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (getItemViewType(position) == VIEW_TYPE_LOADING) {
			convertView = getView(convertView, parent, R.layout.row_status);

			final TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
			final View progressView = convertView.findViewById(android.R.id.progress);
			if (hasError()) {
				progressView.setVisibility(View.GONE);
				textView.setText(data.getErrorMessage());
			} else {
				progressView.setVisibility(View.VISIBLE);
				textView.setText(R.string.loading);
			}

			return convertView;
		} else {
			T item = getItem(position);
			convertView = getView(convertView, parent, layoutResourceId);
			bind(convertView, item);
			return convertView;
		}
	}

	@DebugLog
	private View getView(View convertView, ViewGroup parent, int row_status) {
		if (convertView == null) {
			convertView = LayoutInflater.from(getContext()).inflate(row_status, parent, false);
		}
		return convertView;
	}

	@DebugLog
	protected abstract void bind(View view, T item);

	@DebugLog
	private boolean hasError() {
		return !TextUtils.isEmpty(data.getErrorMessage());
	}
}
