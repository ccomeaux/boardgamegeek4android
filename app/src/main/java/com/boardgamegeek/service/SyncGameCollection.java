package com.boardgamegeek.service;

import android.accounts.Account;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;

import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.io.CollectionRequest;
import com.boardgamegeek.model.CollectionItem;
import com.boardgamegeek.model.CollectionResponse;
import com.boardgamegeek.model.persister.CollectionPersister;
import com.boardgamegeek.provider.BggContract;

import java.util.List;

import timber.log.Timber;

public class SyncGameCollection extends UpdateTask {
	private static final String STATUS_PLAYED = "played";

	private final int gameId;

	public SyncGameCollection(int gameId) {
		this.gameId = gameId;
	}

	@NonNull
	@Override
	public String getDescription() {
		// TODO use resources for description
		if (isValid()) {
			return "update collection for game " + gameId;
		}
		return "update collection for unknown game";
	}

	@Override
	public boolean isValid() {
		return gameId != BggContract.INVALID_ID;
	}

	@Override
	public void execute(Context context) {
		Account account = Authenticator.getAccount(context);
		if (account == null) {
			return;
		}

		List<CollectionItem> items = request(context, account);
		CollectionPersister persister = new CollectionPersister(context).includePrivateInfo().includeStats();
		persister.save(items);
		Timber.i("Synced " + (items == null ? 0 : items.size()) + " collection item(s) for game ID=" + gameId);

		// XXX: this deleted more games that I expected. need to rework
		// int deleteCount = persister.delete(items, gameId);
		// Timber.i("Removed " + deleteCount + " collection item(s) for game ID=" + gameId);
	}

	private List<CollectionItem> request(Context context, @NonNull Account account) {
		// Only one of these requests will return results
		BggService service = Adapter.createWithAuth(context);

		ArrayMap<String, String> options = new ArrayMap<>();
		options.put(BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE, "1");
		options.put(BggService.COLLECTION_QUERY_KEY_STATS, "1");
		options.put(BggService.COLLECTION_QUERY_KEY_ID, String.valueOf(gameId));
		List<CollectionItem> items;

		items = requestItems(account, service, options);
		if (items != null) {
			return items;
		}

		options.put(STATUS_PLAYED, "1");
		items = requestItems(account, service, options);
		if (items != null) {
			return items;
		}

		options.remove(STATUS_PLAYED);
		options.put(BggService.COLLECTION_QUERY_KEY_SUBTYPE, BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY);
		items = requestItems(account, service, options);
		if (items != null) {
			return items;
		}

		options.put(STATUS_PLAYED, "1");
		items = requestItems(account, service, options);
		if (items != null) {
			return items;
		}

		Timber.i("No collection items for game ID=" + gameId);
		return null;
	}

	private List<CollectionItem> requestItems(@NonNull Account account, BggService service, ArrayMap<String, String> options) {
		CollectionResponse response = new CollectionRequest(service, account.name, options).execute();
		if (response == null || response.items == null || response.items.size() == 0) {
			Timber.i("No collection items for game ID=" + gameId + " with options=" + options);
			return null;
		} else {
			return response.items;
		}
	}
}
