package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.PlayerColors;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class PlayersNameColorsOrderProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		String playerName = PlayerColors.getPlayerName(uri);
		int sortOrder = PlayerColors.getSortOrder(uri);
		return new SelectionBuilder().table(Tables.PLAYER_COLORS)
			.where(PlayerColors.PLAYER_TYPE + "=?", String.valueOf(PlayerColors.TYPE_PLAYER))
			.where(PlayerColors.PLAYER_NAME + "=?", playerName)
			.where(PlayerColors.PLAYER_COLOR_SORT_ORDER + "=?", String.valueOf(sortOrder));
	}

	@Override
	protected String getPath() {
		return BggContract.PATH_PLAYERS + "/*/" + BggContract.PATH_COLORS + "/#";
	}

	@Override
	protected String getType(Uri uri) {
		return PlayerColors.CONTENT_ITEM_TYPE;
	}
}
