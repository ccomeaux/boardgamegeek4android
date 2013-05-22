package com.boardgamegeek.ui;

import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.HelpUtils;
import com.boardgamegeek.util.UIUtils;

public class SearchResultsActivity extends SimpleSinglePaneActivity implements SearchResultsFragment.Callbacks {
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

		UIUtils.showHelpDialog(this, HelpUtils.HELP_SEARCHRESULTS_KEY, HELP_VERSION, R.string.help_searchresults);
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
	public void onResultCount(int subtitle) {
		String message = String.format(getResources().getString(R.string.search_results), subtitle, mSearchText);
		getSupportActionBar().setSubtitle(message);
	}

	@Override
	public void onExactMatch() {
		finish();
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		Fragment fragment = null;
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			mSearchText = intent.getExtras().getString(SearchManager.QUERY);
			if (TextUtils.isEmpty(mSearchText)) {
				fragment = buildTextFragment(getString(R.string.search_error_no_text));
			} else {
				getSupportActionBar().setSubtitle(
					String.format(getResources().getString(R.string.search_searching), mSearchText));
				fragment = new SearchResultsFragment();
			}
		} else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			Uri uri = intent.getData();
			if (uri == null) {
				fragment = buildTextFragment(getString(R.string.search_error_no_data));
			} else {
				ActivityUtils.launchGame(this, Games.getGameId(uri), "");
				finish();
				return null;
			}
		} else {
			fragment = buildTextFragment(getString(R.string.search_error_bad_intent) + intent.getAction());
		}
		return fragment;
	}

	private Fragment buildTextFragment(String text) {
		mFragment = new TextFragment();
		getIntent().putExtra(TextFragment.KEY_TEXT, text);
		return mFragment;
	}
}
