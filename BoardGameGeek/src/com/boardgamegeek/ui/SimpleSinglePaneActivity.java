package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.boardgamegeek.R;
import com.boardgamegeek.util.UIUtils;

public abstract class SimpleSinglePaneActivity extends DrawerActivity {
	private static final String TAG_SINGLE_PANE = "single_pane";
	protected Fragment mFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		if (savedInstanceState == null) {
			mFragment = onCreatePane(getIntent());
			if (mFragment != null) {
				mFragment.setArguments(UIUtils.intentToFragmentArguments(getIntent()));
				getSupportFragmentManager().beginTransaction().add(R.id.root_container, mFragment, TAG_SINGLE_PANE)
					.commit();
			}
		} else {
			mFragment = getSupportFragmentManager().findFragmentByTag(TAG_SINGLE_PANE);
		}
	}

	@Override
	protected int getContentViewId() {
		return R.layout.activity_singlepane_empty;
	}

	/**
	 * Called in <code>onCreate</code> when the fragment constituting this activity is needed. The returned fragment's
	 * arguments will be set to the intent used to invoke this activity.
	 */
	protected abstract Fragment onCreatePane(Intent intent);

	public Fragment getFragment() {
		return mFragment;
	}
}
