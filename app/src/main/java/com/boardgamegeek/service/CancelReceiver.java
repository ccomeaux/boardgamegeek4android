package com.boardgamegeek.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.boardgamegeek.util.PreferencesUtils;

import timber.log.Timber;

public class CancelReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, @NonNull Intent intent) {
		String action = intent.getAction();
		if (SyncService.ACTION_CANCEL_SYNC.equals(action)) {
			Timber.i("User requested cancel.");
			cancel(context);
		} else if (Intent.ACTION_BATTERY_LOW.equals(action)) {
			Timber.i("Cancelling because battery is running low.");
			cancel(context);
		} else if (Intent.ACTION_POWER_DISCONNECTED.equals(action) && PreferencesUtils.getSyncOnlyCharging(context)) {
			Timber.i("Cancelling because device was unplugged and user asked for this behavior.");
			cancel(context);
		} else if ("android.net.conn.CONNECTIVITY_CHANGE".equals(action) && PreferencesUtils.getSyncOnlyWifi(context)) {
			Timber.i("Cancelling because device lost Wifi and user asked for this behavior.");
			cancel(context);
		}
	}

	private void cancel(Context context) {
		SyncService.cancelSync(context);
	}
}
