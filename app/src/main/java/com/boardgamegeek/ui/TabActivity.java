package com.boardgamegeek.ui;

import android.os.Bundle;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.google.android.material.tabs.TabLayout;

import androidx.appcompat.app.ActionBar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;

/**
 * A navigation drawer activity that displays a view pager.
 */
public abstract class TabActivity extends DrawerActivity {
	@BindView(R.id.coordinator) CoordinatorLayout coordinator;
	@BindView(R.id.tab_layout) TabLayout tabLayout;
	@BindView(R.id.viewpager) ViewPager viewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		setUpViewPager();
		tabLayout.setupWithViewPager(viewPager);
	}

	protected abstract void setUpViewPager();

	@Override
	protected int getLayoutResId() {
		return R.layout.activity_tab;
	}

	protected void safelySetTitle(String title) {
		if (!TextUtils.isEmpty(title)) {
			ActionBar actionBar = getSupportActionBar();
			if (actionBar != null) {
				actionBar.setTitle(title);
			}
		}
	}
}
