package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.GeekList;
import com.boardgamegeek.model.GeekListItem;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.ui.loader.BggLoader;
import com.boardgamegeek.ui.loader.SafeResponse;
import com.boardgamegeek.ui.widget.TimestampView;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.AnimationUtils;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.UIUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;
import timber.log.Timber;

public class GeekListFragment extends Fragment implements LoaderCallbacks<SafeResponse<GeekList>> {
	private static final int LOADER_ID = 99103;
	private int geekListId;
	private GeekListRecyclerViewAdapter adapter;

	Unbinder unbinder;
	@BindView(android.R.id.progress) View progressView;
	@BindView(android.R.id.empty) TextView emptyView;
	@BindView(android.R.id.list) RecyclerView recyclerView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		geekListId = intent.getIntExtra(ActivityUtils.KEY_ID, BggContract.INVALID_ID);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_thread, container, false);
		unbinder = ButterKnife.bind(this, rootView);
		setUpRecyclerView();
		return rootView;
	}

	@Override
	public void onResume() {
		super.onResume();
		// If this is called in onActivityCreated as recommended, the loader is finished twice
		getLoaderManager().initLoader(LOADER_ID, null, this);
	}

	@Override
	public void onDestroy() {
		if (unbinder != null) unbinder.unbind();
		super.onDestroy();
	}

	@DebugLog
	private void setUpRecyclerView() {
		recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
		recyclerView.setHasFixedSize(true);
	}

	@Override
	public Loader<SafeResponse<GeekList>> onCreateLoader(int id, Bundle data) {
		return new GeekListLoader(getActivity(), geekListId);
	}

	@Override
	public void onLoadFinished(Loader<SafeResponse<GeekList>> loader, SafeResponse<GeekList> data) {
		if (getActivity() == null) {
			return;
		}

		if (adapter == null) {
			adapter = new GeekListRecyclerViewAdapter(getActivity(), data == null ? null : data.getBody());
			recyclerView.setAdapter(adapter);
		}

		if (adapter.getItemCount() == 0 || data == null) {
			AnimationUtils.fadeIn(getActivity(), emptyView, isResumed());
		} else if (data.hasError()) {
			emptyView.setText(data.getErrorMessage());
			AnimationUtils.fadeIn(getActivity(), emptyView, isResumed());
		} else {
			AnimationUtils.fadeIn(getActivity(), recyclerView, isResumed());
		}
		AnimationUtils.fadeOut(progressView);
	}

	@Override
	public void onLoaderReset(Loader<SafeResponse<GeekList>> loader) {
	}

	private static class GeekListLoader extends BggLoader<SafeResponse<GeekList>> {
		private final BggService service;
		private final int geekListId;

		public GeekListLoader(Context context, int geekListId) {
			super(context);
			service = Adapter.createForXml();
			this.geekListId = geekListId;
		}

		@Override
		public SafeResponse<GeekList> loadInBackground() {
			return new SafeResponse<>(service.geekList(geekListId));
		}
	}

	public static class GeekListRecyclerViewAdapter extends RecyclerView.Adapter<GeekListRecyclerViewAdapter.GeekListViewHolder> {
		private static final int VIEW_TYPE_HEADER = R.layout.header_geeklist;
		private static final int VIEW_TYPE_ITEM = R.layout.row_geeklist_item;
		private final LayoutInflater inflater;
		private final GeekList geekList;

		public GeekListRecyclerViewAdapter(Context context, GeekList geekList) {
			inflater = LayoutInflater.from(context);
			this.geekList = geekList;
			setHasStableIds(true);
		}

		@Override
		public int getItemCount() {
			return (geekList == null || geekList.getItems() == null) ? 0 : geekList.getItems().size();
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getItemViewType(int position) {
			if (position == 0) {
				return VIEW_TYPE_HEADER;
			} else {
				return VIEW_TYPE_ITEM;
			}
		}

		@Override
		public GeekListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View itemView = inflater.inflate(viewType, parent, false);
			if (viewType == VIEW_TYPE_HEADER) {
				return new GeekListHeaderViewHolder(itemView);
			} else if (viewType == VIEW_TYPE_ITEM) {
				return new GeekListItemViewHolder(itemView);
			}
			return null;
		}

		@Override
		public void onBindViewHolder(GeekListViewHolder holder, int position) {
			if (holder.getItemViewType() == VIEW_TYPE_HEADER) {
				((GeekListHeaderViewHolder) holder).bind(geekList);
			} else if (holder.getItemViewType() == VIEW_TYPE_ITEM) {
				try {
					GeekListItem item = geekList.getItems().get(position);
					((GeekListItemViewHolder) holder).bind(item, position);
				} catch (ArrayIndexOutOfBoundsException e) {
					Timber.w("Didn't find a GeekList item as expected", e);
				}
			}
		}

		public abstract class GeekListViewHolder extends RecyclerView.ViewHolder {
			public GeekListViewHolder(View itemView) {
				super(itemView);
			}
		}

		public class GeekListHeaderViewHolder extends GeekListViewHolder {
			@BindView(R.id.username) TextView username;
			@BindView(R.id.description) TextView description;
			@BindView(R.id.items) TextView items;
			@BindView(R.id.thumbs) TextView thumbs;
			@BindView(R.id.posted_date) TimestampView postDate;
			@BindView(R.id.edited_date) TimestampView editDate;

			public GeekListHeaderViewHolder(View itemView) {
				super(itemView);
				ButterKnife.bind(this, itemView);
			}

			public void bind(final GeekList geekList) {
				if (geekList != null) {
					final Context context = itemView.getContext();

					username.setText(context.getString(R.string.by_prefix, geekList.getUsername()));
					PresentationUtils.setTextOrHide(description, geekList.getDescription());
					items.setText(context.getString(R.string.items_suffix, geekList.getNumberOfItems()));
					thumbs.setText(context.getString(R.string.thumbs_suffix, geekList.getThumbs()));
					postDate.setTimestamp(geekList.getPostDate());
					editDate.setTimestamp(geekList.getEditDate());

					itemView.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							Intent intent = new Intent(context, GeekListDescriptionActivity.class);
							intent.putExtra(ActivityUtils.KEY_ID, geekList.getId());
							intent.putExtra(ActivityUtils.KEY_TITLE, geekList.getTitle());
							intent.putExtra(ActivityUtils.KEY_GEEKLIST, (Parcelable) itemView.getTag());
							context.startActivity(intent);
						}
					});
				}
			}
		}

		public class GeekListItemViewHolder extends GeekListViewHolder {
			@BindView(R.id.order) TextView orderView;
			@BindView(R.id.thumbnail) ImageView thumbnail;
			@BindView(R.id.game_name) TextView name;
			@BindView(R.id.username) TextView username;
			@BindView(R.id.type) TextView type;

			public GeekListItemViewHolder(View itemView) {
				super(itemView);
				ButterKnife.bind(this, itemView);
			}

			public void bind(final GeekListItem item, int order) {
				if (item == null) return;

				final Context context = itemView.getContext();

				orderView.setText(String.valueOf(order));
				ImageUtils.loadThumbnail(item.imageId(), thumbnail);
				name.setText(item.getObjectName());
				int objectTypeId = item.getObjectTypeId();
				if (objectTypeId != 0) {
					type.setText(objectTypeId);
				}
				username.setText(username.getContext().getString(R.string.by_prefix, item.username));

				itemView.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (item.getObjectId() != BggContract.INVALID_ID) {
							Intent intent = new Intent(context, GeekListItemActivity.class);
							intent.putExtra(ActivityUtils.KEY_ID, geekList.getId());
							intent.putExtra(ActivityUtils.KEY_TITLE, geekList.getTitle());
							intent.putExtra(ActivityUtils.KEY_ORDER, orderView.getText().toString());
							intent.putExtra(ActivityUtils.KEY_NAME, name.getText().toString());
							intent.putExtra(ActivityUtils.KEY_TYPE, type.getText().toString());
							intent.putExtra(ActivityUtils.KEY_IMAGE_ID, item.imageId());
							intent.putExtra(ActivityUtils.KEY_USERNAME, username.getText().toString());
							intent.putExtra(ActivityUtils.KEY_THUMBS, item.getThumbCount());
							intent.putExtra(ActivityUtils.KEY_POSTED_DATE, item.getPostDate());
							intent.putExtra(ActivityUtils.KEY_EDITED_DATE, item.getEditDate());
							intent.putExtra(ActivityUtils.KEY_BODY, item.body);
							intent.putExtra(ActivityUtils.KEY_OBJECT_URL, item.getObjectUrl());
							intent.putExtra(ActivityUtils.KEY_OBJECT_ID, item.getObjectId());
							intent.putExtra(ActivityUtils.KEY_IS_BOARD_GAME, item.isBoardGame());
							context.startActivity(intent);
						}
					}
				});
			}
		}
	}
}
