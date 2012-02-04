package com.boardgamegeek.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.pref.Preferences;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.DetachableResultReceiver;
import com.boardgamegeek.util.UIUtils;

public class HomeActivity extends Activity implements DetachableResultReceiver.Receiver {
	private final static String TAG = "HomeActivity";

	private State mState;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		UIUtils.allowTypeToSearch(this);
		UIUtils.setTitle(this);

		mState = (State) getLastNonConfigurationInstance();
		if (mState == null) {
			mState = new State();
		}
		mState.mReceiver.setReceiver(this);
	}

	@Override
	protected void onResume() {
		super.onResume();

		findViewById(R.id.home_btn_collection).setVisibility(
				(BggApplication.getInstance().getSyncStatuses() != null && BggApplication.getInstance()
						.getSyncStatuses().length > 0) ? View.VISIBLE : View.GONE);
		findViewById(R.id.home_btn_buddies).setVisibility(
				BggApplication.getInstance().getSyncBuddies() ? View.VISIBLE : View.GONE);
		findViewById(R.id.home_btn_plays).setVisibility(
				BggApplication.getInstance().getSyncPlays() ? View.VISIBLE : View.GONE);
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		mState.mReceiver.clearReceiver();
		return mState;
	}

	@Override
	public void setTitle(CharSequence title) {
		UIUtils.setTitle(this, title);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		final MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.home, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem mi = menu.findItem(R.id.home_btn_sync);
		if (mi != null) {
			if (!mState.mSyncing) {
				long time = BggApplication.getInstance().getSyncTimestamp();
				int d = DateTimeUtils.howManyHoursOld(time);
				boolean b = d == 0;
				mState.mSyncing = b;
			}
			mi.setEnabled(!mState.mSyncing);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.home_btn_sync:
				Intent intent = new Intent(Intent.ACTION_SYNC, null, this, SyncService.class);
				intent.putExtra(SyncService.EXTRA_STATUS_RECEIVER, mState.mReceiver);
				startService(intent);
				return true;
			case R.id.home_btn_about:
				startActivity(new Intent(this, AboutActivity.class));
				return true;
		}
		return false;
	}

	public void onHomeClick(View v) {
		// do nothing; we're already home
	}

	public void onSearchClick(View v) {
		onSearchRequested();
	}

	public void onCollectionClick(View v) {
		final Intent intent = new Intent(Intent.ACTION_VIEW, Collection.CONTENT_URI);
		startActivity(intent);
	}

	public void onHotnessClick(View v) {
		final Intent intent = new Intent(this, HotnessActivity.class);
		startActivity(intent);
	}

	public void onForumsClick(View v) {
		Intent forumsIntent = new Intent(this, ForumlistActivity.class);
		forumsIntent.putExtra(ForumlistActivity.KEY_FORUMLIST_ID, 0);
		forumsIntent.putExtra(ForumlistActivity.KEY_THUMBNAIL_URL, "");
		forumsIntent.putExtra(ForumlistActivity.KEY_GAME_NAME, "");
		startActivity(forumsIntent);
	}

	public void onBuddiesClick(View v) {
		final Intent intent = new Intent(Intent.ACTION_VIEW, Buddies.CONTENT_URI);
		startActivity(intent);
	}

	public void onPlaysClick(View v) {
		final Intent intent = new Intent(Intent.ACTION_VIEW, Plays.CONTENT_URI);
		startActivity(intent);
	}

	public void onSettingsClick(View v) {
		startActivity(new Intent(this, Preferences.class));
	}

	public void onReceiveResult(int resultCode, Bundle resultData) {
		switch (resultCode) {
			case SyncService.STATUS_RUNNING:
				mState.mSyncing = true;
				break;
			case SyncService.STATUS_COMPLETE:
				mState.mSyncing = false;
				break;
			case SyncService.STATUS_ERROR:
				mState.mSyncing = false;
				final String error = resultData.getString(Intent.EXTRA_TEXT);
				if (error != null) {
					Toast.makeText(this, error, Toast.LENGTH_LONG).show();
				}
				break;
			default:
				Log.w(TAG, "Received unexpected result: " + resultCode);
				break;
		}
	}

	private static class State {
		public boolean mSyncing = false;
		public DetachableResultReceiver mReceiver;

		private State() {
			mReceiver = new DetachableResultReceiver(new Handler());
		}
	}
}