package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class BuddiesProvider extends BaseProvider {
	private static final String TABLE = Tables.BUDDIES;

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		return new SelectionBuilder().table(TABLE);
	}

	@Override
	protected String getPath() {
		return "buddies";
	}

	@Override
	protected String getType(Uri uri) {
		return Buddies.CONTENT_TYPE;
	}

	@Override
	protected Uri insert(SQLiteDatabase db, Uri uri, ContentValues values) {
		insert(db, uri, values, TABLE);
		return Buddies.buildBuddyUri(values.getAsInteger(Buddies.BUDDY_ID));
	}

}
