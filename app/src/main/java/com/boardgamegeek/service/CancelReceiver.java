package com.boardgamegeek.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.boardgamegeek.BuildConfig;
import com.boardgamegeek.util.PreferencesUtils;

import timber.log.Timber;

public class CancelReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, @NonNull Intent intent) {
		String action = intent.getAction();
		if (SyncService.ACTION_CANCEL_SYNC.equals(action)) {
			notifyCause(context, "User requested cancel.");
			cancelSync(context);
		} else if (Intent.ACTION_BATTERY_LOW.equals(action)) {
			notifyCause(context, "Cancelling because battery is running low.");
			cancelSync(context);
		} else if (Intent.ACTION_POWER_DISCONNECTED.equals(action) && PreferencesUtils.getSyncOnlyCharging(context)) {
			notifyCause(context, "Cancelling because device was unplugged and user asked for this behavior.");
			cancelSync(context);
		} else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action) && PreferencesUtils.getSyncOnlyWifi(context)) {
			notifyCause(context, "Cancelling because device lost Wifi and user asked for this behavior.");
			cancelSync(context);
		} else {
			notifyCause(context, "Not cancelling due to an unexpected action: " + action);
		}
	}

	private void notifyCause(Context context, String message) {
		Timber.i(message);
		if (BuildConfig.DEBUG) {
			Toast.makeText(context, message, Toast.LENGTH_LONG).show();
		}
	}

	private void cancelSync(Context context) {
		SyncService.cancelSync(context);
	}
}
