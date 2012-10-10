package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemoteSearchHandler;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.model.SearchResult;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.UIUtils;

public class SearchResultsFragment extends SherlockListFragment implements
	LoaderManager.LoaderCallbacks<List<SearchResult>> {
	private static final String TAG = makeLogTag(SearchResultsFragment.class);
	private static final int LOADER_ID = 0;

	private String mSearchText;
	private SearchResultsAdapter mAdapter;

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
		view.setBackgroundColor(Color.WHITE);
		final ListView listView = getListView();
		listView.setCacheColorHint(Color.WHITE);
		listView.setFastScrollEnabled(true);
		listView.setOnCreateContextMenuListener(this);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		registerForContextMenu(getListView());
		setEmptyText(getString(R.string.empty_search));
		setListShown(false);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mSearchText = intent.getStringExtra(SearchManager.QUERY);

		getLoaderManager().restartLoader(LOADER_ID, null, this);
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
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		} catch (ClassCastException e) {
			LOGE(TAG, "bad menuInfo", e);
			return;
		}

		SearchResult game = (SearchResult) mAdapter.getItem(info.position);
		UIUtils.createBoardgameContextMenu(menu, menuInfo, game.Name);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		} catch (ClassCastException e) {
			LOGE(TAG, "bad menuInfo", e);
			return false;
		}

		SearchResult game = (SearchResult) mAdapter.getItem(info.position);
		if (game == null) {
			return false;
		}

		switch (item.getItemId()) {
			case UIUtils.MENU_ITEM_VIEW: {
				ActivityUtils.launchGame(getActivity(), game.Id, game.Name);
				return true;
			}
			case UIUtils.MENU_ITEM_LOG_PLAY: {
				ActivityUtils.logPlay(getActivity(), false, game.Id, game.Name);
				return true;
			}
			case UIUtils.MENU_ITEM_QUICK_LOG_PLAY: {
				ActivityUtils.logPlay(getActivity(), true, game.Id, game.Name);
				return true;
			}
			case UIUtils.MENU_ITEM_SHARE: {
				ActivityUtils.shareGame(getActivity(), game.Id, game.Name);
				return true;
			}
			case UIUtils.MENU_ITEM_LINK_BGG: {
				ActivityUtils.linkBgg(getActivity(), game.Id);
				return true;
			}
		}
		return false;
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

		if (results != null && results.size() == 1 && BggApplication.getInstance().getSkipResults()) {
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
			RemoteExecutor executor = new RemoteExecutor(httpClient, null);
			RemoteSearchHandler handler = new RemoteSearchHandler();
			mErrorMessage = "";

			LOGI(TAG, "Searching for " + mQuery);
			if (BggApplication.getInstance().getExactSearch()) {
				String url = HttpUtils.constructSearchUrl(mQuery, true);
				try {
					executor.executeGet(url, handler);
					if (handler.isBggDown()) {
						mErrorMessage = getContext().getString(R.string.bgg_down);
					}
				} catch (HandlerException e) {
					LOGE(TAG, "searching", e);
					mErrorMessage = e.getMessage();
				}
			}

			if (TextUtils.isEmpty(mErrorMessage) && handler.getCount() == 0) {
				String url = HttpUtils.constructSearchUrl(mQuery, false);
				try {
					executor.executeGet(url, handler);
					if (handler.isBggDown()) {
						mErrorMessage = getContext().getString(R.string.bgg_down);
					}
				} catch (HandlerException e) {
					LOGE(TAG, "searching", e);
					mErrorMessage = e.getMessage();
				}
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
}
