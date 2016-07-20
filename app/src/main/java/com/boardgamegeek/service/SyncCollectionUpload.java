package com.boardgamegeek.service;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.text.SpannableString;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.service.model.CollectionItem;
import com.boardgamegeek.ui.CollectionActivity;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.NotificationUtils;
import com.boardgamegeek.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import hugo.weaving.DebugLog;
import okhttp3.OkHttpClient;
import timber.log.Timber;

public class SyncCollectionUpload extends SyncUploadTask {
	private ContentResolver resolver;
	private SyncResult syncResult;
	private final List<String> timestampColumns = new ArrayList<>();
	private final OkHttpClient okHttpClient;
	private List<CollectionUploadTask> tasks;

	@DebugLog
	public SyncCollectionUpload(Context context, BggService service) {
		super(context, service);
		okHttpClient = HttpUtils.getHttpClientWithAuth(context);
		tasks = createTasks();
		timestampColumns.add(Collection.STATUS_DIRTY_TIMESTAMP);
		timestampColumns.add(Collection.RATING_DIRTY_TIMESTAMP);
		timestampColumns.add(Collection.COMMENT_DIRTY_TIMESTAMP);
		timestampColumns.add(Collection.PRIVATE_INFO_DIRTY_TIMESTAMP);
	}

	private List<CollectionUploadTask> createTasks() {
		List<CollectionUploadTask> tasks = new ArrayList<>();
		tasks.add(new CollectionStatusUploadTask(okHttpClient));
		tasks.add(new CollectionRatingUploadTask(okHttpClient));
		tasks.add(new CollectionCommentUploadTask(okHttpClient));
		tasks.add(new CollectionPrivateInfoUploadTask(okHttpClient));
		return tasks;
	}

	@DebugLog
	@Override
	public int getSyncType() {
		return SyncService.FLAG_SYNC_COLLECTION_UPLOAD;
	}

	@DebugLog
	@Override
	protected int getNotificationTitleResId() {
		return R.string.sync_notification_title_collection_upload;
	}

	@DebugLog
	@Override
	protected Class<?> getNotificationIntentClass() {
		return CollectionActivity.class;
	}

	@DebugLog
	@Override
	protected int getNotificationErrorId() {
		return NotificationUtils.ID_SYNC_COLLECTION_UPLOAD_ERROR;
	}

	@DebugLog
	@Override
	protected int getNotificationMessageId() {
		return NotificationUtils.ID_SYNC_COLLECTION_UPLOAD;
	}

	@DebugLog
	@Override
	protected int getUploadSummaryWithSize() {
		return R.string.sync_notification_collection_upload_summary;
	}

	@DebugLog
	@Override
	public void execute(Account account, SyncResult syncResult) {
		init(syncResult);
		Cursor cursor = null;
		try {
			cursor = fetchDirtyCollectionItems();
			while (cursor != null && cursor.moveToNext()) {
				if (isCancelled()) {
					break;
				}
				processCollectionItem(cursor);
			}
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
	}

	private void init(SyncResult syncResult) {
		resolver = context.getContentResolver();
		this.syncResult = syncResult;
	}

	private Cursor fetchDirtyCollectionItems() {
		String selection = "";
		for (CollectionUploadTask task : tasks) {
			if (!TextUtils.isEmpty(selection)) {
				selection += " OR ";
			}
			selection += task.getTimestampColumn() + ">0";
		}
		Cursor cursor = context.getContentResolver().query(Collection.CONTENT_URI,
			CollectionItem.PROJECTION,
			selection,
			null,
			null);
		final int count = cursor != null ? cursor.getCount() : 0;
		String detail = context.getResources().getQuantityString(R.plurals.sync_notification_collection_uploading, count, count);
		Timber.i(detail);
		showNotification(detail);
		return cursor;
	}

	private void processCollectionItem(Cursor cursor) {
		CollectionItem collectionItem = CollectionItem.fromCursor(cursor);
		if (collectionItem.getCollectionId() != BggContract.INVALID_ID) {
			ContentValues contentValues = new ContentValues();
			for (CollectionUploadTask response : tasks) {
				response.addCollectionItem(collectionItem);
				if (response.isDirty()) {
					response.post();
					if (processResponseForError(response)) {
						return;
					}
					response.appendContentValues(contentValues);
				}
			}
			if (contentValues != null && contentValues.size() > 0) {
				resolver.update(Collection.buildUri(collectionItem.getInternalId()), contentValues, null, null);
				notifySuccess(collectionItem.getCollectionName());
			}
		} else {
			Timber.d("Invalid collectionItem ID for internal ID %1$s; game ID %2$s", collectionItem.getInternalId(), collectionItem.getGameId());
		}
	}

	private void notifySuccess(String collectionName) {
		syncResult.stats.numUpdates++;
		SpannableString message = StringUtils.boldSecondString(context.getString(R.string.sync_notification_collection_upload_detail), collectionName);
		Timber.i(message.toString());
		notifyUser(message);
	}

	private boolean processResponseForError(CollectionUploadTask response) {
		if (response.hasAuthError()) {
			Timber.w("Auth error; clearing password");
			syncResult.stats.numAuthExceptions++;
			Authenticator.clearPassword(context);
			return true;
		} else if (response.hasError()) {
			syncResult.stats.numIoExceptions++;
			notifyUploadError(response.getErrorMessage());
			return true;
		}
		return false;
	}
}
