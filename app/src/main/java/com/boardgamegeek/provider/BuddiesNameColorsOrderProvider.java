package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.PlayerColors;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class BuddiesNameColorsOrderProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		String buddyName = PlayerColors.getBuddyName(uri);
		int sortOrder = PlayerColors.getSortOrder(uri);
		return new SelectionBuilder().table(Tables.PLAYER_COLORS)
			.where(PlayerColors.PLAYER_TYPE + "=?", String.valueOf(PlayerColors.TYPE_BUDDY))
			.where(PlayerColors.PLAYER_NAME + "=?", buddyName)
			.where(PlayerColors.PLAYER_COLOR_SORT_ORDER + "=?", String.valueOf(sortOrder));
	}

	@Override
	protected String getPath() {
		return BggContract.PATH_BUDDIES + "/*/" + BggContract.PATH_COLORS + "/#";
	}

	@Override
	protected String getType(Uri uri) {
		return PlayerColors.CONTENT_ITEM_TYPE;
	}
}
