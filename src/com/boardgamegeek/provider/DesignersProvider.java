package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class DesignersProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		return new SelectionBuilder().table(Tables.DESIGNERS);
	}

	@Override
	protected String getPath() {
		return "designers";
	}

	@Override
	protected String getType(Uri uri) {
		return Designers.CONTENT_TYPE;
	}

	@Override
	protected Uri insert(SQLiteDatabase db, Uri uri, ContentValues values) {
		if (db.insertOrThrow(Tables.DESIGNERS, null, values) == -1) {
			throw new UnsupportedOperationException("Error inserting: " + uri);
		}
		return Designers.buildDesignerUri(values.getAsInteger(Designers.DESIGNER_ID));
	}
}
