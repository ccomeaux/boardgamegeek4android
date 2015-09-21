package com.boardgamegeek.ui;

import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.events.SearchResultsCountChangedEvent;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.HelpUtils;

import hugo.weaving.DebugLog;
import timber.log.Timber;

public class SearchResultsActivity extends SimpleSinglePaneActivity {
	private static final String SEARCH_TEXT = "search_text";
	private static final int HELP_VERSION = 1;
	private String mSearchText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(R.string.title_search_results);

		if (savedInstanceState != null) {
			mSearchText = savedInstanceState.getString(SEARCH_TEXT);
		}

		HelpUtils.showHelpDialog(this, HelpUtils.HELP_SEARCHRESULTS_KEY, HELP_VERSION, R.string.help_searchresults);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString(SEARCH_TEXT, mSearchText);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.search_only;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		final MenuItem searchItem = menu.findItem(R.id.menu_search);
		if (searchItem != null) {
			final SearchView view = (SearchView) MenuItemCompat.getActionView(searchItem);
			if (view == null) {
				Timber.w("Could not set up search view, view is null.");
			} else {
				view.setIconified(false);
				view.setOnCloseListener(new SearchView.OnCloseListener() {
					@Override
					public boolean onClose() {
						finish();
						return true;
					}
				});
				view.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
					@Override
					public boolean onQueryTextSubmit(String s) {
						if (s != null && s.length() > 1 && s.length() <= 2) {
							((SearchResultsFragment) getFragment()).forceQueryUpdate(s);
						}
						// close the auto-complete list; don't pass to a different activity
						view.clearFocus();
						return true;
					}

					@Override
					public boolean onQueryTextChange(String s) {
						if (s != null && s.length() > 2) {
							((SearchResultsFragment) getFragment()).requestQueryUpdate(s);
						} else {
							((SearchResultsFragment) getFragment()).requestQueryUpdate("");
						}
						return true;
					}
				});
			}
		}
		return true;
	}

	@DebugLog
	public void onEvent(SearchResultsCountChangedEvent event) {
		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			String message = String.format(getResources().getString(R.string.search_results), event.count, mSearchText);
			actionBar.setSubtitle(message);
		}
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		Fragment fragment;
		String action = intent.getAction();
		if (action != null && Intent.ACTION_VIEW.equals(action)) {
			Uri uri = intent.getData();
			if (uri == null) {
				Toast.makeText(this, R.string.search_error_no_data, Toast.LENGTH_LONG).show();
			} else {
				ActivityUtils.launchGame(this, Games.getGameId(uri), "");
			}
			finish();
			return null;
		} else {
			mSearchText = "";
			if (intent.hasExtra(SearchManager.QUERY)) {
				mSearchText = intent.getExtras().getString(SearchManager.QUERY);
			}
			final ActionBar actionBar = getSupportActionBar();
			if (actionBar != null) {
				actionBar.setSubtitle(String.format(getResources().getString(R.string.search_searching), mSearchText));
			}
			fragment = new SearchResultsFragment();
		}
		return fragment;
	}

	@Override
	protected int getDrawerResId() {
		return R.string.title_search;
	}
}
