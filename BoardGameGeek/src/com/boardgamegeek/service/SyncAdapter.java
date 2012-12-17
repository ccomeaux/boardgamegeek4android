package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

import com.boardgamegeek.BuildConfig;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.util.HttpUtils;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
	private static final String TAG = makeLogTag(SyncAdapter.class);

	private final Context mContext;
	private final boolean mUseGzip = true;

	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		mContext = context;

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
		final boolean uploadOnly = extras.getBoolean(ContentResolver.SYNC_EXTRAS_UPLOAD, false);
		final boolean manualSync = extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);
		final boolean initialize = extras.getBoolean(ContentResolver.SYNC_EXTRAS_INITIALIZE, false);

		LOGI(TAG, "Beginning sync for account " + account.name + "," + " uploadOnly=" + uploadOnly + " manualSync="
			+ manualSync + " initialize=" + initialize);

		if (uploadOnly) {
			LOGW(TAG, "Upload only, returning.");
			return;
		}

		if (initialize) {
			ContentResolver.setIsSyncable(account, authority, 1);
			ContentResolver.setSyncAutomatically(account, authority, true);
			Bundle b = new Bundle();
			ContentResolver.addPeriodicSync(account, authority, b, 6 * 60 * 60); // 6 hours
		}

		AccountManager accountManager = AccountManager.get(mContext);
		HttpClient mHttpClient = HttpUtils.createHttpClient(mContext, account.name,
			accountManager.getPassword(account),
			Long.parseLong(accountManager.getUserData(account, Authenticator.KEY_PASSWORD_EXPIRY)), mUseGzip);
		RemoteExecutor mRemoteExecutor = new RemoteExecutor(mHttpClient, mContext);

		List<SyncTask> tasks = new ArrayList<SyncTask>();
		tasks.add(new SyncCollectionList());
		tasks.add(new SyncCollectionDetail());
		tasks.add(new SyncBuddiesList());
		tasks.add(new SyncBuddiesDetail());
		tasks.add(new SyncPlays());
		tasks.add(new SyncPlaysUpload());

		for (SyncTask task : tasks) {
			try {
				task.execute(mRemoteExecutor, mContext);
				// TODO set detail in syncResult.stats
				syncResult.stats.numDeletes++;
				syncResult.stats.numEntries++;
				syncResult.stats.numUpdates++;
				syncResult.stats.numSkippedEntries++;
			} catch (HandlerException e) {
				// TODO separate exceptions
				LOGE(TAG, "Syncing collection list", e);
				syncResult.stats.numAuthExceptions++;
				syncResult.stats.numIoExceptions++;
				syncResult.stats.numParseExceptions++;
			}
		}
	}
}
