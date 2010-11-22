package com.boardgamegeek.ui;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.boardgamegeek.Preferences;
import com.boardgamegeek.R;
import com.boardgamegeek.Utility;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.DetachableResultReceiver;
import com.boardgamegeek.util.UIUtils;

public class HomeActivity extends Activity implements DetachableResultReceiver.Receiver {
	private final static String TAG = "HomeActivity";
	
	private DetachableResultReceiver mReceiver;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);

		Log.d(TAG, getIntent().toString());
		
		if (Intent.ACTION_SYNC.equals(getIntent().getAction())) {
			NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			nm.cancelAll();
		}

		mReceiver = new DetachableResultReceiver(new Handler());
		mReceiver.setReceiver(this);

		UIUtils.allowTypeToSearch(this);

		UIUtils.setTitle(this);
		((TextView) findViewById(R.id.version)).setText(Utility.getVersionDescription(this));
	}
	
	@Override
	protected void onDestroy() {
		mReceiver.clearReceiver();
		super.onDestroy();
	}

	@Override
	public void setTitle(CharSequence title) {
		UIUtils.setTitle(this, title);
	}
	
	public void onHomeClick(View v){
		// do nothing; we're already home
	}

	public void onSearchClick(View v) {
		onSearchRequested();
	}

	public void onCollectionClick(View v) {
		final Intent intent = new Intent(Intent.ACTION_VIEW, Collection.CONTENT_URI);
		startActivity(intent);
	}

	public void onBuddiesClick(View v){
		final Intent intent = new Intent(Intent.ACTION_VIEW, Buddies.CONTENT_URI);
		startActivity(intent);
	}

	public void onSyncClick(View v) {
		final Intent intent = new Intent(Intent.ACTION_SYNC, null, this, SyncService.class);
		intent.putExtra(SyncService.EXTRA_STATUS_RECEIVER, mReceiver);
		startService(intent);
	}
	
	public void onSettingsClick(View v){
		startActivity(new Intent(this, Preferences.class));
	}
	
	public void onAboutClick(View v){
		startActivity(new Intent(this, AboutActivity.class));
	}
	
	@Override
	public void onReceiveResult(int resultCode, Bundle resultData) {
		switch (resultCode) {
			case SyncService.STATUS_RUNNING:
				updateUiForSync(true);
				break;
			case SyncService.STATUS_COMPLETE:
				updateUiForSync(false);
				break;
			case SyncService.STATUS_ERROR:
				updateUiForSync(false);
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

	private void updateUiForSync(boolean isSyncing) {
		findViewById(R.id.home_btn_sync).setEnabled(!isSyncing);
	}
}