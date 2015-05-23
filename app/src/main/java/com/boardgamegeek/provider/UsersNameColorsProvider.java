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

public class UsersNameColorsProvider extends BaseProvider {
	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		String username = PlayerColors.getUsername(uri);
		return new SelectionBuilder().table(Tables.PLAYER_COLORS)
			.where(PlayerColors.PLAYER_TYPE + "=?", String.valueOf(PlayerColors.TYPE_USER))
			.where(PlayerColors.PLAYER_NAME + "=?", username);
	}

	@Override
	protected String getPath() {
		return BggContract.PATH_USERS + "/*/" + BggContract.PATH_COLORS;
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
		String username = PlayerColors.getUsername(uri);
		if (TextUtils.isEmpty(username)) {
			throw new SQLException("Missing username.");
		}

		values.put(PlayerColors.PLAYER_TYPE, PlayerColors.TYPE_USER);
		values.put(PlayerColors.PLAYER_NAME, username);
		db.insertOrThrow(Tables.PLAYER_COLORS, null, values);
		return PlayerColors.buildUserUri(username, values.getAsInteger(PlayerColors.PLAYER_COLOR_SORT_ORDER));
	}
}
