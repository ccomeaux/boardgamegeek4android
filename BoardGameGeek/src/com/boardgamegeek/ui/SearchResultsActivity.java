package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemoteSearchHandler;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.model.SearchResult;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.UIUtils;

public class SearchResultsActivity extends ListActivity {
	private static final String TAG = makeLogTag(SearchResultsActivity.class);

	private static final int HELP_VERSION = 1;

	private List<SearchResult> mSearchResults = new ArrayList<SearchResult>();
	private BoardGameAdapter mAdapter;
	private TextView mSearchTextView;
	private String mSearchText;

	// Workaround for bug http://code.google.com/p/android/issues/detail?id=7139
	private AdapterContextMenuInfo mLinksMenuInfo = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_searchresults);

		UIUtils.setTitle(this, getResources().getString(R.string.title_search_results));
		UIUtils.allowTypeToSearch(this);

		getListView().setOnCreateContextMenuListener(this);
		mAdapter = new BoardGameAdapter();
		mSearchTextView = (TextView) findViewById(R.id.search_text);

		parseIntent(getIntent());

		UIUtils.showHelpDialog(this, BggApplication.HELP_SEARCHRESULTS_KEY, HELP_VERSION, R.string.help_searchresults);
	}

	@Override
	public void onNewIntent(Intent intent) {
		parseIntent(intent);
	}

	@Override
	public void setTitle(CharSequence title) {
		UIUtils.setTitle(this, title);
	}

	public void onHomeClick(View v) {
		UIUtils.resetToHome(this);
	}

	public void onSearchClick(View v) {
		onSearchRequested();
	}

	public void onListItemClick(ListView l, View v, int position, long id) {
		SearchResult game = (SearchResult) mAdapter.getItem(position);
		viewBoardGame(game.Id, game.Name);
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
			if (info == null && mLinksMenuInfo != null) {
				info = mLinksMenuInfo;
			}
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
				viewBoardGame(game.Id, game.Name);
				return true;
			}
			case UIUtils.MENU_ITEM_LOG_PLAY: {
				ActivityUtils.logPlay(this, false, game.Id, game.Name);
				return true;
			}
			case UIUtils.MENU_ITEM_QUICK_LOG_PLAY: {
				ActivityUtils.logPlay(this, true, game.Id, game.Name);
				return true;
			}
			case UIUtils.MENU_ITEM_SHARE: {
				ActivityUtils.shareGame(this, game.Id, game.Name);
				return true;
			}
			case UIUtils.MENU_ITEM_LINKS: {
				mLinksMenuInfo = info;
				return true;
			}
			case UIUtils.MENU_ITEM_LINK_BGG: {
				ActivityUtils.linkBgg(this, game.Id);
				return true;
			}
			case UIUtils.MENU_ITEM_LINK_BG_PRICES: {
				ActivityUtils.linkBgPrices(this, game.Name);
				return true;
			}
			case UIUtils.MENU_ITEM_LINK_AMAZON: {
				ActivityUtils.linkAmazon(this, game.Name);
				return true;
			}
			case UIUtils.MENU_ITEM_LINK_EBAY: {
				ActivityUtils.linkEbay(this, game.Name);
				return true;
			}
			case UIUtils.MENU_ITEM_COMMENTS: {
				ActivityUtils.showComments(this, game.Id, game.Name);
			}
		}
		return false;
	}

	@Override
	public void onContextMenuClosed(Menu menu) {
		// We don't need it anymore
		mLinksMenuInfo = null;
	}

	private void parseIntent(Intent intent) {
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			mSearchText = intent.getExtras().getString(SearchManager.QUERY);
			if (TextUtils.isEmpty(mSearchText)) {
				showError("Search performed with no search text");
			} else {
				mSearchResults.clear();
				setListAdapter(null);
				String message = String.format(getResources().getString(R.string.search_searching), mSearchText);
				mSearchTextView.setText(message);
				UIUtils.showListMessage(this, R.string.search_message, false);
				SearchTask task = new SearchTask();
				task.execute();
			}
		} else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			String data = intent.getDataString();
			if (TextUtils.isEmpty(data)) {
				showError("Trying to view an unspecified game.");
			} else {
				Uri uri = Uri.parse(data);
				viewBoardGame(StringUtils.parseInt(uri.getLastPathSegment(), 0), "");
				finish();
			}
		} else {
			showError("Received bad intent action: " + intent.getAction());
		}
	}

	private void showError(String message) {
		LOGW(TAG, message);
		Toast.makeText(this, message, Toast.LENGTH_LONG).show();
		UIUtils.showListMessage(this, "Error");
	}

	private void viewBoardGame(int gameId, String gameName) {
		final Uri gameUri = Games.buildGameUri(gameId);
		final Intent intent = new Intent(Intent.ACTION_VIEW, gameUri);
		intent.putExtra(BoardgameActivity.KEY_GAME_NAME, gameName);
		startActivity(intent);
	}

	private class SearchTask extends AsyncTask<Void, Void, RemoteSearchHandler> {

		private HttpClient mHttpClient;
		private RemoteExecutor mExecutor;
		private RemoteSearchHandler mHandler = new RemoteSearchHandler();

		@Override
		protected void onPreExecute() {
			mSearchResults.clear();
			mHttpClient = HttpUtils.createHttpClient(SearchResultsActivity.this, true);
			mExecutor = new RemoteExecutor(mHttpClient, null);
		}

		@Override
		protected RemoteSearchHandler doInBackground(Void... params) {
			if (BggApplication.getInstance().getExactSearch()) {
				executeGet(true);
				if (mHandler.isBggDown() || mHandler.getCount() > 0) {
					return mHandler;
				}
			}
			executeGet(false);
			return mHandler;
		}

		@Override
		protected void onPostExecute(RemoteSearchHandler result) {
			int count = result.getCount();
			if (result.isBggDown()) {
				UIUtils.showListMessage(SearchResultsActivity.this, R.string.bgg_down);
			} else if (count == 0) {
				String message = String.format(getResources().getString(R.string.search_no_results), mSearchText);
				mSearchTextView.setText(message);
				UIUtils.showListMessage(SearchResultsActivity.this, R.string.search_no_results_details);
			} else if (count == 1 && BggApplication.getInstance().getSkipResults()) {
				SearchResult game = result.getResults().get(0);
				viewBoardGame(game.Id, game.Name);
				finish();
			} else {
				mSearchResults = result.getResults();
				String message = String.format(getResources().getString(R.string.search_results),
					mSearchResults.size(), mSearchText);
				mSearchTextView.setText(message);
				mAdapter = new BoardGameAdapter();
				setListAdapter(mAdapter);
			}
		}

		private void executeGet(boolean useExact) {
			String url = HttpUtils.constructSearchUrl(mSearchText, useExact);
			try {
				mExecutor.executeGet(url, mHandler);
			} catch (HandlerException e) {
				LOGE(TAG, e.toString());
			}
		}
	}

	class BoardGameAdapter extends ArrayAdapter<SearchResult> {
		private LayoutInflater mInflater;

		BoardGameAdapter() {
			super(SearchResultsActivity.this, R.layout.row_search, mSearchResults);
			mInflater = getLayoutInflater();
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.row_search, parent, false);
				holder = new ViewHolder(convertView);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			SearchResult game = mSearchResults.get(position);
			if (game != null) {
				holder.name.setText(game.Name);
				if (!game.IsNamePrimary) {
					holder.name.setTypeface(holder.name.getTypeface(), 2);
				}
				if (game.YearPublished > 0) {
					holder.year.setText("" + game.YearPublished);
				}
				holder.gameId.setText(String.format(getResources().getString(R.string.id_list_text), game.Id));
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
