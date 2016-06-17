package com.boardgamegeek.ui.adapter;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.ui.model.PaginatedData;

import hugo.weaving.DebugLog;

public abstract class PaginatedArrayAdapter<T> extends BaseAdapter {
	private static final int VIEW_TYPE_ITEM = 0;
	private static final int VIEW_TYPE_LOADING = 1;

	private final LayoutInflater inflater;
	@LayoutRes private final int layoutResourceId;
	private PaginatedData<T> data;

	@DebugLog
	public PaginatedArrayAdapter(Context context, @LayoutRes int layoutResourceId, PaginatedData<T> data) {
		inflater = LayoutInflater.from(context);
		this.layoutResourceId = layoutResourceId;
		this.data = data;
	}

	@DebugLog
	public void update(PaginatedData<T> data) {
		this.data = data;
		notifyDataSetChanged();
	}

	public void clear() {
		this.data.clear();
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
		return data.getItems().size() + ((data.hasMoreResults() || data.hasError()) ? 1 : 0);
	}

	@DebugLog
	@Override
	public int getItemViewType(int position) {
		return (position >= data.getItems().size()) ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
	}

	@DebugLog
	@Override
	public T getItem(int position) {
		return (getItemViewType(position) == VIEW_TYPE_ITEM) ? data.getItems().get(position) : null;
	}

	@DebugLog
	@Override
	public long getItemId(int position) {
		return (getItemViewType(position) == VIEW_TYPE_ITEM) ? position : -1;
	}

	@DebugLog
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (getItemViewType(position) == VIEW_TYPE_LOADING) {
			convertView = getView(convertView, parent, R.layout.row_status);

			final TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
			final View progressView = convertView.findViewById(android.R.id.progress);
			if (data.hasError()) {
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
			convertView = inflater.inflate(row_status, parent, false);
		}
		return convertView;
	}

	@DebugLog
	protected abstract void bind(View view, T item);
}
