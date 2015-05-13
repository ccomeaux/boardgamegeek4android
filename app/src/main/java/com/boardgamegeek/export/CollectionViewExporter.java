package com.boardgamegeek.export;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.export.model.CollectionView;
import com.boardgamegeek.provider.BggContract.CollectionViews;
import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

public class CollectionViewExporter extends Exporter {
	@Override
	public String getFileName() {
		return "collection-views.json";
	}

	@Override
	public Cursor getCursor(Context context) {
		return context.getContentResolver().query(
			CollectionViews.CONTENT_URI,
			CollectionView.PROJECTION,
			null, null, null);
	}

	@Override
	public void writeJsonRecord(Context context, Cursor cursor, Gson gson, JsonWriter writer) {
		CollectionView cv = CollectionView.fromCursor(cursor);
		cv.addFilters(context);
		gson.toJson(cv, CollectionView.class, writer);
	}
}
