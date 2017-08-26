package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;

import com.boardgamegeek.R;
import com.boardgamegeek.util.UIUtils;

/**
 * A non-top-level DrawerActivity that supports a single pane.
 */
public abstract class SimpleSinglePaneActivity extends DrawerActivity {
	private static final String TAG_SINGLE_PANE = "single_pane";
	private Fragment fragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		readIntent(getIntent());

		if (savedInstanceState == null) {
			createFragment();
		} else {
			fragment = getSupportFragmentManager().findFragmentByTag(TAG_SINGLE_PANE);
		}
	}

	protected void readIntent(Intent intent) {
	}

	protected void createFragment() {
		fragment = onCreatePane(getIntent());
		if (fragment != null) {
			getSupportFragmentManager()
				.beginTransaction()
				.add(R.id.root_container, fragment, TAG_SINGLE_PANE)
				.commit();
		}
	}

	protected void recreateFragment() {
		getSupportFragmentManager().beginTransaction().remove(getFragment()).commit();
		createFragment();
	}

	/**
	 * Called in <code>onCreate</code> when the fragment constituting this activity is needed. The returned fragment's
	 * arguments will be set to the intent used to invoke this activity.
	 */
	protected abstract Fragment onCreatePane(Intent intent);

	public Fragment getFragment() {
		return fragment;
	}

	protected Bundle onBeforeArgumentsSet(Bundle arguments) {
		return arguments;
	}
}
