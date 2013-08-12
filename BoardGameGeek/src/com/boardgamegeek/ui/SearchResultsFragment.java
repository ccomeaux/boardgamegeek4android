package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.http.client.HttpClient;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
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

import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemoteSearchHandler;
import com.boardgamegeek.model.SearchResult;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.UIUtils;
import com.boardgamegeek.util.actionmodecompat.ActionMode;
import com.boardgamegeek.util.actionmodecompat.MultiChoiceModeListener;

public class SearchResultsFragment extends BggListFragment implements
	LoaderManager.LoaderCallbacks<List<SearchResult>>, MultiChoiceModeListener {
	private static final String TAG = makeLogTag(SearchResultsFragment.class);
	private static final int LOADER_ID = 0;

	private String mSearchText;
	private SearchResultsAdapter mAdapter;
	private LinkedHashSet<Integer> mSelectedPositions = new LinkedHashSet<Integer>();
	private MenuItem mLogPlayMenuItem;
	private MenuItem mLogPlayQuickMenuItem;
	private MenuItem mBggLinkMenuItem;

	public interface Callbacks {
		public void onResultCount(int count);

		public void onExactMatch();
	}

	private static Callbacks sDummyCallbacks = new Callbacks() {
		@Override
		public void onResultCount(int count) {
		}

		@Override
		public void onExactMatch() {
		}
	};

	private Callbacks mCallbacks = sDummyCallbacks;

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		getListView().setOnCreateContextMenuListener(this);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mSearchText = intent.getStringExtra(SearchManager.QUERY);

		setEmptyText(getString(R.string.empty_search));
		getLoaderManager().restartLoader(LOADER_ID, null, this);

		ActionMode.setMultiChoiceMode(getListView(), getActivity(), this);
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
	public Loader<List<SearchResult>> onCreateLoader(int id, Bundle data) {
		return new SearchLoader(getActivity(), mSearchText);
	}

	@Override
	public void onLoadFinished(Loader<List<SearchResult>> loader, List<SearchResult> results) {
		if (getActivity() == null) {
			return;
		}

		if (results != null && results.size() == 1 && PreferencesUtils.getSkipResults(getActivity())) {
			SearchResult game = results.get(0);
			ActivityUtils.launchGame(getActivity(), game.Id, game.Name);
			mCallbacks.onExactMatch();
			return;
		}

		mAdapter = new SearchResultsAdapter(getActivity(), results);
		setListAdapter(mAdapter);

		if (loaderHasError()) {
			setEmptyText(loaderErrorMessage());
		} else {
			if (isResumed()) {
				setListShown(true);
			} else {
				setListShownNoAnimation(true);
			}
			restoreScrollState();
			mCallbacks.onResultCount(results.size());
		}
	}

	@Override
	public void onLoaderReset(Loader<List<SearchResult>> results) {
	}

	public void onListItemClick(ListView l, View v, int position, long id) {
		SearchResult game = (SearchResult) mAdapter.getItem(position);
		ActivityUtils.launchGame(getActivity(), game.Id, game.Name);
	}

	private boolean loaderHasError() {
		SearchLoader loader = getLoader();
		return (loader != null) ? loader.hasError() : false;
	}

	private String loaderErrorMessage() {
		SearchLoader loader = getLoader();
		return (loader != null) ? loader.getErrorMessage() : "";
	}

	private SearchLoader getLoader() {
		if (isAdded()) {
			Loader<List<SearchResult>> loader = getLoaderManager().getLoader(LOADER_ID);
			return (SearchLoader) loader;
		}
		return null;
	}

	private static class SearchLoader extends AsyncTaskLoader<List<SearchResult>> {
		private List<SearchResult> mData;
		private String mQuery;
		private String mErrorMessage;

		public SearchLoader(Context context, String query) {
			super(context);
			mQuery = query;
			mErrorMessage = "";
		}

		@Override
		public List<SearchResult> loadInBackground() {
			HttpClient httpClient = HttpUtils.createHttpClient(getContext(), true);
			RemoteExecutor executor = new RemoteExecutor(httpClient, getContext());
			RemoteSearchHandler handler = new RemoteSearchHandler();
			mErrorMessage = "";

			LOGI(TAG, "Searching for " + mQuery);
			if (PreferencesUtils.getExactSearch(getContext())) {
				String url = HttpUtils.constructSearchUrl(mQuery, true);
				executor.safelyExecuteGet(url, handler);
				mErrorMessage = handler.getErrorMessage();
			}

			if (TextUtils.isEmpty(mErrorMessage) && handler.getCount() == 0) {
				String url = HttpUtils.constructSearchUrl(mQuery, false);
				executor.safelyExecuteGet(url, handler);
				mErrorMessage = handler.getErrorMessage();
			}
			return handler.getResults();
		}

		@Override
		public void deliverResult(List<SearchResult> data) {
			if (isReset()) {
				return;
			}

			mData = data;
			if (isStarted()) {
				super.deliverResult(data == null ? null : new ArrayList<SearchResult>(data));
			}
		}

		@Override
		protected void onStartLoading() {
			if (mData != null) {
				deliverResult(mData);
			}
			if (takeContentChanged() || mData == null) {
				forceLoad();
			}
		}

		@Override
		protected void onStopLoading() {
			cancelLoad();
		}

		@Override
		protected void onReset() {
			super.onReset();
			onStopLoading();
			mData = null;
		}

		public boolean hasError() {
			return !TextUtils.isEmpty(mErrorMessage);
		}

		public String getErrorMessage() {
			return mErrorMessage;
		}
	}

	public static class SearchResultsAdapter extends ArrayAdapter<SearchResult> {
		private LayoutInflater mInflater;
		private Resources mResources;

		public SearchResultsAdapter(Activity activity, List<SearchResult> results) {
			super(activity, R.layout.row_search, results);
			mInflater = activity.getLayoutInflater();
			mResources = activity.getResources();
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

			SearchResult game = null;
			try {
				game = getItem(position);
			} catch (ArrayIndexOutOfBoundsException e) {
				return convertView;
			}
			if (game != null) {
				holder.name.setText(game.Name);
				if (!game.IsNamePrimary) {
					// make italic
					holder.name.setTypeface(holder.name.getTypeface(), 2);
				}
				if (game.YearPublished > 0) {
					holder.year.setText("" + game.YearPublished);
				}
				holder.gameId.setText(String.format(mResources.getString(R.string.id_list_text), game.Id));
			}

			return convertView;
		}
	}

	static class ViewHolder {
		TextView name;
		TextView year;
		TextView gameId;

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
		mLogPlayQuickMenuItem.setVisible(count == 1 && PreferencesUtils.showQuickLogPlay(getActivity()));
		mBggLinkMenuItem.setVisible(count == 1);
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		SearchResult game = (SearchResult) mAdapter.getItem(mSelectedPositions.iterator().next());
		switch (item.getItemId()) {
			case R.id.menu_log_play:
				mode.finish();
				ActivityUtils.logPlay(getActivity(), game.Id, game.Name);
				return true;
			case R.id.menu_log_play_quick:
				mode.finish();
				Toast.makeText(this, R.string.msg_logging_play, Toast.LENGTH_SHORT).show();
				ActivityUtils.logQuickPlay(getActivity(), game.Id, game.Name);
				return true;
			case R.id.menu_share:
				mode.finish();
				if (mSelectedPositions.size() == 1) {
					ActivityUtils.shareGame(getActivity(), game.Id, game.Name);
				} else {
					List<Pair<Integer, String>> games = new ArrayList<Pair<Integer, String>>(mSelectedPositions.size());
					for (int position : mSelectedPositions) {
						SearchResult g = (SearchResult) mAdapter.getItem(position);
						games.add(new Pair<Integer, String>(g.Id, g.Name));
					}
					ActivityUtils.shareGames(getActivity(), games);
				}
				return true;
			case R.id.menu_link:
				mode.finish();
				ActivityUtils.linkBgg(getActivity(), game.Id);
				return true;
		}
		return false;
	}
}
