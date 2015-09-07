package com.boardgamegeek.ui;

import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.events.BuddiesCountChangedEvent;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.HelpUtils;

import hugo.weaving.DebugLog;

public class SearchResultsActivity extends SimpleSinglePaneActivity {
	private static final String VOICE_SEARCH_ACTION = "com.google.android.gms.actions.SEARCH_ACTION";
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

	@DebugLog
	public void onEvent(BuddiesCountChangedEvent event) {
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
		if (action == null) {
			fragment = buildTextFragment(getString(R.string.search_initial_help));
		} else {
			switch (action) {
				case Intent.ACTION_SEARCH:
				case VOICE_SEARCH_ACTION:
					mSearchText = intent.getExtras().getString(SearchManager.QUERY);
					if (TextUtils.isEmpty(mSearchText)) {
						fragment = buildTextFragment(getString(R.string.search_error_no_text));
					} else {
						final ActionBar actionBar = getSupportActionBar();
						if (actionBar != null) {
							actionBar.setSubtitle(String.format(getResources().getString(R.string.search_searching), mSearchText));
						}
						fragment = new SearchResultsFragment();
					}
					break;
				case Intent.ACTION_VIEW:
					Uri uri = intent.getData();
					if (uri == null) {
						fragment = buildTextFragment(getString(R.string.search_error_no_data));
					} else {
						ActivityUtils.launchGame(this, Games.getGameId(uri), "");
						finish();
						return null;
					}
					break;
				default:
					fragment = buildTextFragment(getString(R.string.search_error_bad_intent) + action);
					break;
			}
		}
		return fragment;
	}

	private Fragment buildTextFragment(String text) {
		mFragment = new TextFragment();
		getIntent().putExtra(ActivityUtils.KEY_TEXT, text);
		return mFragment;
	}
}
