package com.boardgamegeek.ui;

import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;

import icepick.Icepick;
import icepick.State;
import timber.log.Timber;

public class SearchResultsActivity extends SimpleSinglePaneActivity {
	@State @Nullable String searchText;
	private SearchView searchView;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Icepick.restoreInstanceState(this, savedInstanceState);
		setTitle(null);
	}

	@Override
	protected void onNewIntent(@NonNull Intent intent) {
		super.onNewIntent(intent);
		parseIntent(intent);
		if (searchView != null) {
			String query = searchView.getQuery().toString();
			if (!query.equals(searchText)) {
				searchView.setQuery(searchText, true);
			}
		}
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.search_widget;
	}

	@Override
	public boolean onCreateOptionsMenu(@NonNull Menu menu) {
		super.onCreateOptionsMenu(menu);
		final MenuItem searchItem = menu.findItem(R.id.menu_search);
		if (searchItem != null) {
			searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
			if (searchView == null) {
				Timber.w("Could not set up search view, view is null.");
			} else {
				SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
				searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
				searchView.setIconified(false);
				searchView.setOnCloseListener(new SearchView.OnCloseListener() {
					@Override
					public boolean onClose() {
						finish();
						return true;
					}
				});
				searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
					@Override
					public boolean onQueryTextSubmit(@Nullable String query) {
						if (query != null && query.length() > 1 && query.length() <= 2) {
							((SearchResultsFragment) getFragment()).forceQueryUpdate(query);
						}
						// close the auto-complete list; don't pass to a different activity
						searchView.clearFocus();
						searchText = query;
						return true;
					}

					@Override
					public boolean onQueryTextChange(@Nullable String newText) {
						if (newText != null && newText.length() > 2) {
							if (!newText.equals(searchText)) {
								((SearchResultsFragment) getFragment()).requestQueryUpdate(newText);
								searchText = newText;
							}
						} else {
							((SearchResultsFragment) getFragment()).requestQueryUpdate("");
						}
						return true;
					}
				});
				if (!TextUtils.isEmpty(searchText)) {
					searchView.setQuery(searchText, false);
				}
			}
		}
		return true;
	}

	@NonNull
	@Override
	protected Fragment onCreatePane(@NonNull Intent intent) {
		parseIntent(intent);
		return new SearchResultsFragment();
	}

	private void parseIntent(@NonNull Intent intent) {
		String action = intent.getAction();
		if (action != null && Intent.ACTION_VIEW.equals(action)) {
			Uri uri = intent.getData();
			if (uri == null) {
				Toast.makeText(this, R.string.search_error_no_data, Toast.LENGTH_LONG).show();
				finish();
			} else {
				GameActivity.start(this, Games.getGameId(uri), "");
			}
		} else if (action != null &&
			(Intent.ACTION_SEARCH.equals(action) || "com.google.android.gms.actions.SEARCH_ACTION".equals(action))) {
			searchText = "";
			if (intent.hasExtra(SearchManager.QUERY)) {
				searchText = intent.getExtras().getString(SearchManager.QUERY);
			}
			final ActionBar actionBar = getSupportActionBar();
			if (actionBar != null) {
				actionBar.setSubtitle(String.format(getResources().getString(R.string.search_searching), searchText));
			}
		}
	}

	@Override
	protected int getDrawerResId() {
		return R.string.title_search;
	}
}
