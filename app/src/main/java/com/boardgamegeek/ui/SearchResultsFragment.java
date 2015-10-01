package com.boardgamegeek.ui;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.SearchResponse;
import com.boardgamegeek.model.SearchResult;
import com.boardgamegeek.ui.loader.BggLoader;
import com.boardgamegeek.ui.loader.Data;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.UIUtils;
import com.boardgamegeek.util.actionmodecompat.ActionMode;
import com.boardgamegeek.util.actionmodecompat.MultiChoiceModeListener;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class SearchResultsFragment extends BggListFragment implements
	LoaderManager.LoaderCallbacks<SearchResultsFragment.SearchData>, MultiChoiceModeListener {
	private static final int LOADER_ID = 0;

	private String mSearchText;
	private SearchResultsAdapter mAdapter;
	private final LinkedHashSet<Integer> mSelectedPositions = new LinkedHashSet<>();
	private MenuItem mLogPlayMenuItem;
	private MenuItem mLogPlayQuickMenuItem;
	private MenuItem mBggLinkMenuItem;
	private Snackbar mSnackbar;

	private static final int MESSAGE_QUERY_UPDATE = 1;
	private static final int QUERY_UPDATE_DELAY_MILLIS = 2000;
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == MESSAGE_QUERY_UPDATE) {
				requery((String) msg.obj);
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mSearchText = intent.getStringExtra(SearchManager.QUERY);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setEmptyText(getString(R.string.search_initial_help));
		getListView().setOnCreateContextMenuListener(this);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getLoaderManager().initLoader(LOADER_ID, null, this);
		ActionMode.setMultiChoiceMode(getListView(), getActivity(), this);
	}

	@Override
	protected boolean padTop() {
		return true;
	}

	@Override
	protected boolean padBottomForSnackBar() {
		return true;
	}

	@Override
	protected boolean dividerShown() {
		return true;
	}

	@Override
	public Loader<SearchData> onCreateLoader(int id, Bundle data) {
		return new SearchLoader(getActivity(), mSearchText);
	}

	@Override
	public void onLoadFinished(Loader<SearchData> loader, SearchData data) {
		setProgressShown(false);

		if (getActivity() == null) {
			return;
		}

		int count = data == null ? 0 : data.count();

		if (data != null) {
			mAdapter = new SearchResultsAdapter(getActivity(), data.list());
			setListAdapter(mAdapter);
		} else if (mAdapter != null) {
			mAdapter.clear();
		}

		if (data != null && data.hasError()) {
			setEmptyText(data.getErrorMessage());
		} else {
			if (TextUtils.isEmpty(mSearchText)) {
				setEmptyText(getString(R.string.search_initial_help));
			} else {
				setEmptyText(getString(R.string.empty_search));
			}
		}

		if (isResumed()) {
			setListShown(true);
		} else {
			setListShownNoAnimation(true);
		}

		restoreScrollState();

		if (TextUtils.isEmpty(mSearchText)) {
			if (mSnackbar != null) {
				mSnackbar.dismiss();
			}
		} else {
			if (mSnackbar == null || !mSnackbar.isShown()) {
				mSnackbar = Snackbar.make(getListContainer(),
					String.format(getResources().getString(R.string.search_results), count, mSearchText),
					Snackbar.LENGTH_INDEFINITE);
				mSnackbar.getView().setBackgroundResource(R.color.primary_dark);
			} else {
				mSnackbar.setText(String.format(getResources().getString(R.string.search_results), count, mSearchText));
			}
			mSnackbar.show();
		}
	}

	@Override
	public void onLoaderReset(Loader<SearchData> results) {
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		SearchResult game = mAdapter.getItem(position);
		ActivityUtils.launchGame(getActivity(), game.id, game.name);
	}

	public void requestQueryUpdate(String query) {
		setProgressShown(true);
		if (TextUtils.isEmpty(query)) {
			requery(query);
		} else {
			mHandler.removeMessages(MESSAGE_QUERY_UPDATE);
			mHandler.sendMessageDelayed(Message.obtain(mHandler, MESSAGE_QUERY_UPDATE, query), QUERY_UPDATE_DELAY_MILLIS);
		}
	}

	public void forceQueryUpdate(String query) {
		mHandler.removeMessages(MESSAGE_QUERY_UPDATE);
		setProgressShown(true);
		requery(query);
	}

	private void requery(String query) {
		if (query == null && mSearchText == null) {
			return;
		}
		if (mSearchText != null && mSearchText.equals(query)) {
			return;
		}
		mSearchText = query;
		getLoaderManager().restartLoader(LOADER_ID, null, SearchResultsFragment.this);
	}

	private static class SearchLoader extends BggLoader<SearchData> {
		private final BggService mService;
		private final String mQuery;

		public SearchLoader(Context context, String query) {
			super(context);
			mService = Adapter.create();
			mQuery = query;
		}

		@Override
		public SearchData loadInBackground() {
			if (TextUtils.isEmpty(mQuery)) {
				return null;
			}
			SearchData games = null;
			try {
				if (PreferencesUtils.getExactSearch(getContext())) {
					games = new SearchData(mService.search(mQuery, BggService.SEARCH_TYPE_BOARD_GAME, 1));
				}
			} catch (Exception e) {
				// we'll try it again below
			}
			try {
				if (games == null || games.count() == 0) {
					games = new SearchData(mService.search(mQuery, BggService.SEARCH_TYPE_BOARD_GAME, 0));
				}
			} catch (Exception e) {
				games = new SearchData(e);
			}
			return games;
		}
	}

	static class SearchData extends Data<SearchResult> {
		private SearchResponse mResponse;

		public SearchData(SearchResponse response) {
			mResponse = response;
		}

		public SearchData(Exception e) {
			super(e);
		}

		public int count() {
			if (mResponse == null) {
				return 0;
			}
			return mResponse.total;
		}

		@Override
		public List<SearchResult> list() {
			if (mResponse == null || mResponse.games == null) {
				return new ArrayList<>();
			}
			return mResponse.games;
		}
	}

	public static class SearchResultsAdapter extends ArrayAdapter<SearchResult> {
		private final LayoutInflater mInflater;
		private final String mGameString;

		public SearchResultsAdapter(Activity activity, List<SearchResult> results) {
			super(activity, R.layout.row_search, results);
			mInflater = activity.getLayoutInflater();
			mGameString = activity.getResources().getString(R.string.id_list_text);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.row_search, parent, false);
				holder = new ViewHolder(convertView);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			SearchResult game;
			try {
				game = getItem(position);
			} catch (ArrayIndexOutOfBoundsException e) {
				return convertView;
			}
			if (game != null) {
				holder.name.setText(game.name);
				int style;
				switch (game.getNameType()) {
					case SearchResult.NAME_TYPE_ALTERNATE:
						style = Typeface.ITALIC;
						break;
					case SearchResult.NAME_TYPE_PRIMARY:
					case SearchResult.NAME_TYPE_UNKNOWN:
					default:
						style = Typeface.NORMAL;
						break;
				}
				holder.name.setTypeface(holder.name.getTypeface(), style);
				holder.year.setText(PresentationUtils.describeYear(getContext(), game.getYearPublished()));
				holder.gameId.setText(String.format(mGameString, game.id));
			}

			return convertView;
		}
	}

	static class ViewHolder {
		final TextView name;
		final TextView year;
		final TextView gameId;

		public ViewHolder(View view) {
			name = (TextView) view.findViewById(R.id.name);
			year = (TextView) view.findViewById(R.id.year);
			gameId = (TextView) view.findViewById(R.id.gameId);
		}
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(R.menu.game_context, menu);
		mLogPlayMenuItem = menu.findItem(R.id.menu_log_play);
		mLogPlayQuickMenuItem = menu.findItem(R.id.menu_log_play_quick);
		mBggLinkMenuItem = menu.findItem(R.id.menu_link);
		mSelectedPositions.clear();
		return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		return false;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
	}

	@Override
	public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
		if (checked) {
			mSelectedPositions.add(position);
		} else {
			mSelectedPositions.remove(position);
		}

		int count = mSelectedPositions.size();
		mode.setTitle(getResources().getQuantityString(R.plurals.msg_games_selected, count, count));

		mLogPlayMenuItem.setVisible(count == 1 && PreferencesUtils.showLogPlay(getActivity()));
		mLogPlayQuickMenuItem.setVisible(PreferencesUtils.showQuickLogPlay(getActivity()));
		mBggLinkMenuItem.setVisible(count == 1);
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		if (mSelectedPositions == null || !mSelectedPositions.iterator().hasNext()) {
			return false;
		}
		SearchResult game = mAdapter.getItem(mSelectedPositions.iterator().next());
		switch (item.getItemId()) {
			case R.id.menu_log_play:
				mode.finish();
				ActivityUtils.logPlay(getActivity(), game.id, game.name, null, null, false);
				return true;
			case R.id.menu_log_play_quick:
				mode.finish();
				String text = getResources().getQuantityString(R.plurals.msg_logging_plays, mSelectedPositions.size());
				Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
				for (int position : mSelectedPositions) {
					SearchResult g = mAdapter.getItem(position);
					ActivityUtils.logQuickPlay(getActivity(), g.id, g.name);
				}
				return true;
			case R.id.menu_share:
				mode.finish();
				if (mSelectedPositions.size() == 1) {
					ActivityUtils.shareGame(getActivity(), game.id, game.name);
				} else {
					List<Pair<Integer, String>> games = new ArrayList<>(mSelectedPositions.size());
					for (int position : mSelectedPositions) {
						SearchResult g = mAdapter.getItem(position);
						games.add(new Pair<>(g.id, g.name));
					}
					ActivityUtils.shareGames(getActivity(), games);
				}
				return true;
			case R.id.menu_link:
				mode.finish();
				ActivityUtils.linkBgg(getActivity(), game.id);
				return true;
		}
		return false;
	}
}
