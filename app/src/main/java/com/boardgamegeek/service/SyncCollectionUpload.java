package com.boardgamegeek.service;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.text.SpannableString;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.io.CollectionRatingConverter;
import com.boardgamegeek.io.PostConverter;
import com.boardgamegeek.model.CollectionPostResponse;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.ui.CollectionActivity;
import com.boardgamegeek.util.NotificationUtils;
import com.boardgamegeek.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

import hugo.weaving.DebugLog;
import timber.log.Timber;

public class SyncCollectionUpload extends SyncUploadTask {
	ContentResolver resolver;
	SyncResult syncResult;
	private ContentValues contentValues;

	@DebugLog
	public SyncCollectionUpload(Context context, BggService service) {
		super(context, service);
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
		Cursor cursor = context.getContentResolver().query(Collection.CONTENT_URI,
			Query.PROJECTION,
			Collection.RATING_DIRTY_TIMESTAMP + ">0",
			null,
			null);
		final int count = cursor != null ? cursor.getCount() : 0;
		String detail = context.getResources().getQuantityString(R.plurals.sync_notification_collection_uploading, count, count);
		Timber.i(detail);
		showNotification(detail);
		return cursor;
	}

	private void processCollectionItem(Cursor cursor) {
		int collectionId = cursor.getInt(Query.COLLECTION_ID);
		int gameId = cursor.getInt(Query.GAME_ID);
		double rating = cursor.getDouble(Query.RATING);
		long internalId = cursor.getLong(Query._ID);
		String collectionName = cursor.getString(Query.COLLECTION_NAME);
		long ratingTimestamp = cursor.getLong(Query.RATING_DIRTY_TIMESTAMP);

		if (collectionId != BggContract.INVALID_ID) {
			contentValues = new ContentValues();
			if (ratingTimestamp > 0) {
				Map<String, String> form = createRatingForm(gameId, collectionId, rating);
				CollectionPostResponse response = postForm(form, new CollectionRatingConverter());
				if (processResponseForError(response)) {
					return;
				}
				contentValues = createRatingContentValues(response.getRating());
			}
			if (contentValues != null && contentValues.size() > 0) {
				resolver.update(Collection.buildUri(internalId), contentValues, null, null);
				notifySuccess(collectionName);
			}
		} else {
			Timber.d("Invalid collection ID for internal ID %1$s; game ID %2$s", internalId, gameId);
		}
	}

	private Map<String, String> createRatingForm(int gameId, int collectionId, double rating) {
		Map<String, String> form = createForm(gameId, collectionId);
		form.put("fieldname", "rating");
		form.put("rating", String.valueOf(rating));
		return form;
	}

	@NonNull
	private Map<String, String> createForm(int gameId, int collectionId) {
		Map<String, String> form = new HashMap<>();
		form.put("ajax", "1");
		form.put("action", "savedata");
		form.put("objecttype", "thing");
		form.put("objectid", String.valueOf(gameId));
		form.put("collid", String.valueOf(collectionId));
		return form;
	}

	private CollectionPostResponse postForm(Map<String, String> form, PostConverter converter) {
		CollectionPostResponse response;
		try {
			BggService service = Adapter.createForPost(context, converter);
			response = service.geekCollection(form);
		} catch (Exception e) {
			response = new CollectionPostResponse(e);
		}
		return response;
	}

	private void notifySuccess(String collectionName) {
		syncResult.stats.numUpdates++;
		SpannableString message = StringUtils.boldSecondString(context.getString(R.string.sync_notification_collection_upload_detail), collectionName);
		Timber.i(message.toString());
		notifyUser(message);
	}

	private boolean processResponseForError(CollectionPostResponse response) {
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

	@NonNull
	private ContentValues createRatingContentValues(double rating) {
		ContentValues values = new ContentValues(2);
		values.put(Collection.RATING, rating);
		values.put(Collection.RATING_DIRTY_TIMESTAMP, 0);
		return values;
	}

	private interface Query {
		String[] PROJECTION = {
			Collection._ID,
			Collection.GAME_ID,
			Collection.COLLECTION_ID,
			Collection.COLLECTION_NAME,
			Collection.RATING,
			Collection.RATING_DIRTY_TIMESTAMP
		};
		int _ID = 0;
		int GAME_ID = 1;
		int COLLECTION_ID = 2;
		int COLLECTION_NAME = 3;
		int RATING = 4;
		int RATING_DIRTY_TIMESTAMP = 5;
	}
}
