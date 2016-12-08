package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.ScrimUtils;
import com.boardgamegeek.util.UIUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

public class GeekListItemActivity extends HeroTabActivity {
	private int geekListId;
	private String geekListTitle;
	private int objectId;
	private String objectName;
	private String url;
	private boolean isBoardGame;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		geekListTitle = intent.getStringExtra(ActivityUtils.KEY_TITLE);
		geekListId = intent.getIntExtra(ActivityUtils.KEY_ID, BggContract.INVALID_ID);
		objectId = intent.getIntExtra(ActivityUtils.KEY_OBJECT_ID, BggContract.INVALID_ID);
		objectName = intent.getStringExtra(ActivityUtils.KEY_NAME);
		url = intent.getStringExtra(ActivityUtils.KEY_OBJECT_URL);
		isBoardGame = intent.getBooleanExtra(ActivityUtils.KEY_IS_BOARD_GAME, false);
		int imageId = intent.getIntExtra(ActivityUtils.KEY_IMAGE_ID, BggContract.INVALID_ID);

		safelySetTitle(objectName);

		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent()
				.putContentType("GeekListItem")
				.putContentId(String.valueOf(objectId))
				.putContentName(objectName));
		}

		ScrimUtils.applyInvertedScrim(scrimView);
		ImageUtils.safelyLoadImage(toolbarImage, imageId, null);
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.view;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				if (geekListId != BggContract.INVALID_ID) {
					Intent intent = new Intent(this, GeekListActivity.class);
					intent.putExtra(ActivityUtils.KEY_ID, geekListId);
					intent.putExtra(ActivityUtils.KEY_TITLE, geekListTitle);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
					finish();
				} else {
					onBackPressed();
				}
				return true;
			case R.id.menu_view:
				if (isBoardGame) {
					ActivityUtils.launchGame(this, objectId, objectName);
				} else {
					ActivityUtils.link(this, url);
				}
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void setUpViewPager() {
		GeekListItemPagerAdapter adapter = new GeekListItemPagerAdapter(getSupportFragmentManager());
		viewPager.setAdapter(adapter);
	}

	private final class GeekListItemPagerAdapter extends FragmentPagerAdapter {
		public GeekListItemPagerAdapter(FragmentManager fragmentManager) {
			super(fragmentManager);
		}

		@Override
		public CharSequence getPageTitle(int position) {
			if (position == 0) return getString(R.string.title_description);
			if (position == 1) return getString(R.string.title_comments);
			return "";
		}

		@Override
		public Fragment getItem(int position) {
			if (position == 0) {
				return Fragment.instantiate(
					GeekListItemActivity.this,
					GeekListItemFragment.class.getName(),
					UIUtils.intentToFragmentArguments(getIntent()));
			}
			if (position == 1) {
				return Fragment.instantiate(
					GeekListItemActivity.this,
					GeekListCommentsFragment.class.getName(),
					UIUtils.intentToFragmentArguments(getIntent()));
			}
			return null;
		}

		@Override
		public int getCount() {
			return 2;
		}
	}
}
