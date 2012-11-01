package com.boardgamegeek.ui;

import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.UIUtils;

public class SearchResultsActivity extends BaseActivity implements SearchResultsFragment.Callbacks {
	private static final String TAG_SINGLE_PANE = "single_pane";
	private static final int HELP_VERSION = 1;

	private Fragment mFragment;
	private String mSearchText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.activity_singlepane_empty);
		setTitle(R.string.title_search_results);

		if (savedInstanceState == null) {
			parseIntent(getIntent());
		} else {
			mFragment = getSupportFragmentManager().findFragmentByTag(TAG_SINGLE_PANE);
		}

		UIUtils.showHelpDialog(this, BggApplication.HELP_SEARCHRESULTS_KEY, HELP_VERSION, R.string.help_searchresults);
	}

	@Override
	public void onNewIntent(Intent intent) {
		parseIntent(intent);
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

	public Fragment getFragment() {
		return mFragment;
	}

	private void parseIntent(Intent intent) {
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			mSearchText = intent.getExtras().getString(SearchManager.QUERY);
			if (TextUtils.isEmpty(mSearchText)) {
				buildTextFragment(getString(R.string.search_error_no_text));
			} else {
				getSupportActionBar().setSubtitle(
					String.format(getResources().getString(R.string.search_searching), mSearchText));
				mFragment = new SearchResultsFragment();
			}
		} else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			Uri uri = intent.getData();
			if (uri == null) {
				buildTextFragment(getString(R.string.search_error_no_data));
			} else {
				ActivityUtils.launchGame(this, Games.getGameId(uri), "");
				finish();
				return;
			}
		} else {
			buildTextFragment(getString(R.string.search_error_bad_intent) + intent.getAction());
		}
		mFragment.setArguments(UIUtils.intentToFragmentArguments(intent));
		getSupportFragmentManager().beginTransaction().add(R.id.root_container, mFragment, TAG_SINGLE_PANE).commit();
	}

	private void buildTextFragment(String text) {
		mFragment = new TextFragment();
		getIntent().putExtra(TextFragment.KEY_TEXT, text);
	}
}
