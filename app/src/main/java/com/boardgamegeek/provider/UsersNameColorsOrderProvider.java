package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.PlayerColors;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class UsersNameColorsOrderProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		String username = PlayerColors.getUsername(uri);
		int sortOrder = PlayerColors.getSortOrder(uri);
		return new SelectionBuilder().table(Tables.PLAYER_COLORS)
			.where(PlayerColors.PLAYER_TYPE + "=?", String.valueOf(PlayerColors.TYPE_USER))
			.where(PlayerColors.PLAYER_NAME + "=?", username)
			.where(PlayerColors.PLAYER_COLOR_SORT_ORDER + "=?", String.valueOf(sortOrder));
	}

	@Override
	protected String getPath() {
		return BggContract.PATH_USERS + "/*/" + BggContract.PATH_COLORS + "/#";
	}

	@Override
	protected String getType(Uri uri) {
		return PlayerColors.CONTENT_ITEM_TYPE;
	}
}
