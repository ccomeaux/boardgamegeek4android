package com.boardgamegeek.tasks.sync;


import android.text.TextUtils;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.auth.AccountUtils;
import com.boardgamegeek.db.CollectionDao;
import com.boardgamegeek.entities.CollectionItemEntity;
import com.boardgamegeek.entities.CollectionItemGameEntity;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.io.model.CollectionItem;
import com.boardgamegeek.io.model.CollectionResponse;
import com.boardgamegeek.mappers.CollectionItemMapper;
import com.boardgamegeek.provider.BggContract;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.StringRes;
import androidx.collection.ArrayMap;
import kotlin.Pair;
import retrofit2.Call;
import timber.log.Timber;

public class SyncCollectionByGameTask extends SyncTask<CollectionResponse> {
	private final int gameId;
	private final String username;
	private final CollectionDao dao;
	private final List<Integer> results = new ArrayList<>();
	private long timestamp;

	public SyncCollectionByGameTask(BggApplication application, int gameId) {
		super(application.getApplicationContext());
		this.gameId = gameId;
		username = AccountUtils.getUsername(context);
		dao = new CollectionDao(application);
	}

	@Override
	@StringRes
	protected int getTypeDescriptionResId() {
		return R.string.title_collection;
	}

	@Override
	protected BggService createService() {
		return Adapter.createForXmlWithAuth(context);
	}

	@Override
	protected Call<CollectionResponse> createCall() {
		timestamp = System.currentTimeMillis();
		ArrayMap<String, String> options = new ArrayMap<>();
		options.put(BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE, "1");
		options.put(BggService.COLLECTION_QUERY_KEY_STATS, "1");
		options.put(BggService.COLLECTION_QUERY_KEY_ID, String.valueOf(gameId));
		return bggService.collection(username, options);
	}

	@Override
	protected boolean isRequestParamsValid() {
		return gameId != BggContract.INVALID_ID && !TextUtils.isEmpty(username);
	}

	@Override
	protected void persistResponse(CollectionResponse body) {
		results.clear();
		if (body != null && body.items != null) {
			CollectionItemMapper mapper = new CollectionItemMapper();
			for (CollectionItem item : body.items) {
				final Pair<CollectionItemEntity, CollectionItemGameEntity> entities = mapper.map(item);
				int collectionId = dao.saveItem(entities.getFirst(), entities.getSecond(), timestamp, true, true, false);
				results.add(collectionId);
			}
			Timber.i("Synced %,d collection item(s) for game '%s'", body.items.size(), gameId);
		} else {
			Timber.i("No collection items for game '%s'", gameId);
		}
	}

	@Override
	protected void finishSync() {
		int deleteCount = dao.delete(gameId, results);
		Timber.i("Removed %,d collection item(s) for game '%s'", deleteCount, gameId);
	}
}
