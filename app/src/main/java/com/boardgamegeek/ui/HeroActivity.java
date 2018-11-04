package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.boardgamegeek.R;
import com.boardgamegeek.util.PresentationUtils;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.ActionBar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener;
import butterknife.BindView;

/**
 * A navigation drawer activity that displays a hero image.
 */
public abstract class HeroActivity extends DrawerActivity implements OnRefreshListener {
	private static final String TAG_SINGLE_PANE = "single_pane";
	private Fragment fragment;
	private boolean isRefreshing;

	@BindView(R.id.swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
	@BindView(R.id.collapsing_toolbar) CollapsingToolbarLayout collapsingToolbar;
	@BindView(R.id.toolbar_image) ImageView toolbarImage;
	@BindView(R.id.scrim) View scrimView;
	@BindView(R.id.fab) FloatingActionButton fab;

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
		if (isRefreshable()) {
			swipeRefreshLayout.setOnRefreshListener(this);
			swipeRefreshLayout.setColorSchemeResources(PresentationUtils.getColorSchemeResources());
			swipeRefreshLayout.setEnabled(true);
		} else {
			swipeRefreshLayout.setEnabled(false);
		}
	}

	protected abstract void readIntent(Intent intent);

	protected boolean isRefreshable() {
		return true;
	}

	protected void createFragment() {
		fragment = onCreatePane();
		if (fragment != null) {
			getSupportFragmentManager()
				.beginTransaction()
				.add(R.id.root_container, fragment, TAG_SINGLE_PANE)
				.commit();
		}
	}

	/**
	 * Called in <code>onCreate</code> when the fragment constituting this activity is needed. The returned fragment's
	 * arguments will be set to the intent used to invoke this activity.
	 */
	protected abstract Fragment onCreatePane();

	protected Fragment getFragment() {
		return fragment;
	}

	@Override
	protected int getLayoutResId() {
		return R.layout.activity_hero;
	}

	protected void safelySetTitle(String title) {
		if (!TextUtils.isEmpty(title)) {
			collapsingToolbar.setTitle(title);
		}
	}

	@Override
	public void onRefresh() {
		//No-op; just here for the children
	}

	protected void updateRefreshStatus(boolean refreshing) {
		this.isRefreshing = refreshing;
		if (swipeRefreshLayout != null) {
			swipeRefreshLayout.post(new Runnable() {
				@Override
				public void run() {
					swipeRefreshLayout.setRefreshing(isRefreshing);
				}
			});
		}
	}
}
