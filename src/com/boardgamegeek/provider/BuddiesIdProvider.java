package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.util.SelectionBuilder;

public class BuddiesIdProvider extends BaseProvider {
	BuddiesProvider mProvider = new BuddiesProvider();

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int buddyId = Buddies.getBuddyId(uri);
		return mProvider.buildSimpleSelection(uri).whereEquals(Buddies.BUDDY_ID, buddyId);
	}

	@Override
	protected String getPath() {
		return addIdToPath(mProvider.getPath());
	}

	@Override
	protected String getType(Uri uri) {
		return Buddies.CONTENT_ITEM_TYPE;
	}
}
