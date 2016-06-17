package com.boardgamegeek.ui.adapter;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.ui.loader.PaginatedData;

import butterknife.BindView;
import butterknife.ButterKnife;
import hugo.weaving.DebugLog;

public abstract class PaginatedRecyclerViewAdapter<T> extends RecyclerView.Adapter<PaginatedRecyclerViewAdapter.PaginatedViewHolder> {
	private static final int VIEW_TYPE_ITEM = 0;
	private static final int VIEW_TYPE_LOADING = 1;

	private final LayoutInflater inflater;
	@LayoutRes private final int layoutResourceId;
	private PaginatedData<T> data;

	@DebugLog
	public PaginatedRecyclerViewAdapter(Context context, @LayoutRes int layoutResourceId, PaginatedData<T> data) {
		inflater = LayoutInflater.from(context);
		this.layoutResourceId = layoutResourceId;
		this.data = data;
		setHasStableIds(true);
	}

	@DebugLog
	public void update(PaginatedData<T> data) {
		this.data = data;
		notifyDataSetChanged();
	}

	@DebugLog
	public void clear() {
		this.data.clear();
	}

	@DebugLog
	@Override
	public PaginatedViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		switch (viewType) {
			case VIEW_TYPE_LOADING:
				View view = inflater.inflate(R.layout.row_status, parent, false);
				return new LoadingViewHolder(view);
			case VIEW_TYPE_ITEM:
				final View itemView = inflater.inflate(layoutResourceId, parent, false);
				return getViewHolder(itemView);
		}
		return null;
	}

	@NonNull
	protected abstract PaginatedViewHolder getViewHolder(View itemView);

	@DebugLog
	@Override
	@SuppressWarnings("unchecked")
	public void onBindViewHolder(PaginatedRecyclerViewAdapter.PaginatedViewHolder holder, int position) {
		switch (holder.getItemViewType()) {
			case VIEW_TYPE_LOADING:
				((LoadingViewHolder) holder).bind(data);
				break;
			case VIEW_TYPE_ITEM:
				T item = data.getItems().get(position);
				((PaginatedItemViewHolder) holder).bind(item);
				break;
		}
	}

	@DebugLog
	@Override
	public int getItemCount() {
		return data.getItems().size() + ((data.hasMoreResults() || data.hasError()) ? 1 : 0);
	}

	@DebugLog
	@Override
	public int getItemViewType(int position) {
		return (position >= data.getItems().size()) ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
	}

	@DebugLog
	@Override
	public long getItemId(int position) {
		return (getItemViewType(position) == VIEW_TYPE_ITEM) ? position : -1;
	}

	abstract class PaginatedViewHolder extends RecyclerView.ViewHolder {
		PaginatedViewHolder(View itemView) {
			super(itemView);
		}
	}

	abstract class PaginatedItemViewHolder extends PaginatedViewHolder {
		PaginatedItemViewHolder(View itemView) {
			super(itemView);
		}

		protected abstract void bind(T item);
	}

	class LoadingViewHolder extends PaginatedViewHolder {
		@BindView(android.R.id.text1) TextView textView;
		@BindView(android.R.id.progress) View progressView;

		LoadingViewHolder(View itemView) {
			super(itemView);
			ButterKnife.bind(this, itemView);
		}

		public void bind(PaginatedData<T> data) {
			if (data.hasError()) {
				progressView.setVisibility(View.GONE);
				textView.setText(data.getErrorMessage());
			} else {
				progressView.setVisibility(View.VISIBLE);
				textView.setText(R.string.loading);
			}
		}
	}
}
