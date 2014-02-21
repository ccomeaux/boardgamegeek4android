package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteBuddyCollectionParser;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.model.BuddyGame;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.BuddyUtils;
import com.boardgamegeek.util.UIUtils;

public class BuddyCollectionFragment extends BggListFragment implements LoaderManager.LoaderCallbacks<List<BuddyGame>> {
	private static final String TAG = makeLogTag(BuddyCollectionFragment.class);
	private static final int BUDDY_GAMES_LOADER_ID = 1;
	private static final String STATE_STATUS_VALUE = "buddy_collection_status_value";
	private static final String STATE_STATUS_LABEL = "buddy_collection_status_entry";

	private BuddyGamesAdapter mGamesAdapter;
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
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mName = BuddyUtils.getNameFromIntent(intent);

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
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		if (!(activity instanceof Callbacks)) {
			throw new ClassCastException("Activity must implement fragment's callbacks.");
		}

		mCallbacks = (Callbacks) activity;
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
	public void onListItemClick(ListView listView, View convertView, int position, long id) {
		super.onListItemClick(listView, convertView, position, id);
		BuddyGameViewHolder holder = (BuddyGameViewHolder) convertView.getTag();
		if (holder != null) {
			ActivityUtils.launchGame(getActivity(), Integer.parseInt(holder.id), holder.name.getText().toString());
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
	public Loader<List<BuddyGame>> onCreateLoader(int id, Bundle data) {
		return new BuddyGamesLoader(getActivity(), mName, mStatusValue);
	}

	@Override
	public void onLoadFinished(Loader<List<BuddyGame>> loader, List<BuddyGame> games) {
		if (getActivity() == null) {
			return;
		}

		if (mGamesAdapter == null) {
			mGamesAdapter = new BuddyGamesAdapter(getActivity(), games);
			setListAdapter(mGamesAdapter);
		} else {
			mGamesAdapter.setGames(games);
		}

		if (loaderHasError()) {
			setEmptyText(loaderErrorMessage());
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
	public void onLoaderReset(Loader<List<BuddyGame>> loader) {
	}

	private boolean loaderHasError() {
		final BuddyGamesLoader loader = getLoader();
		return (loader != null) ? loader.hasError() : false;
	}

	private String loaderErrorMessage() {
		final BuddyGamesLoader loader = getLoader();
		return (loader != null) ? loader.getErrorMessage() : "";
	}

	private BuddyGamesLoader getLoader() {
		if (isAdded()) {
			Loader<List<BuddyGame>> loader = getLoaderManager().getLoader(BUDDY_GAMES_LOADER_ID);
			return (BuddyGamesLoader) loader;
		}
		return null;
	}

	private static class BuddyGamesLoader extends AsyncTaskLoader<List<BuddyGame>> {
		private String mUsername;
		private String mErrorMessage;
		private String mStatus;

		public BuddyGamesLoader(Context context, String username, String status) {
			super(context);
			mUsername = username;
			mStatus = status;
			mErrorMessage = "";
		}

		@Override
		public List<BuddyGame> loadInBackground() {
			RemoteExecutor executor = new RemoteExecutor(getContext());
			RemoteBuddyCollectionParser parser = new RemoteBuddyCollectionParser(mUsername, mStatus);
			executor.safelyExecuteGet(parser);
			mErrorMessage = parser.getErrorMessage();
			return parser.getResults();
		}

		@Override
		public void deliverResult(List<BuddyGame> games) {
			if (isStarted()) {
				super.deliverResult(games == null ? null : new ArrayList<BuddyGame>(games));
			}
		}

		@Override
		protected void onStartLoading() {
			forceLoad();
		}

		@Override
		protected void onStopLoading() {
			cancelLoad();
		}

		@Override
		protected void onReset() {
			super.onReset();
			onStopLoading();
		}

		public boolean hasError() {
			return !TextUtils.isEmpty(mErrorMessage);
		}

		public String getErrorMessage() {
			return mErrorMessage;
		}
	}

	public static class BuddyGamesAdapter extends ArrayAdapter<BuddyGame> implements SectionIndexer {
		private static final int STATE_UNKNOWN = 0;
		private static final int STATE_SECTIONED_CELL = 1;
		private static final int STATE_REGULAR_CELL = 2;

		private List<BuddyGame> mBuddyGames;
		private LayoutInflater mInflater;
		private String[] mSections;
		HashMap<String, Integer> mIndexer;
		private int[] mCellStates;

		public BuddyGamesAdapter(Activity activity, List<BuddyGame> games) {
			super(activity, R.layout.row_collection, games);
			mInflater = activity.getLayoutInflater();
			setGames(games);
		}

		@Override
		public void notifyDataSetChanged() {
			super.notifyDataSetChanged();
			updateIndex();
		}

		public void setGames(List<BuddyGame> games) {
			mBuddyGames = games;
			mCellStates = mBuddyGames == null ? null : new int[mBuddyGames.size()];
			notifyDataSetChanged();
		}

		private void updateIndex() {
			// Create indexer
			mIndexer = new HashMap<String, Integer>();
			int size = mBuddyGames.size();
			for (int i = size - 1; i >= 0; i--) {
				String index = getIndexForPosition(i);
				mIndexer.put(index, i);
			}
			// Create sections from indexer
			ArrayList<String> sections = new ArrayList<String>(mIndexer.keySet());
			Collections.sort(sections);
			mSections = new String[sections.size()];
			sections.toArray(mSections);
		}

		@Override
		public int getCount() {
			return mBuddyGames.size();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			String currentIndex = getIndexForPosition(position);

			boolean needSeparator = false;

			switch (mCellStates[position]) {
				case STATE_SECTIONED_CELL:
					needSeparator = true;
					break;
				case STATE_REGULAR_CELL:
					needSeparator = false;
					break;
				case STATE_UNKNOWN:
				default:
					if (position == 0) {
						needSeparator = true;
					} else {
						String previousIndex = getIndexForPosition(position - 1);
						if (!currentIndex.equals(previousIndex)) {
							needSeparator = true;
						}
					}
					mCellStates[position] = needSeparator ? STATE_SECTIONED_CELL : STATE_REGULAR_CELL;
					break;
			}

			BuddyGameViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.row_collection, parent, false);
				holder = new BuddyGameViewHolder(convertView);
				convertView.setTag(holder);
			} else {
				holder = (BuddyGameViewHolder) convertView.getTag();
			}
			convertView.findViewById(R.id.list_thumbnail).setVisibility(View.GONE);

			BuddyGame game;
			try {
				game = mBuddyGames.get(position);
			} catch (ArrayIndexOutOfBoundsException e) {
				return convertView;
			}
			if (game != null) {
				holder.name.setText(game.Name);
				holder.year.setText(game.Year);
				holder.id = game.Id;
				if (needSeparator) {
					holder.separator.setText(currentIndex);
					holder.separator.setVisibility(View.VISIBLE);
				} else {
					holder.separator.setVisibility(View.GONE);
				}
			}
			return convertView;
		}

		@Override
		public int getPositionForSection(int section) {
			String letter = mSections[section];
			return mIndexer.get(letter);
		}

		@Override
		public int getSectionForPosition(int position) {
			String index = getIndexForPosition(position);
			for (int i = 0; i < mSections.length; i++) {
				if (index.equals(mSections[i])) {
					return i;
				}
			}
			return 0;
		}

		@Override
		public Object[] getSections() {
			return mSections;
		}

		private String getIndexForPosition(int position) {
			if (position < mBuddyGames.size()) {
				BuddyGame game = mBuddyGames.get(position);
				return game.SortName.substring(0, 1).toUpperCase(Locale.getDefault());
			}
			return "";
		}
	}

	public static class BuddyGameViewHolder {
		public TextView separator;
		public TextView name;
		public TextView year;
		public String id;

		public BuddyGameViewHolder(View view) {
			separator = (TextView) view.findViewById(R.id.separator);
			name = (TextView) view.findViewById(R.id.name);
			year = (TextView) view.findViewById(R.id.year);
		}
	}
}
