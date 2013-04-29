package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.xmlpull.v1.XmlPullParserException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.BuildConfig;
import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.NetworkUtils;
import com.boardgamegeek.util.NotificationUtils;
import com.boardgamegeek.util.PreferencesUtils;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
	private static final String TAG = makeLogTag(SyncAdapter.class);

	private final Context mContext;
	private final boolean mUseGzip = true;
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

		AccountManager accountManager = AccountManager.get(mContext);
		if (getAuthToken(accountManager, account, syncResult) == null) {
			return;
		}

		HttpClient httpClient = HttpUtils.createHttpClient(mContext, account.name, accountManager.getPassword(account),
			Long.parseLong(accountManager.getUserData(account, Authenticator.KEY_PASSWORD_EXPIRY)), mUseGzip);
		RemoteExecutor remoteExecutor = new RemoteExecutor(httpClient, mContext);

		List<SyncTask> tasks = createTasks(type);

		NotificationCompat.Builder builder = createNotificationBuilder();
		for (int i = 0; i < tasks.size(); i++) {
			if (mIsCancelled) {
				showError(getContext().getString(R.string.sync_notification_error_cancel), null);
				break;
			}
			mCurrentTask = tasks.get(i);
			try {
				if (mShowNotifications) {
					builder.setProgress(tasks.size(), i, true);
					builder.setContentText(mContext.getString(mCurrentTask.getNotification()));
					NotificationCompat.InboxStyle detail = new NotificationCompat.InboxStyle(builder);
					detail.setSummaryText(String.format(mContext.getString(R.string.sync_notification_step_summary),
						i + 1, tasks.size()));
					for (int j = i; j >= 0; j--) {
						detail.addLine(mContext.getString(tasks.get(j).getNotification()));
					}
					NotificationUtils.notify(mContext, NotificationUtils.ID_SYNC, builder);
				}
				mCurrentTask.execute(remoteExecutor, account, syncResult);
			} catch (IOException e) {
				LOGE(TAG, "Syncing " + mCurrentTask, e);
				syncResult.stats.numIoExceptions++;
				showError(e);
				break;
			} catch (XmlPullParserException e) {
				LOGE(TAG, "Syncing " + mCurrentTask, e);
				syncResult.stats.numParseExceptions++;
				showError(e);
			} catch (Exception e) {
				LOGE(TAG, "Syncing " + mCurrentTask, e);
				showError(e);
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

	private String getAuthToken(AccountManager accountManager, Account account, SyncResult syncResult) {
		String token = null;
		try {
			token = accountManager.blockingGetAuthToken(account, BggApplication.AUTHTOKEN_TYPE, true);
		} catch (OperationCanceledException e) {
			LOGE(TAG, "Getting auth token", e);
			syncResult.stats.numIoExceptions++;
			showAuthError(e);
		} catch (AuthenticatorException e) {
			LOGE(TAG, "Getting auth token", e);
			syncResult.stats.numAuthExceptions++;
			showAuthError(e);
		} catch (IOException e) {
			LOGE(TAG, "Getting auth token", e);
			syncResult.stats.numIoExceptions++;
			showAuthError(e);
		}
		return token;
	}

	private List<SyncTask> createTasks(final int type) {
		List<SyncTask> tasks = new ArrayList<SyncTask>();
		if ((type & SyncService.FLAG_SYNC_COLLECTION) == SyncService.FLAG_SYNC_COLLECTION) {
			tasks.add(new SyncCollectionListComplete());
			tasks.add(new SyncCollectionListModifiedSince());
			tasks.add(new SyncCollectionDetailOldest());
			tasks.add(new SyncCollectionDetailUnupdated());
		}
		if ((type & SyncService.FLAG_SYNC_BUDDIES) == SyncService.FLAG_SYNC_BUDDIES) {
			tasks.add(new SyncBuddiesList());
			tasks.add(new SyncBuddiesDetailOldest());
			tasks.add(new SyncBuddiesDetailUnupdated());
		}
		if ((type & SyncService.FLAG_SYNC_PLAYS_UPLOAD) == SyncService.FLAG_SYNC_PLAYS_UPLOAD) {
			tasks.add(new SyncPlaysUpload());
		}
		if ((type & SyncService.FLAG_SYNC_PLAYS_DOWNLOAD) == SyncService.FLAG_SYNC_PLAYS_DOWNLOAD) {
			tasks.add(new SyncPlays());
		}
		return tasks;
	}

	private void toggleReceiver(boolean enable) {
		ComponentName receiver = new ComponentName(mContext, CancelReceiver.class);
		PackageManager pm = mContext.getPackageManager();
		pm.setComponentEnabledSetting(receiver, enable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
			: PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
	}

	private NotificationCompat.Builder createNotificationBuilder() {
		PendingIntent pi = PendingIntent.getBroadcast(mContext, 0, new Intent(SyncService.ACTION_CANCEL_SYNC), 0);
		return NotificationUtils.createNotificationBuilder(mContext, R.string.sync_notification_title)
			.setPriority(NotificationCompat.PRIORITY_LOW)
			.addAction(R.drawable.ic_stat_cancel, mContext.getString(R.string.cancel), pi);
	}

	private void showError(Throwable t) {
		showError(mContext.getString(R.string.sync_notification_error), t.getLocalizedMessage());
	}

	private void showAuthError(Throwable t) {
		showError(mContext.getString(R.string.sync_notification_error_auth), t.getLocalizedMessage());
	}

	private void showError(String text, String message) {
		if (!mShowNotifications)
			return;

		NotificationCompat.Builder builder = NotificationUtils.createNotificationBuilder(mContext,
			R.string.sync_notification_title).setContentText(text);
		if (!TextUtils.isEmpty(message)) {
			builder.setStyle(new NotificationCompat.BigTextStyle().bigText(message).setSummaryText(text));
		}
		NotificationUtils.notify(mContext, NotificationUtils.ID_SYNC_ERROR, builder);
	}
}
