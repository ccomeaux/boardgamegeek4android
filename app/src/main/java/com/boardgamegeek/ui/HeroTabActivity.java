package com.boardgamegeek.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.boardgamegeek.R;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import androidx.appcompat.app.ActionBar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;

/**
 * A navigation drawer activity that displays a hero image over a view pager.
 */
public abstract class HeroTabActivity extends DrawerActivity {
	@BindView(R.id.coordinator) CoordinatorLayout coordinator;
	@BindView(R.id.collapsing_toolbar) CollapsingToolbarLayout collapsingToolbar;
	@BindView(R.id.toolbar_image) ImageView toolbarImage;
	@BindView(R.id.scrim) View scrimView;
	@BindView(R.id.tab_layout) TabLayout tabLayout;
	@BindView(R.id.viewpager) ViewPager viewPager;
	@BindView(R.id.fab) FloatingActionButton fab;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}

	protected void initializeViewPager() {
		setUpViewPager();
		tabLayout.setupWithViewPager(viewPager);
	}

	protected abstract void setUpViewPager();

	@Override
	protected int getLayoutResId() {
		return R.layout.activity_hero_tab;
	}

	protected void safelySetTitle(String title) {
		if (!TextUtils.isEmpty(title)) {
			collapsingToolbar.setTitle(title);
		}
	}
}
