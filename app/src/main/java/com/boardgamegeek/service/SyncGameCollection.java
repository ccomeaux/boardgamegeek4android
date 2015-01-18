package com.boardgamegeek.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.accounts.Account;
import android.content.Context;

import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.CollectionItem;
import com.boardgamegeek.model.CollectionResponse;
import com.boardgamegeek.model.persister.CollectionPersister;

import timber.log.Timber;

public class SyncGameCollection extends UpdateTask {
	private static final String STATUS_PLAYED = "played";

	private int mGameId;

	public SyncGameCollection(int gameId) {
		mGameId = gameId;
	}

	@Override
	public String getDescription() {
		return "Sync collection for game ID=" + mGameId;
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
		Timber.i("Synced " + (items == null ? 0 : items.size()) + " collection item(s) for game ID=" + mGameId);

		// XXX: this deleted more games that I expected. need to rework
		// int deleteCount = persister.delete(items, mGameId);
		// Timber.i("Removed " + deleteCount + " collection item(s) for game ID=" + mGameId);
	}

	protected List<CollectionItem> request(Context context, Account account) {
		// Only one of these requests will return results
		BggService service = Adapter.createWithAuth(context);

		Map<String, String> options = new HashMap<String, String>();
		options.put(BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE, "1");
		options.put(BggService.COLLECTION_QUERY_KEY_STATS, "1");
		options.put(BggService.COLLECTION_QUERY_KEY_ID, String.valueOf(mGameId));
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

		Timber.i("No collection items for game ID=" + mGameId);
		return null;
	}

	private List<CollectionItem> requestItems(Account account, BggService service, Map<String, String> options) {
		CollectionResponse response = getCollectionResponse(service, account.name, options);
		if (response == null || response.items == null || response.items.size() == 0) {
			Timber.i("No collection items for game ID=" + mGameId + " with options=" + options);
			return null;
		} else {
			return response.items;
		}
	}
}
