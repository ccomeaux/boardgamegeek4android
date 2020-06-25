package com.boardgamegeek.service;

import android.accounts.Account;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.BuildConfig;
import com.boardgamegeek.R;
import com.boardgamegeek.events.SyncCompleteEvent;
import com.boardgamegeek.events.SyncEvent;
import com.boardgamegeek.extensions.BatteryUtils;
import com.boardgamegeek.extensions.NetworkUtils;
import com.boardgamegeek.extensions.PreferenceUtils;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.NotificationUtils;
import com.boardgamegeek.util.RemoteConfig;
import com.boardgamegeek.util.StringUtils;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.BigTextStyle;
import hugo.weaving.DebugLog;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import timber.log.Timber;

import static com.boardgamegeek.extensions.PreferenceUtils.PREFERENCES_KEY_SYNC_BUDDIES;
import static com.boardgamegeek.extensions.PreferenceUtils.PREFERENCES_KEY_SYNC_PLAYS;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
	private final BggApplication application;
	private SyncTask currentTask;
	private boolean isCancelled;
	private final CancelReceiver cancelReceiver = new CancelReceiver();
	SharedPreferences prefs;

	static class CrashKeys {
		private static final String SYNC_TYPES = "SYNC_TYPES";
		private static final String SYNC_TYPE = "SYNC_TYPE";
		private static final String SYNC_SETTINGS = "SYNC_SETTINGS";
	}

	@DebugLog
	public SyncAdapter(BggApplication context) {
		super(context.getApplicationContext(), false);
		application = context;

		if (!BuildConfig.DEBUG) {
			Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> Timber.e(throwable, "Uncaught sync exception, suppressing UI in release build."));
		}

		context.registerReceiver(cancelReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
	}

	/**
	 * Perform a sync. This builds a list of sync tasks from the types specified in the {@code extras bundle}, iterating
	 * over each. It posts and removes a {@code SyncEvent} with the type of sync task. As well as showing the progress
	 * in a notification.
	 */
	@DebugLog
	@Override
	public void onPerformSync(@NonNull Account account, @NonNull Bundle extras, String authority, ContentProviderClient provider, @NonNull SyncResult syncResult) {
		RemoteConfig.fetch();

		isCancelled = false;
		final boolean uploadOnly = extras.getBoolean(ContentResolver.SYNC_EXTRAS_UPLOAD, false);
		final boolean manualSync = extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);
		final boolean initialize = extras.getBoolean(ContentResolver.SYNC_EXTRAS_INITIALIZE, false);
		final int type = extras.getInt(SyncService.EXTRA_SYNC_TYPE, SyncService.FLAG_SYNC_ALL);

		Timber.i("Beginning sync for account %s, uploadOnly=%s manualSync=%s initialize=%s, type=%d", account.name, uploadOnly, manualSync, initialize, type);
		FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
		crashlytics.setCustomKey(CrashKeys.SYNC_TYPES, type);

		prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
		String statuses = StringUtils.formatList(Collections.singletonList(PreferenceUtils.getSyncStatusesOrDefault(prefs)));
		if (prefs.getBoolean(PREFERENCES_KEY_SYNC_PLAYS, false)) statuses += " | plays";
		if (prefs.getBoolean(PREFERENCES_KEY_SYNC_BUDDIES, false)) statuses += " | buddies";
		crashlytics.setCustomKey(CrashKeys.SYNC_SETTINGS, statuses);

		if (initialize) {
			ContentResolver.setIsSyncable(account, authority, 1);
			ContentResolver.setSyncAutomatically(account, authority, true);
			Bundle b = new Bundle();
			ContentResolver.addPeriodicSync(account, authority, b, 24 * 60 * 60); // 24 hours
		}

		if (!shouldContinueSync()) {
			finishSync();
			return;
		}

		toggleCancelReceiver(true);
		List<SyncTask> tasks = createTasks(application, type, uploadOnly, syncResult, account);
		for (int i = 0; i < tasks.size(); i++) {
			if (isCancelled) {
				Timber.i("Cancelling all sync tasks");
				if (currentTask != null) {
					notifySyncIsCancelled(currentTask.getNotificationSummaryMessageId());
				}
				break;
			}
			currentTask = tasks.get(i);
			try {
				EventBus.getDefault().postSticky(new SyncEvent(currentTask.getSyncType()));
				FirebaseCrashlytics.getInstance().setCustomKey(CrashKeys.SYNC_TYPE, currentTask.getSyncType());
				currentTask.updateProgressNotification();
				currentTask.execute();
				EventBus.getDefault().removeStickyEvent(SyncEvent.class);
				if (currentTask.isCancelled()) {
					Timber.i("Sync task %s has requested the sync operation to be cancelled", currentTask);
					break;
				}
			} catch (Exception e) {
				Timber.e(e, "Syncing %s", currentTask);
				syncResult.stats.numIoExceptions += 10;
				showException(currentTask, e);
				if (e.getCause() instanceof SocketTimeoutException) {
					break;
				}
			}
		}
		finishSync();
	}

	private void finishSync() {
		NotificationUtils.cancel(getContext(), NotificationUtils.TAG_SYNC_PROGRESS);
		toggleCancelReceiver(false);
		EventBus.getDefault().post(new SyncCompleteEvent());
		try {
			getContext().unregisterReceiver(cancelReceiver);
		} catch (Exception e) {
			Timber.w(e);
		}
	}

	/**
	 * Indicates that a sync operation has been canceled.
	 */
	@DebugLog
	@Override
	public void onSyncCanceled() {
		super.onSyncCanceled();
		Timber.i("Sync cancel requested.");
		isCancelled = true;
		if (currentTask != null) currentTask.cancel();
	}

	/**
	 * Determine if the sync should continue based on the current state of the device.
	 */
	@DebugLog
	private boolean shouldContinueSync() {
		if (NetworkUtils.isOffline(getContext())) {
			Timber.i("Skipping sync; offline");
			return false;
		}

		if (PreferenceUtils.getSyncOnlyCharging(prefs) && !BatteryUtils.isCharging(getContext())) {
			Timber.i("Skipping sync; not charging");
			return false;
		}

		if (PreferenceUtils.getSyncOnlyWifi(prefs) && !NetworkUtils.isOnWiFi(getContext())) {
			Timber.i("Skipping sync; not on wifi");
			return false;
		}

		if (BatteryUtils.isBatteryLow(getContext())) {
			Timber.i("Skipping sync; battery low");
			return false;
		}

		if (!RemoteConfig.getBoolean(RemoteConfig.KEY_SYNC_ENABLED)) {
			Timber.i("Sync disabled remotely");
			return false;
		}

		if (hasPrivacyError()) {
			Timber.i("User still hasn't accepted the new privacy policy.");
			return false;
		}

		return true;
	}

	private boolean hasPrivacyError() {
		int weeksToCompare = RemoteConfig.getInt(RemoteConfig.KEY_PRIVACY_CHECK_WEEKS);
		int weeks = DateTimeUtils.howManyWeeksOld(PreferenceUtils.getLastPrivacyCheckTimestamp(prefs));
		if (weeks < weeksToCompare) {
			Timber.i("We checked the privacy statement less than %,d weeks ago; skipping", weeksToCompare);
			return false;
		}
		OkHttpClient httpClient = HttpUtils.getHttpClientWithAuth(getContext());
		final String url = "https://www.boardgamegeek.com";
		Request request = new Request.Builder().url(url).build();
		try {
			Response response = httpClient.newCall(request).execute();
			final ResponseBody body = response.body();
			final String content = body == null ? "" : body.string().trim();
			if (content.contains("Please update your privacy and marketing preferences")) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				PendingIntent pi = PendingIntent.getActivity(getContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
				final String message = getContext().getString(R.string.sync_notification_message_privacy_error);
				NotificationCompat.Builder builder = NotificationUtils
					.createNotificationBuilder(getContext(), R.string.sync_notification_title_error, NotificationUtils.CHANNEL_ID_ERROR)
					.setContentText(message)
					.setStyle(new BigTextStyle().bigText(message))
					.setContentIntent(pi)
					.setCategory(NotificationCompat.CATEGORY_ERROR)
					.setPriority(NotificationCompat.PRIORITY_HIGH);
				NotificationUtils.notify(getContext(), NotificationUtils.TAG_SYNC_ERROR, Integer.MAX_VALUE, builder);
				return true;
			} else {
				PreferenceUtils.setLastPrivacyCheckTimestamp(prefs);
				return false;
			}
		} catch (IOException e) {
			Timber.w(e);
			return true;
		}
	}

	/**
	 * Create a list of sync tasks based on the specified type.
	 */
	@DebugLog
	@NonNull
	private List<SyncTask> createTasks(BggApplication application, final int typeList, boolean uploadOnly, @NonNull SyncResult syncResult, @NonNull Account account) {
		BggService service = Adapter.createForXmlWithAuth(application);
		List<SyncTask> tasks = new ArrayList<>();
		if (shouldCreateTask(typeList, SyncService.FLAG_SYNC_COLLECTION_UPLOAD)) {
			tasks.add(new SyncCollectionUpload(application, service, syncResult));
		}
		if (shouldCreateTask(typeList, SyncService.FLAG_SYNC_COLLECTION_DOWNLOAD) && !uploadOnly) {
			tasks.add(new SyncCollectionComplete(application, service, syncResult, account));
			tasks.add(new SyncCollectionModifiedSince(application, service, syncResult, account));
			tasks.add(new SyncCollectionUnupdated(application, service, syncResult, account));
		}
		if (shouldCreateTask(typeList, SyncService.FLAG_SYNC_GAMES) && !uploadOnly) {
			tasks.add(new SyncGamesRemove(application, service, syncResult));
			tasks.add(new SyncGamesOldest(application, service, syncResult));
			tasks.add(new SyncGamesUnupdated(application, service, syncResult));
		}
		if (shouldCreateTask(typeList, SyncService.FLAG_SYNC_PLAYS_UPLOAD)) {
			tasks.add(new SyncPlaysUpload(application, service, syncResult));
		}
		if (shouldCreateTask(typeList, SyncService.FLAG_SYNC_PLAYS_DOWNLOAD) && !uploadOnly) {
			tasks.add(new SyncPlays(application, service, syncResult, account));
		}
		if (shouldCreateTask(typeList, SyncService.FLAG_SYNC_BUDDIES) && !uploadOnly) {
			tasks.add(new SyncBuddiesList(application, service, syncResult, account));
			tasks.add(new SyncBuddiesDetailOldest(application, service, syncResult));
			tasks.add(new SyncBuddiesDetailUnupdated(application, service, syncResult));
		}
		return tasks;
	}

	private boolean shouldCreateTask(int typeList, int type) {
		return (typeList & type) == type;
	}

	/**
	 * Enable or disable the cancel receiver. (There's no reason for the receiver to be enabled when the sync isn't running.
	 */
	@DebugLog
	private void toggleCancelReceiver(boolean enable) {
		ComponentName receiver = new ComponentName(getContext(), CancelReceiver.class);
		PackageManager pm = getContext().getPackageManager();
		pm.setComponentEnabledSetting(receiver, enable ?
				PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
				PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
			PackageManager.DONT_KILL_APP);
	}

	/**
	 * Show a notification of any exception thrown by a sync task that isn't caught by the task.
	 * ]
	 */
	@DebugLog
	private void showException(@NonNull SyncTask task, @NonNull Throwable t) {
		String message = t.getMessage();
		if (TextUtils.isEmpty(message)) {
			Throwable t1 = t.getCause();
			if (t1 != null) {
				message = t1.toString();
			}
		}

		Timber.w(message);

		if (!PreferenceUtils.getSyncShowErrors(prefs)) return;

		final int messageId = task.getNotificationSummaryMessageId();
		if (messageId != SyncTask.NO_NOTIFICATION) {
			CharSequence text = getContext().getText(messageId);
			NotificationCompat.Builder builder = NotificationUtils
				.createNotificationBuilder(getContext(), R.string.sync_notification_title_error, NotificationUtils.CHANNEL_ID_ERROR)
				.setContentText(text)
				.setPriority(NotificationCompat.PRIORITY_HIGH)
				.setCategory(NotificationCompat.CATEGORY_ERROR);
			if (!TextUtils.isEmpty(message)) {
				builder.setStyle(new NotificationCompat.BigTextStyle().bigText(message).setSummaryText(text));
			}
			NotificationUtils.notify(getContext(), NotificationUtils.TAG_SYNC_ERROR, 0, builder);
		}
	}

	/**
	 * Show that the sync was cancelled in a notification. This may be useless since the notification is cancelled
	 * almost immediately after this is shown.
	 */
	@DebugLog
	private void notifySyncIsCancelled(int messageId) {
		if (!PreferenceUtils.getSyncShowNotifications(prefs)) return;

		CharSequence contextText = messageId == SyncTask.NO_NOTIFICATION ? "" : getContext().getText(messageId);

		NotificationCompat.Builder builder = NotificationUtils
			.createNotificationBuilder(getContext(), R.string.sync_notification_title_cancel, NotificationUtils.CHANNEL_ID_SYNC_PROGRESS)
			.setContentText(contextText)
			.setCategory(NotificationCompat.CATEGORY_SERVICE);
		NotificationUtils.notify(getContext(), NotificationUtils.TAG_SYNC_PROGRESS, 0, builder);
	}
}
