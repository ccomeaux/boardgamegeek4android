package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.boardgamegeek.R;
import com.boardgamegeek.util.UIUtils;

public class GameActivity extends SherlockFragmentActivity implements ActionBar.TabListener,
	ViewPager.OnPageChangeListener {
	private static final String TAG = makeLogTag(GameActivity.class);

	private ViewPager mViewPager;
	private GameInfoFragment mGameInfoFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game);

		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(new GamePagerAdapter(getSupportFragmentManager()));
		mViewPager.setOnPageChangeListener(this);
		mViewPager.setPageMarginDrawable(R.drawable.grey_border_inset_lr);
		mViewPager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.page_margin_width));

		final ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.addTab(actionBar.newTab().setText(R.string.tab_title_info).setTabListener(this));
		actionBar.addTab(actionBar.newTab().setText(R.string.tab_title_stats).setTabListener(this));
		actionBar.addTab(actionBar.newTab().setText(R.string.tab_title_lists).setTabListener(this));
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
				case 1:
					return new GameInfoFragment();
				case 2:
					return new GameInfoFragment();
			}
			return null;
		}

		@Override
		public int getCount() {
			return 3;
		}
	}
}
