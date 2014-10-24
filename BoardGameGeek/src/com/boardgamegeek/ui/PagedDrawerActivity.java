package com.boardgamegeek.ui;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;

import com.boardgamegeek.R;

public abstract class PagedDrawerActivity extends DrawerActivity implements ActionBar.TabListener,
	ViewPager.OnPageChangeListener {

	private ViewPager mViewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(getAdapter(getSupportFragmentManager()));
		mViewPager.setOnPageChangeListener(this);
		mViewPager.setPageMarginDrawable(R.drawable.grey_border_inset_lr);
		mViewPager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.page_margin_width));

		final ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		setupActionBarTabs(actionBar);
	}

	protected abstract PagerAdapter getAdapter(FragmentManager fm);

	protected void setupActionBarTabs(ActionBar actionBar) {
		int[] tabTextResIds = getTabTextResIds();
		if (tabTextResIds != null) {
			for (int textId : tabTextResIds) {
				createTab(actionBar, textId);
			}
		}
	}

	protected int[] getTabTextResIds() {
		return null;
	}

	protected void createTab(ActionBar actionBar, int textId) {
		Tab tab = actionBar.newTab().setText(textId).setTabListener(this);
		actionBar.addTab(tab);
	}

	@Override
	public void onPageScrollStateChanged(int state) {
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
	}

	@Override
	public void onPageSelected(int position) {
		getSupportActionBar().setSelectedNavigationItem(position);
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

	protected void changeTab(final int item) {
		mViewPager.postDelayed(new Runnable() {
			@Override
			public void run() {
				// HACK prevent a blank fragment if this page is already selected
				mViewPager.setCurrentItem(item);
			}
		}, 100);
	}

	protected void updateTabs() {
		mViewPager.getAdapter().notifyDataSetChanged();
		setupActionBarTabs(getSupportActionBar());
	}
}