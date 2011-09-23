package com.boardgamegeek.ui;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.pref.Preferences;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.DetachableResultReceiver;
import com.boardgamegeek.util.UIUtils;

public class HomeActivity extends Activity implements DetachableResultReceiver.Receiver {
	private final static String TAG = "HomeActivity";

	private State mState;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);

		Log.d(TAG, getIntent().toString());

		mState = (State) getLastNonConfigurationInstance();
		if (mState == null) {
			mState = new State();
		}
		mState.mReceiver.setReceiver(this);
		updateUiForSync();

		UIUtils.allowTypeToSearch(this);
		UIUtils.setTitle(this);

		if (Intent.ACTION_SYNC.equals(getIntent().getAction())) {
			NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			nm.cancelAll();
		}
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

	public void onSyncClick(View v) {
		final Intent intent = new Intent(Intent.ACTION_SYNC, null, this, SyncService.class);
		intent.putExtra(SyncService.EXTRA_STATUS_RECEIVER, mState.mReceiver);
		startService(intent);
	}

	public void onSettingsClick(View v) {
		startActivity(new Intent(this, Preferences.class));
	}

	public void onAboutClick(View v) {
		startActivity(new Intent(this, AboutActivity.class));
	}

	public void onReceiveResult(int resultCode, Bundle resultData) {
		switch (resultCode) {
			case SyncService.STATUS_RUNNING:
				mState.mSyncing = true;
				updateUiForSync();
				break;
			case SyncService.STATUS_COMPLETE:
				mState.mSyncing = false;
				updateUiForSync();
				break;
			case SyncService.STATUS_ERROR:
				mState.mSyncing = false;
				updateUiForSync();
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

	private void updateUiForSync() {
		findViewById(R.id.home_btn_sync).setEnabled(!mState.mSyncing);
	}

	private static class State {
		public boolean mSyncing = false;
		public DetachableResultReceiver mReceiver;

		private State() {
			mReceiver = new DetachableResultReceiver(new Handler());
		}
	}
}