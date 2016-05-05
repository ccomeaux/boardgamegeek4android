package com.boardgamegeek.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.GeekList;
import com.boardgamegeek.model.GeekListItem;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.ui.loader.BggLoader;
import com.boardgamegeek.ui.loader.SafeResponse;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.UIUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class GeekListFragment extends BggListFragment implements LoaderCallbacks<SafeResponse<GeekList>> {
	private static final int LOADER_ID = 99103;
	private int geekListId;
	private String geekListTitle;
	private GeekListAdapter adapter;
	private View headerView;
	private Header header;

	public static class Header {
		@BindView(R.id.username) TextView username;
		@BindView(R.id.description) TextView description;
		@BindView(R.id.items) TextView items;
		@BindView(R.id.thumbs) TextView thumbs;
		@BindView(R.id.posted_date) TextView postDate;
		@BindView(R.id.edited_date) TextView editDate;

		public Header(View view) {
			ButterKnife.bind(this, view);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		geekListId = intent.getIntExtra(ActivityUtils.KEY_ID, BggContract.INVALID_ID);
		geekListTitle = intent.getStringExtra(ActivityUtils.KEY_TITLE);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getString(R.string.empty_geeklist));
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		headerView = View.inflate(getActivity(), R.layout.header_geeklist, null);
		getListView().addHeaderView(headerView);
		header = new Header(headerView);
	}

	@Override
	public void onResume() {
		super.onResume();
		// If this is called in onActivityCreated as recommended, the loader is finished twice
		getLoaderManager().initLoader(LOADER_ID, null, this);
	}

	@Override
	public void onListItemClick(ListView listView, View convertView, int position, long id) {
		if (position == 0) {
			Intent intent = new Intent(getActivity(), GeekListDescriptionActivity.class);
			intent.putExtra(ActivityUtils.KEY_ID, geekListId);
			intent.putExtra(ActivityUtils.KEY_TITLE, geekListTitle);
			intent.putExtra(ActivityUtils.KEY_GEEKLIST, (Parcelable) headerView.getTag());
			startActivity(intent);
		} else {
			ViewHolder holder = (ViewHolder) convertView.getTag();
			if (holder != null && holder.objectId != BggContract.INVALID_ID) {
				Intent intent = new Intent(getActivity(), GeekListItemActivity.class);
				intent.putExtra(ActivityUtils.KEY_ID, geekListId);
				intent.putExtra(ActivityUtils.KEY_TITLE, geekListTitle);
				intent.putExtra(ActivityUtils.KEY_ORDER, holder.order.getText().toString());
				intent.putExtra(ActivityUtils.KEY_NAME, holder.name.getText().toString());
				intent.putExtra(ActivityUtils.KEY_TYPE, holder.type.getText().toString());
				intent.putExtra(ActivityUtils.KEY_IMAGE_ID, holder.imageId);
				intent.putExtra(ActivityUtils.KEY_USERNAME, holder.username.getText().toString());
				intent.putExtra(ActivityUtils.KEY_THUMBS, holder.thumbs);
				intent.putExtra(ActivityUtils.KEY_POSTED_DATE, holder.postedDate);
				intent.putExtra(ActivityUtils.KEY_EDITED_DATE, holder.editedDate);
				intent.putExtra(ActivityUtils.KEY_BODY, holder.body);
				intent.putExtra(ActivityUtils.KEY_OBJECT_URL, holder.objectUrl);
				intent.putExtra(ActivityUtils.KEY_OBJECT_ID, holder.objectId);
				intent.putExtra(ActivityUtils.KEY_IS_BOARD_GAME, holder.isBoardGame);
				startActivity(intent);
			}
		}
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
			adapter = new GeekListAdapter(getActivity(),
				(data == null || data.getBody() == null) ?
					new ArrayList<GeekListItem>() :
					data.getBody().getItems());
			setListAdapter(adapter);
			bindHeader(data == null ? null : data.getBody());
		}
		initializeTimeBasedUi();

		if (data == null) {
			setEmptyText(getString(R.string.empty_geeklist));
		} else if (data.hasError()) {
			setEmptyText(data.getErrorMessage());
		} else {
			if (isResumed()) {
				setListShown(true);
			} else {
				setListShownNoAnimation(true);
			}
			restoreScrollState();
		}
	}

	private void bindHeader(GeekList geekList) {
		if (geekList != null) {
			headerView.setTag(geekList);
			header.username.setText(getString(R.string.by_prefix, geekList.getUsername()));
			if (!TextUtils.isEmpty(geekList.getDescription())) {
				header.description.setVisibility(View.VISIBLE);
				header.description.setText(geekList.getDescription());
			}
			header.items.setText(getString(R.string.items_suffix, geekList.getNumberOfItems()));
			header.thumbs.setText(getString(R.string.thumbs_suffix, geekList.getThumbs()));
			header.postDate.setText(getString(R.string.posted_prefix, DateTimeUtils.formatForumDate(getActivity(), geekList.getPostDate())));
			header.editDate.setText(getString(R.string.edited_prefix, DateTimeUtils.formatForumDate(getActivity(), geekList.getEditDate())));
		}
	}

	@Override
	public void onLoaderReset(Loader<SafeResponse<GeekList>> loader) {
	}

	@Override
	protected void updateTimeBasedUi() {
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
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

	public class GeekListAdapter extends ArrayAdapter<GeekListItem> {
		private final LayoutInflater inflater;

		public GeekListAdapter(Activity activity, List<GeekListItem> items) {
			super(activity, R.layout.row_geeklist_item, items);
			inflater = activity.getLayoutInflater();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.row_geeklist_item, parent, false);
				holder = new ViewHolder(convertView);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			GeekListItem item;
			try {
				item = getItem(position);
			} catch (ArrayIndexOutOfBoundsException e) {
				return convertView;
			}
			if (item != null) {
				Context context = convertView.getContext();
				holder.imageId = item.imageId();
				holder.objectId = item.getObjectId();
				holder.body = item.body;
				holder.thumbs = item.getThumbCount();
				holder.postedDate = item.getPostDate();
				holder.editedDate = item.getEditDate();
				holder.objectUrl = item.getObejctUrl();
				holder.isBoardGame = item.isBoardGame();

				holder.order.setText(String.valueOf(position + 1));
				loadThumbnail(holder.imageId, holder.thumbnail);
				holder.name.setText(item.getObjectName());
				int objectTypeId = item.getObjectTypeId();
				if (objectTypeId != 0) {
					holder.type.setText(objectTypeId);
				}
				holder.username.setText(context.getString(R.string.by_prefix, item.username));
			}
			return convertView;
		}
	}

	@SuppressWarnings("unused")
	public static class ViewHolder {
		public int imageId;
		public int objectId;
		public String body;
		public int thumbs;
		public long postedDate;
		public long editedDate;
		public String objectUrl;
		public boolean isBoardGame;
		@BindView(R.id.order) TextView order;
		@BindView(R.id.thumbnail) ImageView thumbnail;
		@BindView(R.id.game_name) TextView name;
		@BindView(R.id.username) TextView username;
		@BindView(R.id.type) TextView type;

		public ViewHolder(View view) {
			ButterKnife.bind(this, view);
		}
	}
}
