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
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.io.BoardGameGeekService;
import com.boardgamegeek.model.CollectionCommentPostResponse;
import com.boardgamegeek.model.CollectionPostResponse;
import com.boardgamegeek.model.CollectionRatingPostResponse;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.ui.CollectionActivity;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.NotificationUtils;
import com.boardgamegeek.util.StringUtils;

import hugo.weaving.DebugLog;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import timber.log.Timber;

public class SyncCollectionUpload extends SyncUploadTask {
	public static final String GEEK_COLLECTION_URL = "https://www.boardgamegeek.com/geekcollection.php";
	ContentResolver resolver;
	SyncResult syncResult;
	private ContentValues contentValues;

	@DebugLog
	public SyncCollectionUpload(Context context, BggService bggService, BoardGameGeekService service) {
		super(context, bggService, service);
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
			Collection.RATING_DIRTY_TIMESTAMP + ">0 OR " + Collection.COMMENT_DIRTY_TIMESTAMP + ">0",
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
		long ratingTimestamp = cursor.getLong(Query.RATING_DIRTY_TIMESTAMP);
		String comment = cursor.getString(Query.COMMENT);
		long commentTimestamp = cursor.getLong(Query.COMMENT_DIRTY_TIMESTAMP);
		long internalId = cursor.getLong(Query._ID);
		String collectionName = cursor.getString(Query.COLLECTION_NAME);

		if (collectionId != BggContract.INVALID_ID) {
			contentValues = new ContentValues();
			if (ratingTimestamp > 0) {
				FormBody form = createRatingForm(gameId, collectionId, rating);
				CollectionRatingPostResponse response = postRatingForm(form);
				if (processResponseForError(response)) {
					return;
				}
				createRatingContentValues(response.getRating());
			}
			if (commentTimestamp > 0) {
				FormBody form = createCommentForm(gameId, collectionId, comment);
				CollectionCommentPostResponse response = postCommentForm(form);
				if (processResponseForError(response)) {
					return;
				}
				createCommentContentValues(response.getComment());
			}
			if (contentValues != null && contentValues.size() > 0) {
				resolver.update(Collection.buildUri(internalId), contentValues, null, null);
				notifySuccess(collectionName);
			}
		} else {
			Timber.d("Invalid collection ID for internal ID %1$s; game ID %2$s", internalId, gameId);
		}
	}

	private FormBody createRatingForm(int gameId, int collectionId, double rating) {
		return createForm(gameId, collectionId)
			.add("fieldname", "rating")
			.add("rating", String.valueOf(rating))
			.build();
	}

	private FormBody createCommentForm(int gameId, int collectionId, String comment) {
		return createForm(gameId, collectionId)
			.add("fieldname", "comment")
			.add("value", comment)
			.build();
	}

	@NonNull
	private FormBody.Builder createForm(int gameId, int collectionId) {
		return new FormBody.Builder()
			.add("B1", "Cancel")
			.add("ajax", "1")
			.add("action", "savedata")
			.add("objecttype", "thing")
			.add("objectid", String.valueOf(gameId))
			.add("collid", String.valueOf(collectionId));
	}

	private CollectionRatingPostResponse postRatingForm(FormBody form) {
		OkHttpClient client = HttpUtils.getHttpClientWithAuth(false, context);
		Request request = new Builder()
			.url(GEEK_COLLECTION_URL)
			.post(form)
			.build();
		return new CollectionRatingPostResponse(client, request);
	}


	private CollectionCommentPostResponse postCommentForm(FormBody form) {
		OkHttpClient client = HttpUtils.getHttpClientWithAuth(false, context);
		Request request = new Builder()
			.url(GEEK_COLLECTION_URL)
			.post(form)
			.build();
		return new CollectionCommentPostResponse(client, request);
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

	private void createRatingContentValues(double rating) {
		contentValues.put(Collection.RATING, rating);
		contentValues.put(Collection.RATING_DIRTY_TIMESTAMP, 0);
	}

	private void createCommentContentValues(String comment) {
		contentValues.put(Collection.COMMENT, comment);
		contentValues.put(Collection.COMMENT_DIRTY_TIMESTAMP, 0);
	}

	private interface Query {
		String[] PROJECTION = {
			Collection._ID,
			Collection.GAME_ID,
			Collection.COLLECTION_ID,
			Collection.COLLECTION_NAME,
			Collection.RATING,
			Collection.RATING_DIRTY_TIMESTAMP,
			Collection.COMMENT,
			Collection.COMMENT_DIRTY_TIMESTAMP
		};
		int _ID = 0;
		int GAME_ID = 1;
		int COLLECTION_ID = 2;
		int COLLECTION_NAME = 3;
		int RATING = 4;
		int RATING_DIRTY_TIMESTAMP = 5;
		int COMMENT = 6;
		int COMMENT_DIRTY_TIMESTAMP = 7;
	}
}
