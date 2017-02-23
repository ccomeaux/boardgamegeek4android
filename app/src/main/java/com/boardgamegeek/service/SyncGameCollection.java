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
import com.boardgamegeek.model.persister.CollectionPersister.SaveResults;
import com.boardgamegeek.provider.BggContract;

import java.util.List;

import timber.log.Timber;

/***
 * Syncs the user's collection for the specified game, deleting items that fall outside the selected statuses to sync.
 */
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

		BggService service = Adapter.createForXmlWithAuth(context);

		ArrayMap<String, String> options = new ArrayMap<>();
		options.put(BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE, "1");
		options.put(BggService.COLLECTION_QUERY_KEY_STATS, "1");
		options.put(BggService.COLLECTION_QUERY_KEY_ID, String.valueOf(gameId));

		List<CollectionItem> items;
		CollectionResponse response = new CollectionRequest(service, account.name, options).execute();
		if (response.hasError()) {
			Timber.w("Failed to get a response from the 'Geek - %s", response.getError());
			return;
		} else if (response.getNumberOfItems() > 0) {
			items = response.getItems();
		} else {
			options.put(BggService.COLLECTION_QUERY_KEY_SUBTYPE, BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY);
			response = new CollectionRequest(service, account.name, options).execute();
			if (response.hasError()) {
				Timber.w("Failed to get a response from the 'Geek - %s", response.getError());
				return;
			} else {
				items = response.getItems();
			}
		}

		if (items == null) {
			Timber.w("Didn't find any items for game ID=%s", gameId);
			return;
		}

		SaveResults results = persister.save(items);
		Timber.i("Synced %,d collection item(s) for game ID=%s", items.size(), gameId);

		int deleteCount = persister.delete(gameId, results.getSavedCollectionIds());
		Timber.i("Removed %,d collection item(s) for game ID=%s", deleteCount, gameId);
	}
}
