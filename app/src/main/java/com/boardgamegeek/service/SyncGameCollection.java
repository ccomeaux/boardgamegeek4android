package com.boardgamegeek.service;

import android.accounts.Account;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.io.CollectionRequest;
import com.boardgamegeek.io.CollectionResponse;
import com.boardgamegeek.model.CollectionItem;
import com.boardgamegeek.model.persister.CollectionPersister;
import com.boardgamegeek.provider.BggContract;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class SyncGameCollection extends UpdateTask {
	private final int gameId;

	public SyncGameCollection(int gameId) {
		this.gameId = gameId;
	}

	@NonNull
	@Override
	public String getDescription(Context context) {
		if (isValid()) {
			return context.getString(R.string.sync_msg_game_collection_valid, String.valueOf(gameId));
		}
		return context.getString(R.string.sync_msg_game_collection_invalid);
	}

	@Override
	public boolean isValid() {
		return gameId != BggContract.INVALID_ID;
	}

	@Override
	public void execute(Context context) {
		Account account = Authenticator.getAccount(context);
		if (account == null) return;

		CollectionPersister persister = new CollectionPersister.Builder(context)
			.includePrivateInfo()
			.includeStats()
			.build();

		List<CollectionItem> items = request(context, account);
		if (items == null) {
			Timber.w("Didn't find any items for game ID=%s", gameId);
			return;
		}

		persister.save(items);
		Timber.i("Synced %,d collection item(s) for game ID=%s", items.size(), gameId);

		int deleteCount = persister.delete(items, gameId);
		Timber.i("Removed %,d collection item(s) for game ID=%s", deleteCount, gameId);
	}

	private List<CollectionItem> request(Context context, @NonNull Account account) {
		// Only one of these requests will return results
		BggService service = Adapter.createForXmlWithAuth(context);

		ArrayMap<String, String> options = new ArrayMap<>();
		options.put(BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE, "1");
		options.put(BggService.COLLECTION_QUERY_KEY_STATS, "1");
		options.put(BggService.COLLECTION_QUERY_KEY_ID, String.valueOf(gameId));
		List<CollectionItem> items;

		items = requestItems(account, service, options);
		if (items != null && items.size() > 0) return items;

		options.put(BggService.COLLECTION_QUERY_STATUS_PLAYED, "1");
		items = requestItems(account, service, options);
		if (items != null && items.size() > 0) return items;

		options.remove(BggService.COLLECTION_QUERY_STATUS_PLAYED);
		options.put(BggService.COLLECTION_QUERY_KEY_SUBTYPE, BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY);
		items = requestItems(account, service, options);
		if (items != null && items.size() > 0) return items;

		options.put(BggService.COLLECTION_QUERY_STATUS_PLAYED, "1");
		items = requestItems(account, service, options);
		if (items != null && items.size() > 0) return items;

		Timber.i("No collection items for game ID=%s", gameId);
		return null;
	}

	private List<CollectionItem> requestItems(@NonNull Account account, BggService service, ArrayMap<String, String> options) {
		CollectionResponse response = new CollectionRequest(service, account.name, options).execute();
		if (response.hasError()) {
			Timber.w("Failed to get a response from the 'Geek - %s", response.getError());
			return null;
		} else if (response.getNumberOfItems() == 0) {
			Timber.i("No collection items for game ID=%d with options=%s", gameId, options);
			return new ArrayList<>(0);
		} else {
			return response.getItems();
		}
	}
}
