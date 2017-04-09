package com.boardgamegeek.tasks.sync;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.AccountUtils;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.CollectionResponse;
import com.boardgamegeek.model.persister.CollectionPersister;
import com.boardgamegeek.model.persister.CollectionPersister.SaveResults;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.tasks.sync.SyncCollectionByGameTask.CompletedEvent;

import retrofit2.Call;
import timber.log.Timber;

public class SyncCollectionByGameTask extends SyncTask<CollectionResponse, CompletedEvent> {
	private final int gameId;
	private final String username;
	private final CollectionPersister persister;
	private SaveResults results;

	public SyncCollectionByGameTask(Context context, int gameId) {
		super(context);
		this.gameId = gameId;
		username = AccountUtils.getUsername(context);
		persister = new CollectionPersister.Builder(context)
			.includePrivateInfo()
			.includeStats()
			.build();
	}

	@Override
	@StringRes
	protected int getTypeDescriptionResId() {
		return R.string.title_collection;
	}

	@Override
	protected Call<CollectionResponse> createCall() {
		switch (getCurrentPage()) {
			case 1: {
				ArrayMap<String, String> options = new ArrayMap<>();
				options.put(BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE, "1");
				options.put(BggService.COLLECTION_QUERY_KEY_STATS, "1");
				options.put(BggService.COLLECTION_QUERY_KEY_ID, String.valueOf(gameId));
				return bggService.collection(username, options);
			}
			case 2: {
				ArrayMap<String, String> options = new ArrayMap<>();
				options.put(BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE, "1");
				options.put(BggService.COLLECTION_QUERY_KEY_STATS, "1");
				options.put(BggService.COLLECTION_QUERY_KEY_ID, String.valueOf(gameId));
				return bggService.collection(username, options);
			}
			default:
				return null;
		}
	}

	@Override
	protected boolean isRequestParamsValid() {
		return super.isRequestParamsValid() &&
			gameId != BggContract.INVALID_ID &&
			!TextUtils.isEmpty(username);
	}

	@Override
	protected void persistResponse(CollectionResponse body) {
		results = persister.save(body.items);
		Timber.i("Synced %,d collection item(s) for game '%s'", body.items == null ? 0 : body.items.size(), gameId);
	}

	@Override
	protected boolean hasMorePages(CollectionResponse body) {
		return getCurrentPage() <= 1 && (results == null || results.getRecordCount() == 0);
	}

	@Override
	protected void finishSync() {
		if (results != null) {
			int deleteCount = persister.delete(gameId, results.getSavedCollectionIds());
			Timber.i("Removed %,d collection item(s) for game '%s'", deleteCount, gameId);
		}
	}

	@NonNull
	@Override
	protected CompletedEvent createEvent(String errorMessage) {
		return new CompletedEvent(errorMessage, gameId);
	}

	public class CompletedEvent extends SyncTask.CompletedEvent {
		private final int gameId;

		public CompletedEvent(String errorMessage, int gameId) {
			super(errorMessage);
			this.gameId = gameId;
		}

		public int getGameId() {
			return gameId;
		}
	}
}
