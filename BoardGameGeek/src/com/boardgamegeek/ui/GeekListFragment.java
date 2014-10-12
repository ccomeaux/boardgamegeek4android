package com.boardgamegeek.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.GeekList;
import com.boardgamegeek.model.GeekListItem;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.ui.widget.BggLoader;
import com.boardgamegeek.ui.widget.Data;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.GeekListUtils;
import com.boardgamegeek.util.UIUtils;

public class GeekListFragment extends BggListFragment implements
	LoaderManager.LoaderCallbacks<GeekListFragment.GeeklistData> {
	private static final int GEEKLIST_LOADER_ID = 99103;
	private int mGeekListId;
	private String mGeekListTitle;
	private GeeklistAdapter mGeekListAdapter;
	@InjectView(R.id.username) TextView mUsernameView;
	@InjectView(R.id.items) TextView mItemsView;
	@InjectView(R.id.thumbs) TextView mThumbsView;
	@InjectView(R.id.posted_date) TextView mPostDateView;
	@InjectView(R.id.edited_date) TextView mEditDateView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mGeekListId = intent.getIntExtra(GeekListUtils.KEY_ID, BggContract.INVALID_ID);
		mGeekListTitle = intent.getStringExtra(GeekListUtils.KEY_TITLE);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getString(R.string.empty_geeklist));
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		View header = View.inflate(getActivity(), R.layout.header_geeklist, null);
		getListView().addHeaderView(header);
		ButterKnife.inject(this, header);
	}

	@Override
	public void onResume() {
		super.onResume();
		// If this is called in onActivityCreated as recommended, the loader is finished twice
		getLoaderManager().initLoader(GEEKLIST_LOADER_ID, null, this);
	}

	@Override
	public void onListItemClick(ListView listView, View convertView, int position, long id) {
		ViewHolder holder = (ViewHolder) convertView.getTag();
		if (holder != null && holder.objectId != BggContract.INVALID_ID) {
			Intent intent = new Intent(getActivity(), GeekListItemActivity.class);
			intent.putExtra(GeekListUtils.KEY_ID, mGeekListId);
			intent.putExtra(GeekListUtils.KEY_TITLE, mGeekListTitle);
			intent.putExtra(GeekListUtils.KEY_ORDER, holder.order.getText().toString());
			intent.putExtra(GeekListUtils.KEY_NAME, holder.name.getText().toString());
			intent.putExtra(GeekListUtils.KEY_TYPE, holder.type.getText().toString());
			intent.putExtra(GeekListUtils.KEY_IMAGE_ID, holder.imageId);
			intent.putExtra(GeekListUtils.KEY_USERNAME, holder.username.getText().toString());
			intent.putExtra(GeekListUtils.KEY_THUMBS, holder.thumbs);
			intent.putExtra(GeekListUtils.KEY_POSTED_DATE, holder.postedDate);
			intent.putExtra(GeekListUtils.KEY_EDITED_DATE, holder.editedDate);
			intent.putExtra(GeekListUtils.KEY_BODY, holder.body);
			intent.putExtra(GeekListUtils.KEY_OBJECT_URL, holder.objectUrl);
			intent.putExtra(GeekListUtils.KEY_OBJECT_ID, holder.objectId);
			intent.putExtra(GeekListUtils.KEY_IS_BOARD_GAME, holder.isBoardGame);
			startActivity(intent);
		}
	}

	@Override
	public Loader<GeeklistData> onCreateLoader(int id, Bundle data) {
		return new GeeklistLoader(getActivity(), mGeekListId);
	}

	@Override
	public void onLoadFinished(Loader<GeeklistData> loader, GeeklistData data) {
		if (getActivity() == null) {
			return;
		}

		if (mGeekListAdapter == null) {
			mGeekListAdapter = new GeeklistAdapter(getActivity(), data.list());
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

	private void bindHeader(GeeklistData data) {
		GeekList geekList = data.getGeekList();
		if (geekList != null) {
			mUsernameView.setText(getString(R.string.by_prefix, geekList.getUsername()));
			mItemsView.setText(getString(R.string.items_suffix, geekList.getNumberOfItems()));
			mThumbsView.setText(getString(R.string.thumbs_suffix, geekList.getThumbs()));
			mPostDateView.setText(getString(R.string.posted_prefix,
				DateTimeUtils.formatForumDate(getActivity(), geekList.getPostDate())));
			mEditDateView.setText(getString(R.string.edited_prefix,
				DateTimeUtils.formatForumDate(getActivity(), geekList.getEditDate())));
		}
	}

	@Override
	public void onLoaderReset(Loader<GeeklistData> loader) {
	}

	private static class GeeklistLoader extends BggLoader<GeeklistData> {
		private BggService mService;
		private int mGeeklistId;

		public GeeklistLoader(Context context, int geeklistId) {
			super(context);
			mService = Adapter.create();
			mGeeklistId = geeklistId;
		}

		@Override
		public GeeklistData loadInBackground() {
			GeeklistData geeklistData;
			try {
				geeklistData = new GeeklistData(mService.geekList(mGeeklistId));
			} catch (Exception e) {
				geeklistData = new GeeklistData(e);
			}
			return geeklistData;
		}
	}

	static class GeeklistData extends Data<GeekListItem> {
		private GeekList mGeekList;

		public GeeklistData(GeekList geeklist) {
			super();
			mGeekList = geeklist;
		}

		public GeeklistData(Exception e) {
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

	public class GeeklistAdapter extends ArrayAdapter<GeekListItem> {
		private LayoutInflater mInflater;

		public GeeklistAdapter(Activity activity, List<GeekListItem> items) {
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
