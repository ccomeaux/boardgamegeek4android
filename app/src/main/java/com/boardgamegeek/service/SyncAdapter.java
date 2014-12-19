package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.boardgamegeek.BuildConfig;
import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.NetworkUtils;
import com.boardgamegeek.util.NotificationUtils;
import com.boardgamegeek.util.PreferencesUtils;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
	private static final String TAG = makeLogTag(SyncAdapter.class);

	private final Context mContext;
	private boolean mShowNotifications = true;
	private SyncTask mCurrentTask;
	private boolean mIsCancelled;

	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		mContext = context;
		mShowNotifications = PreferencesUtils.getSyncShowNotifications(mContext);

		// // noinspection ConstantConditions,PointlessBooleanExpression
		if (!BuildConfig.DEBUG) {
			Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
				@Override
				public void uncaughtException(Thread thread, Throwable throwable) {
					LOGE(TAG, "Uncaught sync exception, suppressing UI in release build.", throwable);
				}
			});
		}
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider,
							  SyncResult syncResult) {
		mIsCancelled = false;
		final boolean uploadOnly = extras.getBoolean(ContentResolver.SYNC_EXTRAS_UPLOAD, false);
		final boolean manualSync = extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);
		final boolean initialize = extras.getBoolean(ContentResolver.SYNC_EXTRAS_INITIALIZE, false);
		final int type = extras.getInt(SyncService.EXTRA_SYNC_TYPE, SyncService.FLAG_SYNC_ALL);

		LOGI(TAG, "Beginning sync for account " + account.name + "," + " uploadOnly=" + uploadOnly + " manualSync="
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
		mShowNotifications = PreferencesUtils.getSyncShowNotifications(mContext);
		List<SyncTask> tasks = createTasks(mContext, type);
		for (int i = 0; i < tasks.size(); i++) {
			if (mIsCancelled) {
				showCancel(mCurrentTask.getNotification());
				break;
			}
			mCurrentTask = tasks.get(i);
			try {
				mCurrentTask.showNotification();
				mCurrentTask.execute(account, syncResult);
			} catch (Exception e) {
				LOGE(TAG, "Syncing " + mCurrentTask, e);
				syncResult.stats.numIoExceptions++;
				showError(mCurrentTask, e);
			}
		}
		toggleReceiver(false);
		NotificationUtils.cancel(mContext, NotificationUtils.ID_SYNC);
	}

	@Override
	public void onSyncCanceled() {
		super.onSyncCanceled();
		mIsCancelled = true;
		if (mCurrentTask != null) {
			mCurrentTask.cancel();
		}
	}

	private boolean shouldContinueSync(boolean uploadOnly) {
		if (uploadOnly) {
			LOGW(TAG, "Upload only, returning.");
			return false;
		}

		if (!NetworkUtils.isOnline(mContext)) {
			LOGI(TAG, "Skipping sync; offline");
			return false;
		}

		if (PreferencesUtils.getSyncOnlyCharging(mContext) && !NetworkUtils.isCharging(mContext)) {
			LOGI(TAG, "Skipping sync; not charging");
			return false;
		}

		if (PreferencesUtils.getSyncOnlyWifi(mContext) && !NetworkUtils.isOnWiFi(mContext)) {
			LOGI(TAG, "Skipping sync; not on wifi");
			return false;
		}

		if (NetworkUtils.isBatteryLow(mContext)) {
			LOGI(TAG, "Skipping sync; battery low");
			return false;
		}

		return true;
	}

	private List<SyncTask> createTasks(Context context, final int type) {
		BggService service = Adapter.createWithAuth(context);
		List<SyncTask> tasks = new ArrayList<SyncTask>();
		if ((type & SyncService.FLAG_SYNC_COLLECTION) == SyncService.FLAG_SYNC_COLLECTION) {
			if (PreferencesUtils.isSyncStatus(context)) {
				long lastCompleteSync = Authenticator.getLong(context, SyncService.TIMESTAMP_COLLECTION_COMPLETE);
				if (lastCompleteSync >= 0 && DateTimeUtils.howManyDaysOld(lastCompleteSync) < 7) {
					tasks.add(new SyncCollectionListModifiedSince(context, service));
				} else {
					tasks.add(new SyncCollectionListComplete(context, service));
				}
			} else {
				LOGI(TAG, "...no statuses set to sync");
			}

			tasks.add(new SyncCollectionListUnupdated(context, service));
			tasks.add(new SyncCollectionDetailOldest(context, service));
			tasks.add(new SyncCollectionDetailUnupdated(context, service));
			tasks.add(new SyncCollectionDetailMissing(context, service));
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

	private void toggleReceiver(boolean enable) {
		ComponentName receiver = new ComponentName(mContext, CancelReceiver.class);
		PackageManager pm = mContext.getPackageManager();
		pm.setComponentEnabledSetting(receiver, enable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
			: PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
	}

	private void showError(SyncTask task, Throwable t) {
		if (!mShowNotifications) {
			return;
		}

		String message = t.getMessage();
		if (TextUtils.isEmpty(message)) {
			Throwable t1 = t.getCause();
			if (t1 != null) {
				message = t1.toString();
			}
		}

		CharSequence text = mContext.getText(task.getNotification());
		NotificationCompat.Builder builder = NotificationUtils
			.createNotificationBuilder(mContext, R.string.sync_notification_title_error).setContentText(text)
			.setCategory(NotificationCompat.CATEGORY_ERROR);
		if (!TextUtils.isEmpty(message)) {
			builder.setStyle(new NotificationCompat.BigTextStyle().bigText(message).setSummaryText(text));
		}
		NotificationUtils.notify(mContext, NotificationUtils.ID_SYNC_ERROR, builder);
	}

	private void showCancel(int messageId) {
		if (!mShowNotifications) {
			return;
		}

		NotificationCompat.Builder builder = NotificationUtils
			.createNotificationBuilder(mContext, R.string.sync_notification_title_cancel)
			.setContentText(mContext.getText(messageId)).setCategory(NotificationCompat.CATEGORY_SERVICE);
		NotificationUtils.notify(mContext, NotificationUtils.ID_SYNC_ERROR, builder);
	}
}
