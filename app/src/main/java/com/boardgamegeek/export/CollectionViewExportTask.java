package com.boardgamegeek.export;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.boardgamegeek.export.model.CollectionView;
import com.boardgamegeek.provider.BggContract.CollectionViews;
import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

public class CollectionViewExportTask extends JsonExportTask<CollectionView> {
	public CollectionViewExportTask(Context context, Uri uri) {
		super(context, Constants.TYPE_COLLECTION_VIEWS, uri);
	}

	@Override
	protected Cursor getCursor(Context context) {
		return context.getContentResolver().query(
			CollectionViews.CONTENT_URI,
			CollectionView.PROJECTION,
			null, null, null);
	}

	@Override
	protected void writeJsonRecord(Context context, Cursor cursor, Gson gson, JsonWriter writer) {
		CollectionView cv = CollectionView.fromCursor(cursor);
		cv.addFilters(context);
		gson.toJson(cv, CollectionView.class, writer);
	}
}
