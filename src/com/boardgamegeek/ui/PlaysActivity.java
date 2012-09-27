package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.DateTimeUtils;

public class PlaysActivity extends SimpleSinglePaneActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (DateTimeUtils.howManyHoursOld(BggApplication.getInstance().getLastPlaysSync()) > 2) {
			BggApplication.getInstance().putLastPlaysSync();
			startService(new Intent(Intent.ACTION_SYNC, null, this, SyncService.class).putExtra(
				SyncService.KEY_SYNC_TYPE, SyncService.SYNC_TYPE_PLAYS));
		}
	}

	@Override
	protected Fragment onCreatePane() {
		return new PlaysFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.search_only;
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
