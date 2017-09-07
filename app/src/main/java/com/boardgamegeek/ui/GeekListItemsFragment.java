package com.boardgamegeek.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.model.GeekListItem;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.ui.model.GeekList;
import com.boardgamegeek.util.AnimationUtils;
import com.boardgamegeek.util.ImageUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;
import timber.log.Timber;

public class GeekListItemsFragment extends Fragment {
	private GeekListRecyclerViewAdapter adapter;

	Unbinder unbinder;
	@BindView(android.R.id.progress) ContentLoadingProgressBar progressView;
	@BindView(android.R.id.empty) TextView emptyView;
	@BindView(android.R.id.list) RecyclerView recyclerView;

	public static GeekListItemsFragment newInstance() {
		return new GeekListItemsFragment();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_thread, container, false);
		unbinder = ButterKnife.bind(this, rootView);
		setUpRecyclerView();
		return rootView;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (unbinder != null) unbinder.unbind();
	}

	@DebugLog
	private void setUpRecyclerView() {
		recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
		recyclerView.setHasFixedSize(true);
	}

	public void setData(GeekList geekList, List<GeekListItem> geekListItems) {
		if (geekList == null || geekListItems == null) return;
		if (adapter == null) {
			adapter = new GeekListRecyclerViewAdapter(getActivity(), geekList, geekListItems);
			recyclerView.setAdapter(adapter);
		}
		AnimationUtils.fadeIn(recyclerView, isResumed());
		progressView.hide();
	}

	public void setError() {
		setError(getString(R.string.empty_geeklist));
	}

	public void setError(String message) {
		emptyView.setText(message);
		AnimationUtils.fadeIn(emptyView, isResumed());
		progressView.hide();
	}

	public static class GeekListRecyclerViewAdapter extends RecyclerView.Adapter<GeekListRecyclerViewAdapter.GeekListViewHolder> {
		private final LayoutInflater inflater;
		private final GeekList geekList;
		private final List<GeekListItem> geekListItems;

		public GeekListRecyclerViewAdapter(Context context, GeekList geekList, List<GeekListItem> geekListItems) {
			inflater = LayoutInflater.from(context);
			this.geekList = geekList;
			this.geekListItems = geekListItems;
			setHasStableIds(true);
		}

		@Override
		public int getItemCount() {
			return geekListItems == null ? 0 : geekListItems.size();
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public GeekListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View itemView = inflater.inflate(R.layout.row_geeklist_item, parent, false);
			return new GeekListItemViewHolder(itemView);
		}

		@Override
		public void onBindViewHolder(GeekListViewHolder holder, int position) {
			try {
				GeekListItem item = geekListItems.get(position);
				((GeekListItemViewHolder) holder).bind(item, position + 1);
			} catch (ArrayIndexOutOfBoundsException e) {
				Timber.w(e, "Didn't find a GeekList item as expected");
			}
		}

		public abstract class GeekListViewHolder extends RecyclerView.ViewHolder {
			public GeekListViewHolder(View itemView) {
				super(itemView);
			}
		}

		public class GeekListItemViewHolder extends GeekListViewHolder {
			@BindView(R.id.order) TextView orderView;
			@BindView(R.id.thumbnail) ImageView thumbnailView;
			@BindView(R.id.game_name) TextView itemNameView;
			@BindView(R.id.username) TextView usernameView;

			public GeekListItemViewHolder(View itemView) {
				super(itemView);
				ButterKnife.bind(this, itemView);
			}

			public void bind(final GeekListItem item, final int order) {
				if (item == null) return;

				final Context context = itemView.getContext();

				orderView.setText(String.valueOf(order));
				ImageUtils.loadThumbnail(item.imageId(), thumbnailView);
				itemNameView.setText(item.getObjectName());
				if (item.getUsername().equals(geekList.getUsername())) {
					usernameView.setVisibility(View.GONE);
				} else {
					usernameView.setText(item.getUsername());
					usernameView.setVisibility(View.VISIBLE);
				}

				itemView.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (item.getObjectId() != BggContract.INVALID_ID) {
							GeekListItemActivity.start(context, geekList, item, order);
						}
					}
				});
			}
		}
	}
}
