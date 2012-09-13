package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.widget.SearchView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.DetachableResultReceiver;
import com.boardgamegeek.util.UIUtils;
import com.boardgamegeek.util.VersionUtils;

public class HomeActivity extends SherlockFragmentActivity {
	private SyncStatusUpdaterFragment mSyncStatusUpdaterFragment;
	private Menu mOptionsMenu;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		UIUtils.allowTypeToSearch(this);

		FragmentManager fm = getSupportFragmentManager();
		mSyncStatusUpdaterFragment = (SyncStatusUpdaterFragment) fm.findFragmentByTag(SyncStatusUpdaterFragment.TAG);
		if (mSyncStatusUpdaterFragment == null) {
			mSyncStatusUpdaterFragment = new SyncStatusUpdaterFragment();
			fm.beginTransaction().add(mSyncStatusUpdaterFragment, SyncStatusUpdaterFragment.TAG).commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		mOptionsMenu = menu;
		updateRefreshStatus(mSyncStatusUpdaterFragment.mSyncing);
		getSupportMenuInflater().inflate(R.menu.home, menu);
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
			case R.id.menu_search:
				if (!VersionUtils.hasHoneycomb()) {
					onSearchRequested();
					return true;
				}
				break;
			case R.id.menu_refresh:
				Intent intent = new Intent(Intent.ACTION_SYNC, null, this, SyncService.class);
				intent.putExtra(SyncService.EXTRA_STATUS_RECEIVER, mSyncStatusUpdaterFragment.mReceiver);
				startService(intent);
				return true;
			case R.id.menu_contact_us:
				Intent emailIntent = new Intent(Intent.ACTION_SEND);
				emailIntent.setType("text/plain");
				emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { "bgg4android@gmail.com" });
				emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback");
				startActivity(emailIntent);
				return true;
			case R.id.menu_about:
				startActivity(new Intent(this, AboutActivity.class));
				return true;
		}
		return false;
	}

	private void updateRefreshStatus(boolean refreshing) {
		if (mOptionsMenu == null) {
			return;
		}

		final MenuItem refreshItem = mOptionsMenu.findItem(R.id.menu_refresh);
		if (refreshItem != null) {
			if (!refreshing) {
				long time = BggApplication.getInstance().getSyncTimestamp();
				int hoursSinceSync = DateTimeUtils.howManyHoursOld(time);
				refreshing = hoursSinceSync == 0;
			}
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
			HomeActivity activity = (HomeActivity) getActivity();
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

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			// TODO: the menu hasn't been created yet, so the status is never updated
			((HomeActivity) getActivity()).updateRefreshStatus(mSyncing);
		}
	}
}