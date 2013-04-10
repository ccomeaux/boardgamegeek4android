package com.boardgamegeek.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.boardgamegeek.R;
import com.boardgamegeek.util.UIUtils;

public class BuddyActivity extends BaseActivity {
	private static final String TAG_INFO = "info";
	private static final String TAG_COLLECTION = "collection";

	private Fragment mInfoFragment;
	private Fragment mCollectionFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.activity_buddy);

		if (savedInstanceState == null) {
			mInfoFragment = new BuddyFragment();
			mInfoFragment.setArguments(UIUtils.intentToFragmentArguments(getIntent()));
			getSupportFragmentManager().beginTransaction().add(R.id.root_container, mInfoFragment, TAG_INFO).commit();

			mCollectionFragment = new BuddyCollectionFragment();
			mCollectionFragment.setArguments(UIUtils.intentToFragmentArguments(getIntent()));
			getSupportFragmentManager().beginTransaction()
				.add(R.id.root_container, mCollectionFragment, TAG_COLLECTION).commit();
		} else {
			mInfoFragment = getSupportFragmentManager().findFragmentByTag(TAG_INFO);
			mCollectionFragment = getSupportFragmentManager().findFragmentByTag(TAG_COLLECTION);
		}
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.search_only;
	}
}
