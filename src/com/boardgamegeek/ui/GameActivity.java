package com.boardgamegeek.ui;

import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.widget.SearchView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.UIUtils;
import com.boardgamegeek.util.VersionUtils;

public class GameActivity extends SherlockFragmentActivity implements ActionBar.TabListener,
	ViewPager.OnPageChangeListener {

	public static final String KEY_GAME_NAME = "GAME_NAME";

	private int mGameId;
	private String mGameName;
	private ViewPager mViewPager;
	private GameInfoFragment mGameInfoFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		mGameId = Games.getGameId(getIntent().getData());
		mGameName = getIntent().getExtras().getString(KEY_GAME_NAME);

		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(new GamePagerAdapter(getSupportFragmentManager()));
		mViewPager.setOnPageChangeListener(this);
		mViewPager.setPageMarginDrawable(R.drawable.grey_border_inset_lr);
		mViewPager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.page_margin_width));

		final ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.addTab(actionBar.newTab().setText(R.string.tab_title_info).setTabListener(this));
		actionBar.addTab(actionBar.newTab().setText(R.string.title_forum).setTabListener(this));
		actionBar.addTab(actionBar.newTab().setText(R.string.title_comments).setTabListener(this));
		actionBar.addTab(actionBar.newTab().setText(R.string.title_plays).setTabListener(this));
		actionBar.addTab(actionBar.newTab().setText(R.string.title_colors).setTabListener(this));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getSupportMenuInflater().inflate(R.menu.game, menu);
		setupSearchMenuItem(menu);
		return true;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupSearchMenuItem(Menu menu) {
		MenuItem searchItem = menu.findItem(R.id.menu_search);
		if (searchItem != null && VersionUtils.hasHoneycomb()) {
			SearchView searchView = (SearchView) searchItem.getActionView();
			if (searchView != null) {
				SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
				searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				NavUtils.navigateUpFromSameTask(this);
				return true;
			case R.id.menu_search:
				if (!VersionUtils.hasHoneycomb()) {
					onSearchRequested();
					return true;
				}
				break;
			case R.id.menu_share:
				ActivityUtils.shareGame(this, mGameId, mGameName);
				return true;
			case R.id.menu_shortcut:
				Intent shortcut = ActivityUtils.createShortcut(this, mGameId, mGameName);
				sendBroadcast(shortcut);
				return true;
			case R.id.log_play:
				ActivityUtils.logPlay(this, false, mGameId, mGameName);
				return true;
			case R.id.log_play_quick:
				ActivityUtils.logPlay(this, true, mGameId, mGameName);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
	}

	@Override
	public void onPageScrollStateChanged(int arg0) {
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
	}

	@Override
	public void onPageSelected(int position) {
		getSupportActionBar().setSelectedNavigationItem(position);
	}

	private class GamePagerAdapter extends FragmentPagerAdapter {

		public GamePagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
				case 0:
					if (mGameInfoFragment == null) {
						mGameInfoFragment = new GameInfoFragment();
						mGameInfoFragment.setArguments(UIUtils.intentToFragmentArguments(getIntent()));
					}
					return mGameInfoFragment;
			}
			return null;
		}

		@Override
		public int getCount() {
			return 1;
		}
	}
}
