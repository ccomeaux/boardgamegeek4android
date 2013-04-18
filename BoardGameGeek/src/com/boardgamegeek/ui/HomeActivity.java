package com.boardgamegeek.ui;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.HelpUtils;
import com.boardgamegeek.util.UIUtils;

public class HomeActivity extends SherlockFragmentActivity {
	private static final int HELP_VERSION = 2;
	private Menu mOptionsMenu;
	private Object mSyncObserverHandle;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
		UIUtils.showHelpDialog(this, HelpUtils.HELP_HOME_KEY, HELP_VERSION, R.string.help_home);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mSyncObserverHandle != null) {
			ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
			mSyncObserverHandle = null;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		mSyncStatusObserver.onStatusChanged(0);
		mSyncObserverHandle = ContentResolver.addStatusChangeListener(ContentResolver.SYNC_OBSERVER_TYPE_PENDING
			| ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE, mSyncStatusObserver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		mOptionsMenu = menu;
		getSupportMenuInflater().inflate(R.menu.home, menu);
		setupSearchMenuItem(menu);
		mSyncStatusObserver.onStatusChanged(0);
		return true;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
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
	public boolean onPrepareOptionsMenu(Menu menu) {
		AccountManager am = AccountManager.get(this);
		Account account = Authenticator.getAccount(am);
		menu.findItem(R.id.menu_sign_out).setVisible(account != null && !TextUtils.isEmpty(am.getPassword(account)));
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_refresh:
				triggerRefresh();
				return true;
			case R.id.menu_sign_out:
				ActivityUtils.createConfirmationDialog(this, R.string.are_you_sure_sign_out,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							Authenticator.signOut(HomeActivity.this);
						}
					}).show();
				return true;
			case R.id.menu_contact_us:
				Intent emailIntent = new Intent(Intent.ACTION_SEND);
				emailIntent.setType("text/plain");
				emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { "bgg4android@gmail.com" });
				emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback");
				startActivity(emailIntent);
				return true;
			case R.id.menu_about:
				HelpUtils.showAboutDialog(this);
				return true;
		}
		return false;
	}

	private void triggerRefresh() {
		SyncService.sync(this, SyncService.FLAG_SYNC_ALL);
	}

	private void setRefreshActionButtonState(boolean refreshing) {
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

	private final SyncStatusObserver mSyncStatusObserver = new SyncStatusObserver() {
		@Override
		public void onStatusChanged(int which) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					setRefreshActionButtonState(SyncService.isActiveOrPending(HomeActivity.this));
				}
			});
		}
	};
}