package com.boardgamegeek.service;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.support.annotation.Nullable;
import android.support.annotation.PluralsRes;
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
import com.boardgamegeek.util.ResolverUtils;
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
	private CollectionDeleteTask deleteTask;
	private CollectionAddTask addTask;
	private List<CollectionUploadTask> uploadTasks;

	@DebugLog
	public SyncCollectionUpload(Context context, BggService service) {
		super(context, service);
		okHttpClient = HttpUtils.getHttpClientWithAuth(context);
		deleteTask = new CollectionDeleteTask(okHttpClient);
		addTask = new CollectionAddTask(okHttpClient);
		uploadTasks = createUploadTasks();
		timestampColumns.add(Collection.STATUS_DIRTY_TIMESTAMP);
		timestampColumns.add(Collection.RATING_DIRTY_TIMESTAMP);
		timestampColumns.add(Collection.COMMENT_DIRTY_TIMESTAMP);
		timestampColumns.add(Collection.PRIVATE_INFO_DIRTY_TIMESTAMP);
	}

	private List<CollectionUploadTask> createUploadTasks() {
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
			cursor = fetchDeletedCollectionItems();
			while (cursor != null && cursor.moveToNext()) {
				if (isCancelled()) {
					break;
				}
				processDeletedCollectionItem(cursor);
			}
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}

		cursor = null;
		try {
			cursor = fetchNewCollectionItems();
			while (cursor != null && cursor.moveToNext()) {
				if (isCancelled()) {
					break;
				}
				processNewCollectionItem(cursor);
			}
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}

		cursor = null;
		try {
			cursor = fetchDirtyCollectionItems();
			while (cursor != null && cursor.moveToNext()) {
				if (isCancelled()) {
					break;
				}
				processDirtyCollectionItem(cursor);
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

	private Cursor fetchDeletedCollectionItems() {
		String selection = Collection.COLLECTION_DELETE_TIMESTAMP + ">0";
		return getCollectionItems(selection, R.plurals.sync_notification_collection_deleting);
	}

	private Cursor fetchNewCollectionItems() {
		String selection = Collection.COLLECTION_DIRTY_TIMESTAMP + ">0 AND " + ResolverUtils.generateWhereNullOrEmpty(Collection.COLLECTION_ID);
		return getCollectionItems(selection, R.plurals.sync_notification_collection_adding);
	}

	private Cursor fetchDirtyCollectionItems() {
		String selection = "";
		for (CollectionUploadTask task : uploadTasks) {
			if (!TextUtils.isEmpty(selection)) {
				selection += " OR ";
			}
			selection += task.getTimestampColumn() + ">0";
		}
		return getCollectionItems(selection, R.plurals.sync_notification_collection_uploading);
	}

	@Nullable
	private Cursor getCollectionItems(String selection, @PluralsRes int messageResId) {
		Cursor cursor = context.getContentResolver().query(Collection.CONTENT_URI,
			CollectionItem.PROJECTION,
			selection,
			null,
			null);
		final int count = cursor != null ? cursor.getCount() : 0;
		String detail = context.getResources().getQuantityString(messageResId, count, count);
		Timber.i(detail);
		showNotification(detail);
		return cursor;
	}

	private void processDeletedCollectionItem(Cursor cursor) {
		CollectionItem collectionItem = CollectionItem.fromCursor(cursor);
		deleteTask.addCollectionItem(collectionItem);
		deleteTask.post();
		if (processResponseForError(deleteTask)) {
			return;
		}
		resolver.delete(Collection.buildUri(collectionItem.getInternalId()), null, null);
		notifySuccess(collectionItem.getCollectionName());
	}

	private void processNewCollectionItem(Cursor cursor) {
		CollectionItem collectionItem = CollectionItem.fromCursor(cursor);
		addTask.addCollectionItem(collectionItem);
		addTask.post();
		if (processResponseForError(addTask)) {
			return;
		}
		ContentValues contentValues = new ContentValues();
		addTask.appendContentValues(contentValues);
		resolver.update(Collection.buildUri(collectionItem.getInternalId()), contentValues, null, null);
		UpdateService.start(context, UpdateService.SYNC_TYPE_GAME_COLLECTION, collectionItem.getGameId());
		notifySuccess(collectionItem.getCollectionName());
	}

	private void processDirtyCollectionItem(Cursor cursor) {
		CollectionItem collectionItem = CollectionItem.fromCursor(cursor);
		if (collectionItem.getCollectionId() != BggContract.INVALID_ID) {
			ContentValues contentValues = new ContentValues();
			for (CollectionUploadTask task : uploadTasks) {
				if (processUploadTask(task, collectionItem, contentValues)) return;
			}
			if (contentValues != null && contentValues.size() > 0) {
				resolver.update(Collection.buildUri(collectionItem.getInternalId()), contentValues, null, null);
				notifySuccess(collectionItem.getCollectionName());
			}
		} else {
			Timber.d("Invalid collectionItem ID for internal ID %1$s; game ID %2$s", collectionItem.getInternalId(), collectionItem.getGameId());
		}
	}

	private boolean processUploadTask(CollectionUploadTask task, CollectionItem collectionItem, ContentValues contentValues) {
		task.addCollectionItem(collectionItem);
		if (task.isDirty()) {
			task.post();
			if (processResponseForError(task)) {
				return true;
			}
			task.appendContentValues(contentValues);
		}
		return false;
	}

	private void notifySuccess(String collectionName) {
		syncResult.stats.numUpdates++;
		SpannableString message = StringUtils.boldSecondString(context.getString(R.string.sync_notification_collection_upload_detail), collectionName);
		Timber.i(message.toString());
		notifyUser(message);
	}

	private boolean processResponseForError(CollectionTask response) {
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
