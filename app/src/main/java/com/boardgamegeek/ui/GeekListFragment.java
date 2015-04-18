package com.boardgamegeek.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.LoaderManager;
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
import com.boardgamegeek.ui.loader.Data;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.UIUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class GeekListFragment extends BggListFragment implements
	LoaderManager.LoaderCallbacks<GeekListFragment.GeekListData> {
	private static final int LOADER_ID = 99103;
	private int mGeekListId;
	private String mGeekListTitle;
	private GeekListAdapter mGeekListAdapter;
	private View mHeader;
	@SuppressWarnings("unused") @InjectView(R.id.username) TextView mUsernameView;
	@SuppressWarnings("unused") @InjectView(R.id.description) TextView mDescription;
	@SuppressWarnings("unused") @InjectView(R.id.items) TextView mItemsView;
	@SuppressWarnings("unused") @InjectView(R.id.thumbs) TextView mThumbsView;
	@SuppressWarnings("unused") @InjectView(R.id.posted_date) TextView mPostDateView;
	@SuppressWarnings("unused") @InjectView(R.id.edited_date) TextView mEditDateView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mGeekListId = intent.getIntExtra(ActivityUtils.KEY_ID, BggContract.INVALID_ID);
		mGeekListTitle = intent.getStringExtra(ActivityUtils.KEY_TITLE);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getString(R.string.empty_geeklist));
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mHeader = View.inflate(getActivity(), R.layout.header_geeklist, null);
		getListView().addHeaderView(mHeader);
		ButterKnife.inject(this, mHeader);
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
			intent.putExtra(ActivityUtils.KEY_ID, mGeekListId);
			intent.putExtra(ActivityUtils.KEY_TITLE, mGeekListTitle);
			intent.putExtra(ActivityUtils.KEY_GEEKLIST, (Parcelable) mHeader.getTag());
			startActivity(intent);
		} else {
			ViewHolder holder = (ViewHolder) convertView.getTag();
			if (holder != null && holder.objectId != BggContract.INVALID_ID) {
				Intent intent = new Intent(getActivity(), GeekListItemActivity.class);
				intent.putExtra(ActivityUtils.KEY_ID, mGeekListId);
				intent.putExtra(ActivityUtils.KEY_TITLE, mGeekListTitle);
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
	public Loader<GeekListData> onCreateLoader(int id, Bundle data) {
		return new GeekListLoader(getActivity(), mGeekListId);
	}

	@Override
	public void onLoadFinished(Loader<GeekListData> loader, GeekListData data) {
		if (getActivity() == null) {
			return;
		}

		if (mGeekListAdapter == null) {
			mGeekListAdapter = new GeekListAdapter(getActivity(), data.list());
			setListAdapter(mGeekListAdapter);
			bindHeader(data);
		}
		mGeekListAdapter.notifyDataSetChanged();

		if (data.hasError()) {
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

	private void bindHeader(GeekListData data) {
		GeekList geekList = data.getGeekList();
		if (geekList != null) {
			mHeader.setTag(geekList);
			mUsernameView.setText(getString(R.string.by_prefix, geekList.getUsername()));
			if (!TextUtils.isEmpty(geekList.getDescription())) {
				mDescription.setVisibility(View.VISIBLE);
				mDescription.setText(geekList.getDescription());
			}
			mItemsView.setText(getString(R.string.items_suffix, geekList.getNumberOfItems()));
			mThumbsView.setText(getString(R.string.thumbs_suffix, geekList.getThumbs()));
			mPostDateView.setText(getString(R.string.posted_prefix,
				DateTimeUtils.formatForumDate(getActivity(), geekList.getPostDate())));
			mEditDateView.setText(getString(R.string.edited_prefix,
				DateTimeUtils.formatForumDate(getActivity(), geekList.getEditDate())));
		}
	}

	@Override
	public void onLoaderReset(Loader<GeekListData> loader) {
	}

	private static class GeekListLoader extends BggLoader<GeekListData> {
		private final BggService mService;
		private final int mGeekListId;

		public GeekListLoader(Context context, int geekListId) {
			super(context);
			mService = Adapter.create();
			mGeekListId = geekListId;
		}

		@Override
		public GeekListData loadInBackground() {
			GeekListData geeklistData;
			try {
				geeklistData = new GeekListData(mService.geekList(mGeekListId));
			} catch (Exception e) {
				geeklistData = new GeekListData(e);
			}
			return geeklistData;
		}
	}

	static class GeekListData extends Data<GeekListItem> {
		private GeekList mGeekList;

		public GeekListData(GeekList geeklist) {
			super();
			mGeekList = geeklist;
		}

		public GeekListData(Exception e) {
			super(e);
		}

		@Override
		protected List<GeekListItem> list() {
			if (mGeekList == null || mGeekList.getItems() == null) {
				return new ArrayList<>();
			}
			return mGeekList.getItems();
		}

		public GeekList getGeekList() {
			return mGeekList;
		}
	}

	public class GeekListAdapter extends ArrayAdapter<GeekListItem> {
		private final LayoutInflater mInflater;

		public GeekListAdapter(Activity activity, List<GeekListItem> items) {
			super(activity, R.layout.row_geeklist_item, items);
			mInflater = activity.getLayoutInflater();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.row_geeklist_item, parent, false);
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
		@InjectView(R.id.order) TextView order;
		@InjectView(R.id.thumbnail) ImageView thumbnail;
		@InjectView(R.id.game_name) TextView name;
		@InjectView(R.id.username) TextView username;
		@InjectView(R.id.type) TextView type;

		public ViewHolder(View view) {
			ButterKnife.inject(this, view);
		}
	}
}
