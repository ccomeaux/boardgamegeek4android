package com.boardgamegeek.service;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.boardgamegeek.BuildConfig;
import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.events.SyncCompleteEvent;
import com.boardgamegeek.events.SyncEvent;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BoardGameGeekService;
import com.boardgamegeek.util.BatteryUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.NetworkUtils;
import com.boardgamegeek.util.NotificationUtils;
import com.boardgamegeek.util.PreferencesUtils;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;
import timber.log.Timber;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
	private final Context context;
	private boolean shouldShowNotifications = true;
	private SyncTask currentTask;
	private boolean isCancelled;

	@DebugLog
	public SyncAdapter(Context context) {
		super(context, false);
		this.context = context;
		shouldShowNotifications = PreferencesUtils.getSyncShowNotifications(this.context);

		// // noinspection ConstantConditions,PointlessBooleanExpression
		if (!BuildConfig.DEBUG) {
			Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
				@Override
				public void uncaughtException(Thread thread, Throwable throwable) {
					Timber.e(throwable, "Uncaught sync exception, suppressing UI in release build.");
				}
			});
		}
	}

	@DebugLog
	@Override
	public void onPerformSync(@NonNull Account account, @NonNull Bundle extras, String authority, ContentProviderClient provider, @NonNull SyncResult syncResult) {
		isCancelled = false;
		final boolean uploadOnly = extras.getBoolean(ContentResolver.SYNC_EXTRAS_UPLOAD, false);
		final boolean manualSync = extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);
		final boolean initialize = extras.getBoolean(ContentResolver.SYNC_EXTRAS_INITIALIZE, false);
		final int type = extras.getInt(SyncService.EXTRA_SYNC_TYPE, SyncService.FLAG_SYNC_ALL);

		Timber.i("Beginning sync for account " + account.name + "," + " uploadOnly=" + uploadOnly + " manualSync="
			+ manualSync + " initialize=" + initialize + ", type=" + type);

		if (initialize) {
			ContentResolver.setIsSyncable(account, authority, 1);
			ContentResolver.setSyncAutomatically(account, authority, true);
			Bundle b = new Bundle();
			ContentResolver.addPeriodicSync(account, authority, b, 8 * 60 * 60); // 8 hours
		}

		if (!shouldContinueSync(uploadOnly)) {
			return;
		}

		toggleReceiver(true);
		shouldShowNotifications = PreferencesUtils.getSyncShowNotifications(context);
		List<SyncTask> tasks = createTasks(context, type);
		for (int i = 0; i < tasks.size(); i++) {
			if (isCancelled) {
				Timber.i("Cancelling all sync tasks");
				if (currentTask != null) {
					showCancel(currentTask.getNotification());
				}
				break;
			}
			currentTask = tasks.get(i);
			try {
				EventBus.getDefault().postSticky(new SyncEvent(currentTask.getSyncType()));
				currentTask.showNotification();
				currentTask.execute(account, syncResult);
				EventBus.getDefault().post(new SyncCompleteEvent());
				EventBus.getDefault().removeStickyEvent(SyncEvent.class);
			} catch (Exception e) {
				Timber.e(e, "Syncing " + currentTask);
				syncResult.stats.numIoExceptions++;
				showError(currentTask, e);
			}
		}
		toggleReceiver(false);
		NotificationUtils.cancel(context, NotificationUtils.ID_SYNC);
	}

	@DebugLog
	@Override
	public void onSyncCanceled() {
		super.onSyncCanceled();
		Timber.i("Sync cancel requested.");
		isCancelled = true;
		if (currentTask != null) {
			currentTask.cancel();
		}
	}

	@DebugLog
	private boolean shouldContinueSync(boolean uploadOnly) {
		if (uploadOnly) {
			Timber.w("Upload only, returning.");
			return false;
		}

		if (NetworkUtils.isOffline(context)) {
			Timber.i("Skipping sync; offline");
			return false;
		}

		if (PreferencesUtils.getSyncOnlyCharging(context) && !BatteryUtils.isCharging(context)) {
			Timber.i("Skipping sync; not charging");
			return false;
		}

		if (PreferencesUtils.getSyncOnlyWifi(context) && !NetworkUtils.isOnWiFi(context)) {
			Timber.i("Skipping sync; not on wifi");
			return false;
		}

		if (BatteryUtils.isBatteryLow(context)) {
			Timber.i("Skipping sync; battery low");
			return false;
		}

		return true;
	}

	@DebugLog
	@NonNull
	private List<SyncTask> createTasks(Context context, final int type) {
		BoardGameGeekService service = Adapter.createForXmlWithAuth(context);
		List<SyncTask> tasks = new ArrayList<>();
		if ((type & SyncService.FLAG_SYNC_COLLECTION_UPLOAD) == SyncService.FLAG_SYNC_COLLECTION_UPLOAD) {
			tasks.add(new SyncCollectionUpload(context, service));
		}
		if ((type & SyncService.FLAG_SYNC_COLLECTION) == SyncService.FLAG_SYNC_COLLECTION) {
			if (PreferencesUtils.isSyncStatus(context)) {
				long lastCompleteSync = Authenticator.getLong(context, SyncService.TIMESTAMP_COLLECTION_COMPLETE);
				if (lastCompleteSync >= 0 && DateTimeUtils.howManyDaysOld(lastCompleteSync) < 7) {
					tasks.add(new SyncCollectionModifiedSince(context, service));
				} else {
					tasks.add(new SyncCollectionComplete(context, service));
				}
			} else {
				Timber.i("...no statuses set to sync");
			}

			tasks.add(new SyncCollectionUnupdated(context, service));
			tasks.add(new SyncGamesRemove(context, service));
			tasks.add(new SyncGamesOldest(context, service));
			tasks.add(new SyncGamesUnupdated(context, service));
		}
		if ((type & SyncService.FLAG_SYNC_PLAYS_UPLOAD) == SyncService.FLAG_SYNC_PLAYS_UPLOAD) {
			tasks.add(new SyncPlaysUpload(context, service));
		}
		if ((type & SyncService.FLAG_SYNC_PLAYS_DOWNLOAD) == SyncService.FLAG_SYNC_PLAYS_DOWNLOAD) {
			tasks.add(new SyncPlays(context, service));
		}
		if ((type & SyncService.FLAG_SYNC_BUDDIES) == SyncService.FLAG_SYNC_BUDDIES) {
			tasks.add(new SyncBuddiesList(context, service));
			tasks.add(new SyncBuddiesDetailOldest(context, service));
			tasks.add(new SyncBuddiesDetailUnupdated(context, service));
		}
		return tasks;
	}

	@DebugLog
	private void toggleReceiver(boolean enable) {
		ComponentName receiver = new ComponentName(context, CancelReceiver.class);
		PackageManager pm = context.getPackageManager();
		pm.setComponentEnabledSetting(receiver, enable ?
				PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
				PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
			PackageManager.DONT_KILL_APP);
	}

	@DebugLog
	private void showError(@NonNull SyncTask task, @NonNull Throwable t) {
		if (!shouldShowNotifications) {
			return;
		}

		String message = t.getMessage();
		if (TextUtils.isEmpty(message)) {
			Throwable t1 = t.getCause();
			if (t1 != null) {
				message = t1.toString();
			}
		}

		final int notification = task.getNotification();
		if (notification != ServiceTask.NO_NOTIFICATION) {
			CharSequence text = context.getText(notification);
			NotificationCompat.Builder builder = NotificationUtils
				.createNotificationBuilder(context, R.string.sync_notification_title_error)
				.setContentText(text)
				.setCategory(NotificationCompat.CATEGORY_ERROR);
			if (!TextUtils.isEmpty(message)) {
				builder.setStyle(new NotificationCompat.BigTextStyle().bigText(message).setSummaryText(text));
			}
			NotificationUtils.notify(context, NotificationUtils.ID_SYNC_ERROR, builder);
		}
	}

	@DebugLog
	private void showCancel(int messageId) {
		if (!shouldShowNotifications) {
			return;
		}

		final CharSequence contextText = context.getText(messageId);
		NotificationCompat.Builder builder = NotificationUtils
			.createNotificationBuilder(context, R.string.sync_notification_title_cancel)
			.setContentText(contextText)
			.setCategory(NotificationCompat.CATEGORY_SERVICE);
		NotificationUtils.notify(context, NotificationUtils.ID_SYNC_ERROR, builder);
	}
}
