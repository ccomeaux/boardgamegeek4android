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
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.GeekList;
import com.boardgamegeek.model.GeekListItem;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.ui.widget.BggLoader;
import com.boardgamegeek.ui.widget.Data;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.GeekListUtils;
import com.boardgamegeek.util.UIUtils;

public class GeekListFragment extends BggListFragment implements
	LoaderManager.LoaderCallbacks<GeekListFragment.GeeklistData> {
	private static final int GEEKLIST_LOADER_ID = 99103;
	private int mGeekListId;
	private GeeklistAdapter mGeekListAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mGeekListId = intent.getIntExtra(GeekListUtils.KEY_GEEKLIST_ID, BggContract.INVALID_ID);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		getListView().setSelector(android.R.color.transparent);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getString(R.string.empty_geeklist));
	}

	@Override
	public void onResume() {
		super.onResume();
		// If this is called in onActivityCreated as recommended, the loader is finished twice
		getLoaderManager().initLoader(GEEKLIST_LOADER_ID, null, this);
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
		private GeekList mGeeklist;

		public GeeklistData(GeekList geeklist) {
			super();
			mGeeklist = geeklist;
		}

		public GeeklistData(Exception e) {
			super(e);
		}

		@Override
		protected List<GeekListItem> list() {
			if (mGeeklist == null || mGeeklist.items == null) {
				return new ArrayList<>();
			}
			return mGeeklist.items;
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
				holder.gameId = item.getGameId();
				holder.username.setText(context.getString(R.string.posted_by_prefix, item.username));
				holder.postedDate.setText(context.getString(R.string.posted_prefix,
					DateTimeUtils.formatForumDate(context, item.postDate())));
				holder.editedDate.setText(context.getString(R.string.edited_prefix,
					DateTimeUtils.formatForumDate(context, item.editDate())));
				holder.gameName.setText(item.objectname);

				loadThumbnail(item.imageId(), holder.thumbnail);

				// TODO: change to textview
				String content = GeekListUtils.convertBoardGameGeekXmlText(item.body);
				UIUtils.setWebViewText(holder.body, content);
			}
			return convertView;
		}
	}

	public static class ViewHolder {
		private Context mContext;
		public int gameId;
		@InjectView(R.id.thumbnail) ImageView thumbnail;
		@InjectView(R.id.game_name) TextView gameName;
		@InjectView(R.id.username) TextView username;
		@InjectView(R.id.posted_date) TextView postedDate;
		@InjectView(R.id.edited_date) TextView editedDate;
		@InjectView(R.id.body) WebView body;

		public ViewHolder(View view) {
			mContext = view.getContext();
			ButterKnife.inject(this, view);
		}

		@OnClick(R.id.header)
		public void onHeaderClick(View v) {
			if (gameId != BggContract.INVALID_ID) {
				ActivityUtils.launchGame(mContext, gameId, gameName.getText().toString());
			}
		}
	}
}
