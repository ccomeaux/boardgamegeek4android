package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.CollectionItem;
import com.boardgamegeek.model.CollectionResponse;
import com.boardgamegeek.ui.BuddyCollectionFragment.BuddyCollectionAdapter.BuddyGameViewHolder;
import com.boardgamegeek.ui.widget.BggLoader;
import com.boardgamegeek.ui.widget.Data;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.BuddyUtils;
import com.boardgamegeek.util.UIUtils;

public class BuddyCollectionFragment extends StickyHeaderListFragment implements
	LoaderManager.LoaderCallbacks<BuddyCollectionFragment.BuddyCollectionData> {
	private static final String TAG = makeLogTag(BuddyCollectionFragment.class);
	private static final int BUDDY_GAMES_LOADER_ID = 1;
	private static final String STATE_STATUS_VALUE = "buddy_collection_status_value";
	private static final String STATE_STATUS_LABEL = "buddy_collection_status_entry";

	private BuddyCollectionAdapter mAdapter;
	private SubMenu mSubMenu;
	private String mName;
	private String mStatusValue;
	private String mStatusLabel;
	private String[] mStatusValues;
	private String[] mStatusEntries;

	public interface Callbacks {
		public void onCollectionStatusChanged(String status);
	}

	private static Callbacks sDummyCallbacks = new Callbacks() {
		@Override
		public void onCollectionStatusChanged(String status) {
		}
	};

	private Callbacks mCallbacks = sDummyCallbacks;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		if (!(activity instanceof Callbacks)) {
			throw new ClassCastException("Activity must implement fragment's callbacks.");
		}

		mCallbacks = (Callbacks) activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mName = intent.getStringExtra(BuddyUtils.KEY_BUDDY_NAME);

		if (TextUtils.isEmpty(mName)) {
			LOGW(TAG, "Missing buddy name.");
			return;
		}

		mStatusEntries = getResources().getStringArray(R.array.pref_sync_status_entries);
		mStatusValues = getResources().getStringArray(R.array.pref_sync_status_values);

		setHasOptionsMenu(true);
		if (savedInstanceState == null) {
			mStatusValue = mStatusValues[0];
			mStatusLabel = mStatusEntries[0];
		} else {
			mStatusValue = savedInstanceState.getString(STATE_STATUS_VALUE);
			mStatusLabel = savedInstanceState.getString(STATE_STATUS_LABEL);
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mCallbacks = sDummyCallbacks;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getString(R.string.empty_buddy_collection));
		reload();
	}

	@Override
	public void onListItemClick(View convertView, int position, long id) {
		super.onListItemClick(convertView, position, id);
		BuddyGameViewHolder holder = (BuddyGameViewHolder) convertView.getTag();
		if (holder != null) {
			ActivityUtils.launchGame(getActivity(), holder.id, holder.name.getText().toString());
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString(STATE_STATUS_VALUE, mStatusValue);
		outState.putString(STATE_STATUS_LABEL, mStatusLabel);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		mSubMenu = menu.addSubMenu(1, Menu.FIRST + 99, 0, R.string.menu_collection_status);
		for (int i = 0; i < mStatusEntries.length; i++) {
			mSubMenu.add(1, Menu.FIRST + i, i, mStatusEntries[i]);
		}
		mSubMenu.setGroupCheckable(1, true, true);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		// check the proper submenu item
		if (mSubMenu != null) {
			for (int i = 0; i < mSubMenu.size(); i++) {
				MenuItem mi = mSubMenu.getItem(i);
				if (mi.getTitle().equals(mStatusLabel)) {
					mi.setChecked(true);
					break;
				}
			}
		}
		super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		String status = "";
		int i = id - Menu.FIRST;
		if (i >= 0 && i < mStatusValues.length) {
			status = mStatusValues[i];
		}

		if (!TextUtils.isEmpty(status) && !status.equals(mStatusValue)) {
			mStatusValue = status;
			mStatusLabel = mStatusEntries[i];

			reload();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void reload() {
		mCallbacks.onCollectionStatusChanged(mStatusLabel);
		setListShown(false);
		getLoaderManager().restartLoader(BUDDY_GAMES_LOADER_ID, null, this);
	}

	@Override
	public Loader<BuddyCollectionData> onCreateLoader(int id, Bundle data) {
		return new BuddyGamesLoader(getActivity(), mName, mStatusValue);
	}

	@Override
	public void onLoadFinished(Loader<BuddyCollectionData> loader, BuddyCollectionData data) {
		if (getActivity() == null) {
			return;
		}

		if (mAdapter == null) {
			mAdapter = new BuddyCollectionAdapter(getActivity(), data.list());
			setListAdapter(mAdapter);
		}
		mAdapter.notifyDataSetChanged();

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
	public void onLoaderReset(Loader<BuddyCollectionData> loader) {
	}

	private static class BuddyGamesLoader extends BggLoader<BuddyCollectionData> {
		private BggService mService;
		private String mUsername;
		private Map<String, String> mOptions;

		public BuddyGamesLoader(Context context, String username, String status) {
			super(context);
			mService = Adapter.create();
			mUsername = username;
			mOptions = new HashMap<String, String>();
			mOptions.put(status, "1");
			mOptions.put(BggService.COLLECTION_QUERY_KEY_BRIEF, "1");
		}

		@Override
		public BuddyCollectionData loadInBackground() {
			BuddyCollectionData collection = null;
			try {
				collection = new BuddyCollectionData(mService.collection(mUsername, mOptions));
			} catch (Exception e) {
				collection = new BuddyCollectionData(e);
			}
			return collection;
		}
	}

	static class BuddyCollectionData extends Data<CollectionItem> {
		private CollectionResponse mResponse;

		public BuddyCollectionData(CollectionResponse response) {
			mResponse = response;
		}

		public BuddyCollectionData(Exception e) {
			super(e);
		}

		@Override
		public List<CollectionItem> list() {
			if (mResponse == null || mResponse.items == null) {
				return new ArrayList<CollectionItem>();
			}
			return mResponse.items;
		}
	}

	public static class BuddyCollectionAdapter extends ArrayAdapter<CollectionItem> implements StickyListHeadersAdapter {
		private List<CollectionItem> mBuddyCollection;
		private LayoutInflater mInflater;

		public BuddyCollectionAdapter(Activity activity, List<CollectionItem> collection) {
			super(activity, R.layout.row_collection, collection);
			mInflater = activity.getLayoutInflater();
			setCollection(collection);
		}

		public void setCollection(List<CollectionItem> games) {
			mBuddyCollection = games;
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return mBuddyCollection.size();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			BuddyGameViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.row_collection, parent, false);
				holder = new BuddyGameViewHolder(convertView);
				convertView.setTag(holder);
			} else {
				holder = (BuddyGameViewHolder) convertView.getTag();
			}
			convertView.findViewById(R.id.list_thumbnail).setVisibility(View.GONE);

			CollectionItem game;
			try {
				game = mBuddyCollection.get(position);
			} catch (ArrayIndexOutOfBoundsException e) {
				return convertView;
			}
			if (game != null) {
				holder.name.setText(game.gameName());
				holder.year.setText(String.valueOf(game.gameId));
				holder.id = game.gameId;
			}
			return convertView;
		}

		@Override
		public View getHeaderView(int position, View convertView, ViewGroup parent) {
			HeaderViewHolder holder;
			if (convertView == null) {
				holder = new HeaderViewHolder();
				convertView = mInflater.inflate(R.layout.row_header, parent, false);
				holder.text = (TextView) convertView.findViewById(R.id.separator);
				convertView.setTag(holder);
			} else {
				holder = (HeaderViewHolder) convertView.getTag();
			}
			holder.text.setText(getHeaderText(position));
			return convertView;
		}

		@Override
		public long getHeaderId(int position) {
			return getHeaderText(position).charAt(0);
		}

		private String getHeaderText(int position) {
			CollectionItem game = mBuddyCollection.get(position);
			if (game != null) {
				return game.gameSortName().substring(0, 1);
			}
			return "-";
		}

		class BuddyGameViewHolder {
			public TextView name;
			public TextView year;
			public int id;

			public BuddyGameViewHolder(View view) {
				name = (TextView) view.findViewById(R.id.name);
				year = (TextView) view.findViewById(R.id.year);
			}
		}

		class HeaderViewHolder {
			TextView text;
		}
	}
}
