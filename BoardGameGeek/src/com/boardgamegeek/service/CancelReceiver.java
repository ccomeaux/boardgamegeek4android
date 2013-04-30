package com.boardgamegeek.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.boardgamegeek.util.PreferencesUtils;

public class CancelReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (SyncService.ACTION_CANCEL_SYNC.equals(action)) {
			cancel(context);
		} else if (Intent.ACTION_BATTERY_LOW.equals(action)) {
			cancel(context);
		} else if (Intent.ACTION_POWER_DISCONNECTED.equals(action) && PreferencesUtils.getSyncOnlyCharging(context)) {
			cancel(context);
		} else if ("android.net.conn.CONNECTIVITY_CHANGE".equals(action) && PreferencesUtils.getSyncOnlyWifi(context)) {
			cancel(context);
		}
	}

	private void cancel(Context context) {
		SyncService.cancelSync(context);
	}
}
