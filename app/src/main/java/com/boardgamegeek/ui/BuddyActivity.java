package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.PagerAdapter;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.service.UpdateService;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DetachableResultReceiver;
import com.boardgamegeek.util.UIUtils;

import timber.log.Timber;

public class BuddyActivity extends PagedDrawerActivity implements BuddyFragment.Callbacks,
	BuddyCollectionFragment.Callbacks, PlaysFragment.Callbacks {
	private static final int[] TAB_TEXT_RES_IDS = new int[] { R.string.title_info, R.string.title_collection,
		R.string.title_plays };
	private SyncStatusUpdaterFragment mSyncStatusUpdaterFragment;
	private Menu mOptionsMenu;
	private BuddyFragment mBuddyFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setSubtitle(getIntent().getStringExtra(ActivityUtils.KEY_BUDDY_NAME));

		FragmentManager fm = getSupportFragmentManager();
		mSyncStatusUpdaterFragment = (SyncStatusUpdaterFragment) fm.findFragmentByTag(SyncStatusUpdaterFragment.TAG);
		if (mSyncStatusUpdaterFragment == null) {
			mSyncStatusUpdaterFragment = new SyncStatusUpdaterFragment();
			fm.beginTransaction().add(mSyncStatusUpdaterFragment, SyncStatusUpdaterFragment.TAG).commit();
		}
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
		updateRefreshStatus(mSyncStatusUpdaterFragment.mSyncing);
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
				case 1:
					fragment = new BuddyCollectionFragment();
					break;
				case 2:
					fragment = new PlaysFragment();
					bundle.putInt(PlaysFragment.KEY_MODE, PlaysFragment.MODE_BUDDY);
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

	@Override
	public void onCollectionStatusChanged(String status) {
		String text = getString(R.string.title_collection);
		if (!TextUtils.isEmpty(status)) {
			text += " - " + status;
		}
		getSupportActionBar().getTabAt(1).setText(text);
	}

	@Override
	public boolean onPlaySelected(int playId, int gameId, String gameName, String thumbnailUrl, String imageUrl) {
		ActivityUtils.startPlayActivity(this, playId, gameId, gameName, thumbnailUrl, imageUrl);
		return false;
	}

	@Override
	public void onPlayCountChanged(int count) {
	}

	@Override
	public void onSortChanged(String sortName) {
		// sorting not supported yet
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
				MenuItemCompat.setActionView(refreshItem, R.layout.actionbar_indeterminate_progress);
			} else {
				MenuItemCompat.setActionView(refreshItem, null);
			}
		}
	}

	public static class SyncStatusUpdaterFragment extends Fragment implements DetachableResultReceiver.Receiver {
		private static final String TAG = SyncStatusUpdaterFragment.class.toString();
		private boolean mSyncing = false;
		private DetachableResultReceiver mReceiver;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
			mReceiver = new DetachableResultReceiver(this);
		}

		/**
		 * {@inheritDoc}
		 */
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
						Timber.w("Received unexpected result: " + error);
						Toast.makeText(activity, error, Toast.LENGTH_LONG).show();
					}
					break;
				}
			}

			activity.updateRefreshStatus(mSyncing);
		}
	}
}
