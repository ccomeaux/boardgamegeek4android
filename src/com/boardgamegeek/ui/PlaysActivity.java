package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.DetachableResultReceiver;

public class PlaysActivity extends SimpleSinglePaneActivity implements PlaysFragment.Callbacks,
	ActionBar.OnNavigationListener {
	private SyncStatusUpdaterFragment mSyncStatusUpdaterFragment;
	private Menu mOptionsMenu;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		ArrayAdapter<CharSequence> mSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.plays_filter,
			R.layout.sherlock_spinner_item);
		mSpinnerAdapter.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
		actionBar.setListNavigationCallbacks(mSpinnerAdapter, this);

		FragmentManager fm = getSupportFragmentManager();
		mSyncStatusUpdaterFragment = (SyncStatusUpdaterFragment) fm.findFragmentByTag(SyncStatusUpdaterFragment.TAG);
		if (mSyncStatusUpdaterFragment == null) {
			mSyncStatusUpdaterFragment = new SyncStatusUpdaterFragment();
			fm.beginTransaction().add(mSyncStatusUpdaterFragment, SyncStatusUpdaterFragment.TAG).commit();
		}

		if (DateTimeUtils.howManyHoursOld(BggApplication.getInstance().getLastPlaysSync()) > 2) {
			BggApplication.getInstance().putLastPlaysSync();
			((PlaysFragment) mFragment).triggerRefresh();
		}
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		int filter = Play.SYNC_STATUS_ALL;
		switch (itemPosition) {
			case 1:
				filter = Play.SYNC_STATUS_SYNCED;
				break;
			case 2:
				filter = Play.SYNC_STATUS_IN_PROGRESS;
				break;
			case 3:
				filter = Play.SYNC_STATUS_PENDING_UPDATE;
				break;
			case 4:
				filter = Play.SYNC_STATUS_PENDING_DELETE;
				break;
		}
		((PlaysFragment) mFragment).filter(filter);
		return true;
	}

	@Override
	protected Fragment onCreatePane() {
		return new PlaysFragment();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		mOptionsMenu = menu;
		updateRefreshStatus(mSyncStatusUpdaterFragment.mSyncing);
		return true;
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.plays;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_refresh:
				((PlaysFragment) mFragment).triggerRefresh();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public DetachableResultReceiver getReceiver() {
		return mSyncStatusUpdaterFragment.mReceiver;
	}

	public void updateRefreshStatus(boolean refreshing) {
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
			PlaysActivity activity = (PlaysActivity) getActivity();
			if (activity == null) {
				return;
			}

			switch (resultCode) {
				case SyncService.STATUS_RUNNING: {
					mSyncing = true;
					break;
				}
				case SyncService.STATUS_COMPLETE: {
					mSyncing = false;
					break;
				}
				case SyncService.STATUS_ERROR:
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

	// @Override
	// protected void onResume() {
	// super.onResume();
	// mLogInHelper.logIn();
	// }
	//
	// @Override
	// public void onLogInSuccess() {
	// // do nothing
	// }
	//
	// @Override
	// public void onLogInError(String errorMessage) {
	// Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
	// }
	//
	// @Override
	// public void onNeedCredentials() {
	// Toast.makeText(this, R.string.setUsernamePassword, Toast.LENGTH_LONG).show();
	// }
}
