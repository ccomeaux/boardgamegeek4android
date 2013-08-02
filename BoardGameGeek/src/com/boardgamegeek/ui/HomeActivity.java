package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.HelpUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.UIUtils;

public class HomeActivity extends TopLevelActivity {
	private static final int HELP_VERSION = 2;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (Authenticator.isSignedIn(this)) {
			if (startUserActivity()) {
				return;
			}
		}

		setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
		UIUtils.showHelpDialog(this, HelpUtils.HELP_HOME_KEY, HELP_VERSION, R.string.help_home);
	}

	@Override
	protected int getContentViewId() {
		return R.layout.activity_home;
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.search_only;
	}

	@Override
	protected void onSignInSuccess() {
		startUserActivity();
	}

	private boolean startUserActivity() {
		Intent intent = null;
		String[] statuses = PreferencesUtils.getSyncStatuses(this);
		if (statuses != null && statuses.length > 0) {
			intent = new Intent(Intent.ACTION_VIEW, Collection.CONTENT_URI);
		} else if (PreferencesUtils.getSyncPlays(this)) {
			intent = new Intent(Intent.ACTION_VIEW, Plays.CONTENT_URI);
		} else if (PreferencesUtils.getSyncBuddies(this)) {
			intent = new Intent(Intent.ACTION_VIEW, Buddies.CONTENT_URI);
		}
		if (intent != null) {
			intent.putExtra(EXTRA_NAVIGATION_POSITION, 0);
			startActivity(intent);
			finish();
			return true;
		}
		return false;
	}
}