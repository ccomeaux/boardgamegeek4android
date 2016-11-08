package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.util.SelectionBuilder;

public class BuddiesIdProvider extends BaseProvider {
	final BuddiesProvider provider = new BuddiesProvider();

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		String buddyName = Buddies.getBuddyName(uri);
		return provider.buildSimpleSelection(uri).whereEquals(Buddies.BUDDY_NAME, buddyName);
	}

	@Override
	protected String getPath() {
		return addWildCardToPath(provider.getPath());
	}

	@Override
	protected String getType(Uri uri) {
		return Buddies.CONTENT_ITEM_TYPE;
	}
}
