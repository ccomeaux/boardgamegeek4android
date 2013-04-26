package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.boardgamegeek.R;
import com.boardgamegeek.service.UpdateService;
import com.boardgamegeek.util.BuddyUtils;
import com.boardgamegeek.util.DetachableResultReceiver;
import com.boardgamegeek.util.UIUtils;

public class BuddyActivity extends SherlockFragmentActivity implements ActionBar.TabListener,
	ViewPager.OnPageChangeListener, BuddyFragment.Callbacks {
	private ViewPager mViewPager;
	private SyncStatusUpdaterFragment mSyncStatusUpdaterFragment;
	private Menu mOptionsMenu;
	private BuddyFragment mBuddyFragment;
	private String mName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		mName = getIntent().getStringExtra(BuddyUtils.KEY_BUDDY_NAME);
		changeName(getIntent().getStringExtra(BuddyUtils.KEY_BUDDY_FULL_NAME));

		FragmentManager fm = getSupportFragmentManager();
		mSyncStatusUpdaterFragment = (SyncStatusUpdaterFragment) fm.findFragmentByTag(SyncStatusUpdaterFragment.TAG);
		if (mSyncStatusUpdaterFragment == null) {
			mSyncStatusUpdaterFragment = new SyncStatusUpdaterFragment();
			fm.beginTransaction().add(mSyncStatusUpdaterFragment, SyncStatusUpdaterFragment.TAG).commit();
		}

		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(new BuddyPagerAdapter(getSupportFragmentManager()));
		mViewPager.setOnPageChangeListener(this);
		mViewPager.setPageMarginDrawable(R.drawable.grey_border_inset_lr);
		mViewPager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.page_margin_width));

		final ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.addTab(actionBar.newTab().setText(R.string.title_info).setTabListener(this));
		actionBar.addTab(actionBar.newTab().setText(R.string.title_collection).setTabListener(this));
		// actionBar.addTab(actionBar.newTab().setText(R.string.title_plays).setTabListener(this));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		mOptionsMenu = menu;
		updateRefreshStatus(mSyncStatusUpdaterFragment.mSyncing);
		getSupportMenuInflater().inflate(R.menu.refresh_only, menu);
		setupSearchMenuItem(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_refresh) {
			UpdateService.start(this, UpdateService.SYNC_TYPE_BUDDY, mName, getReceiver());
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void setupSearchMenuItem(Menu menu) {
		MenuItem searchItem = menu.findItem(R.id.menu_search);
		if (searchItem != null) {
			SearchView searchView = (SearchView) searchItem.getActionView();
			if (searchView != null) {
				SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
				searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
			}
		}
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

	private class BuddyPagerAdapter extends FragmentPagerAdapter {

		public BuddyPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			Fragment fragment = null;
			switch (position) {
				case 0:
					mBuddyFragment = new BuddyFragment();
					fragment = mBuddyFragment;
					break;
				case 1:
					fragment = new BuddyCollectionFragment();
					break;
			}
			if (fragment != null) {
				fragment.setArguments(UIUtils.intentToFragmentArguments(getIntent()));
			}
			return fragment;
		}

		@Override
		public int getCount() {
			return 2;
		}
	}

	private void changeName(String name) {
		if (!TextUtils.isEmpty(name)) {
			getIntent().putExtra(BuddyUtils.KEY_BUDDY_FULL_NAME, name);
			getSupportActionBar().setSubtitle(name);
		}
	}

	@Override
	public void onNameChanged(String name) {
		changeName(name);
	}

	@Override
	public DetachableResultReceiver getReceiver() {
		return mSyncStatusUpdaterFragment.mReceiver;
	}

	private void updateRefreshStatus(boolean refreshing) {
		if (mOptionsMenu == null) {
			return;
		}

		final MenuItem refreshItem = mOptionsMenu.findItem(R.id.menu_refresh);
		if (refreshItem != null) {
			if (refreshing) {
				refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
			} else {
				refreshItem.setActionView(null);
			}
		}
	}

	public static class SyncStatusUpdaterFragment extends Fragment implements DetachableResultReceiver.Receiver {
		private static final String TAG = makeLogTag(SyncStatusUpdaterFragment.class);

		private boolean mSyncing = false;
		private DetachableResultReceiver mReceiver;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
			mReceiver = new DetachableResultReceiver(new Handler());
			mReceiver.setReceiver(this);
		}

		/** {@inheritDoc} */
		public void onReceiveResult(int resultCode, Bundle resultData) {
			BuddyActivity activity = (BuddyActivity) getActivity();
			if (activity == null) {
				return;
			}

			switch (resultCode) {
				case UpdateService.STATUS_RUNNING: {
					mSyncing = true;
					break;
				}
				case UpdateService.STATUS_COMPLETE: {
					mSyncing = false;
					break;
				}
				case UpdateService.STATUS_ERROR:
				default: {
					final String error = resultData.getString(Intent.EXTRA_TEXT);
					if (error != null) {
						LOGW(TAG, "Received unexpected result: " + error);
						Toast.makeText(activity, error, Toast.LENGTH_LONG).show();
					}
					break;
				}
			}

			activity.updateRefreshStatus(mSyncing);
		}
	}
}
