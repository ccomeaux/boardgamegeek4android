package com.boardgamegeek.service;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.support.annotation.Nullable;
import android.support.annotation.PluralsRes;
import android.support.annotation.StringRes;

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

import java.util.ArrayList;
import java.util.List;

import hugo.weaving.DebugLog;
import okhttp3.OkHttpClient;
import timber.log.Timber;

public class SyncCollectionUpload extends SyncUploadTask {
	private ContentResolver resolver;
	private SyncResult syncResult;
	private final OkHttpClient okHttpClient;
	private final CollectionDeleteTask deleteTask;
	private final CollectionAddTask addTask;
	private final List<CollectionUploadTask> uploadTasks;

	@DebugLog
	public SyncCollectionUpload(Context context, BggService service) {
		super(context, service);
		okHttpClient = HttpUtils.getHttpClientWithAuth(context);
		deleteTask = new CollectionDeleteTask(okHttpClient);
		addTask = new CollectionAddTask(okHttpClient);
		uploadTasks = createUploadTasks();
	}

	private List<CollectionUploadTask> createUploadTasks() {
		List<CollectionUploadTask> tasks = new ArrayList<>();
		tasks.add(new CollectionStatusUploadTask(okHttpClient));
		tasks.add(new CollectionRatingUploadTask(okHttpClient));
		tasks.add(new CollectionCommentUploadTask(okHttpClient));
		tasks.add(new CollectionPrivateInfoUploadTask(okHttpClient));
		tasks.add(new CollectionWishlistCommentUploadTask(okHttpClient));
		tasks.add(new CollectionTradeConditionUploadTask(okHttpClient));
		tasks.add(new CollectionWantPartsUploadTask(okHttpClient));
		tasks.add(new CollectionHasPartsUploadTask(okHttpClient));
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
	protected String getNotificationMessageTag() {
		return NotificationUtils.TAG_UPLOAD_COLLECTION;
	}

	@DebugLog
	@Override
	protected String getNotificationErrorTag() {
		return NotificationUtils.TAG_UPLOAD_COLLECTION_ERROR;
	}

	@DebugLog
	@Override
	public void execute(Account account, SyncResult syncResult) {
		init(syncResult);

		Cursor cursor = null;
		try {
			cursor = fetchDeletedCollectionItems();
			while (cursor != null && cursor.moveToNext()) {
				if (isCancelled()) break;
				if (wasSleepInterrupted(1000)) break;
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
				if (isCancelled()) break;
				if (wasSleepInterrupted(1000)) break;
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
				if (isCancelled()) break;
				if (wasSleepInterrupted(1000)) break;
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
		return getCollectionItems(isGreaterThanZero(Collection.COLLECTION_DELETE_TIMESTAMP), R.plurals.sync_notification_collection_deleting);
	}

	private Cursor fetchNewCollectionItems() {
		String selection = "(" + getDirtyColumnSelection(isGreaterThanZero(Collection.COLLECTION_DIRTY_TIMESTAMP)) + ") AND " +
			ResolverUtils.generateWhereNullOrEmpty(Collection.COLLECTION_ID);
		return getCollectionItems(selection, R.plurals.sync_notification_collection_adding);
	}

	private Cursor fetchDirtyCollectionItems() {
		String selection = getDirtyColumnSelection("");
		return getCollectionItems(selection, R.plurals.sync_notification_collection_uploading);
	}

	private String getDirtyColumnSelection(String existingSelection) {
		StringBuilder sb = new StringBuilder(existingSelection);
		for (CollectionUploadTask task : uploadTasks) {
			if (sb.length() > 0) sb.append(" OR ");
			sb.append(isGreaterThanZero(task.getTimestampColumn()));
		}
		return sb.toString();
	}

	private static String isGreaterThanZero(String columnName) {
		return columnName + ">0";
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
		updateProgressNotification(detail);
		return cursor;
	}

	private void processDeletedCollectionItem(Cursor cursor) {
		CollectionItem item = CollectionItem.fromCursor(cursor);
		deleteTask.addCollectionItem(item);
		deleteTask.post();
		if (processResponseForError(deleteTask)) {
			return;
		}
		resolver.delete(Collection.buildUri(item.getInternalId()), null, null);
		notifySuccess(item, item.getCollectionId(), R.string.sync_notification_collection_deleted);
	}

	private void processNewCollectionItem(Cursor cursor) {
		CollectionItem item = CollectionItem.fromCursor(cursor);
		addTask.addCollectionItem(item);
		addTask.post();
		if (processResponseForError(addTask)) {
			return;
		}
		ContentValues contentValues = new ContentValues();
		addTask.appendContentValues(contentValues);
		resolver.update(Collection.buildUri(item.getInternalId()), contentValues, null, null);
		UpdateService.start(context, UpdateService.SYNC_TYPE_GAME_COLLECTION, item.getGameId());
		notifySuccess(item, item.getGameId() * -1, R.string.sync_notification_collection_added);
	}

	private void processDirtyCollectionItem(Cursor cursor) {
		CollectionItem item = CollectionItem.fromCursor(cursor);
		if (item.getCollectionId() != BggContract.INVALID_ID) {
			ContentValues contentValues = new ContentValues();
			for (CollectionUploadTask task : uploadTasks) {
				if (processUploadTask(task, item, contentValues)) return;
			}
			if (contentValues.size() > 0) {
				resolver.update(Collection.buildUri(item.getInternalId()), contentValues, null, null);
				notifySuccess(item, item.getCollectionId(), R.string.sync_notification_collection_updated);
			}
		} else {
			Timber.d("Invalid collectionItem ID for internal ID %1$s; game ID %2$s", item.getInternalId(), item.getGameId());
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

	private void notifySuccess(CollectionItem item, int id, @StringRes int messageResId) {
		syncResult.stats.numUpdates++;
		notifyUser(item.getCollectionName(), context.getString(messageResId), id, item.getImageUrl(), item.getThumbnailUrl());
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
