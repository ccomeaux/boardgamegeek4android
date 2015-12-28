package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.HelpUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.UIUtils;

public class HomeActivity extends TopLevelActivity {
	private static final int HELP_VERSION = 2;
	private static final String TAG_SINGLE_PANE = "single_pane";
	private Fragment fragment;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (Authenticator.isSignedIn(this)) {
			if (Authenticator.isOldAuth(this)) {
				Authenticator.signOut(this);
			} else if (startUserActivity()) {
				return;
			}
		}

		if (savedInstanceState == null) {
			fragment = new TextFragment();
			getIntent().putExtra(ActivityUtils.KEY_TEXT, getString(R.string.welcome_text));
			Bundle arguments = UIUtils.intentToFragmentArguments(getIntent());
			fragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction().add(R.id.root_container, fragment, TAG_SINGLE_PANE).commit();
		} else {
			fragment = getSupportFragmentManager().findFragmentByTag(TAG_SINGLE_PANE);
		}

		setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
		HelpUtils.showHelpDialog(this, HelpUtils.HELP_HOME_KEY, HELP_VERSION, R.string.help_home);
	}

	@Override
	protected void onSignInSuccess() {
		super.onSignInSuccess();
		startUserActivity();
	}

	private boolean startUserActivity() {
		Intent intent = null;
		String[] statuses = PreferencesUtils.getSyncStatuses(this);
		if (statuses != null && statuses.length > 0) {
			intent = new Intent(this, CollectionActivity.class);
		} else if (PreferencesUtils.getSyncPlays(this)) {
			intent = new Intent(this, PlaysActivity.class);
		} else if (PreferencesUtils.getSyncBuddies(this)) {
			intent = new Intent(this, BuddiesActivity.class);
		}
		if (intent != null) {
			startActivity(intent);
			finish();
			return true;
		}
		return false;
	}
}