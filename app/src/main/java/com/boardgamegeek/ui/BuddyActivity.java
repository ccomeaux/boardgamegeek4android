package com.boardgamegeek.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.PagerAdapter;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.events.BuddySelectedEvent;
import com.boardgamegeek.events.UpdateCompleteEvent;
import com.boardgamegeek.events.UpdateEvent;
import com.boardgamegeek.service.UpdateService;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.UIUtils;

import de.greenrobot.event.EventBus;

public class BuddyActivity extends PagedDrawerActivity {
	private static final int[] TAB_TEXT_RES_IDS = new int[] { R.string.title_info };
	private boolean mSyncing = false;
	private Menu mOptionsMenu;
	private BuddyFragment mBuddyFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setSubtitle(getIntent().getStringExtra(ActivityUtils.KEY_BUDDY_NAME));

		EventBus.getDefault().removeStickyEvent(BuddySelectedEvent.class);
	}

	@Override
	protected PagerAdapter getAdapter(FragmentManager fm) {
		return new BuddyPagerAdapter(fm);
	}

	@Override
	protected int[] getTabTextResIds() {
		return TAB_TEXT_RES_IDS;
	}

	@Override
	protected int getContentViewId() {
		return R.layout.activity_viewpager;
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.refresh_only;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		mOptionsMenu = menu;
		updateRefreshStatus();
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_refresh:
				if (mBuddyFragment != null) {
					mBuddyFragment.forceRefresh();
				}
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void onEventMainThread(UpdateEvent event) {
		if (event.type == UpdateService.SYNC_TYPE_BUDDY) {
			mSyncing = true;
		} else {
			mSyncing = false;
		}
		updateRefreshStatus();
	}

	public void onEventMainThread(UpdateCompleteEvent event) {
		mSyncing = false;
		updateRefreshStatus();
	}

	private class BuddyPagerAdapter extends FragmentPagerAdapter {

		public BuddyPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			Fragment fragment = null;
			Bundle bundle = UIUtils.intentToFragmentArguments(getIntent());
			switch (position) {
				case 0:
					mBuddyFragment = new BuddyFragment();
					fragment = mBuddyFragment;
					break;
			}
			if (fragment != null) {
				fragment.setArguments(bundle);
			}
			return fragment;
		}

		@Override
		public int getCount() {
			return TAB_TEXT_RES_IDS.length;
		}
	}

	private void updateRefreshStatus() {
		if (mOptionsMenu == null) {
			return;
		}

		final MenuItem refreshItem = mOptionsMenu.findItem(R.id.menu_refresh);
		if (refreshItem != null) {
			if (mSyncing) {
				MenuItemCompat.setActionView(refreshItem, R.layout.actionbar_indeterminate_progress);
			} else {
				MenuItemCompat.setActionView(refreshItem, null);
			}
		}
	}
}
