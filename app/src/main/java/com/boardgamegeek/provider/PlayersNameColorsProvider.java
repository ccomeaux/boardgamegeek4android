package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;

import com.boardgamegeek.provider.BggContract.PlayerColors;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class PlayersNameColorsProvider extends BaseProvider {
	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		String playerName = PlayerColors.getPlayerName(uri);
		return new SelectionBuilder().table(Tables.PLAYER_COLORS)
			.where(PlayerColors.PLAYER_TYPE + "=?", String.valueOf(PlayerColors.TYPE_PLAYER))
			.where(PlayerColors.PLAYER_NAME + "=?", playerName);
	}

	@Override
	protected String getPath() {
		return BggContract.PATH_PLAYERS + "/*/" + BggContract.PATH_COLORS;
	}

	@Override
	protected String getDefaultSortOrder() {
		return PlayerColors.DEFAULT_SORT;
	}

	@Override
	protected String getType(Uri uri) {
		return PlayerColors.CONTENT_TYPE;
	}

	@Override
	protected Uri insert(Context context, SQLiteDatabase db, Uri uri, ContentValues values) {
		String playerName = PlayerColors.getPlayerName(uri);
		if (TextUtils.isEmpty(playerName)) {
			throw new SQLException("Missing player name.");
		}

		values.put(PlayerColors.PLAYER_TYPE, PlayerColors.TYPE_PLAYER);
		values.put(PlayerColors.PLAYER_NAME, playerName);
		db.insertOrThrow(Tables.PLAYER_COLORS, null, values);
		return PlayerColors.buildUserUri(playerName, values.getAsInteger(PlayerColors.PLAYER_COLOR_SORT_ORDER));
	}
}
