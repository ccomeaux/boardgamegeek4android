package com.boardgamegeek.export;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.boardgamegeek.R;
import com.boardgamegeek.export.model.CollectionView;
import com.boardgamegeek.export.model.Filter;
import com.boardgamegeek.provider.BggContract.CollectionViewFilters;
import com.boardgamegeek.provider.BggContract.CollectionViews;
import com.boardgamegeek.util.ResolverUtils;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.util.ArrayList;

public class CollectionViewStep implements Step {
	@NonNull
	@Override
	public String getFileName() {
		return "bgg4a-collection-views.json";
	}

	@NonNull
	@Override
	public String getDescription(@NonNull Context context) {
		return context.getString(R.string.backup_type_collection_view);
	}

	@Nullable
	@Override
	public Cursor getCursor(@NonNull Context context) {
		return context.getContentResolver().query(
			CollectionViews.CONTENT_URI,
			CollectionView.PROJECTION,
			null, null, null);
	}

	@Override
	public void writeJsonRecord(@NonNull Context context, @NonNull Cursor cursor, @NonNull Gson gson, @NonNull JsonWriter writer) {
		CollectionView cv = CollectionView.fromCursor(cursor);
		cv.addFilters(context);
		gson.toJson(cv, CollectionView.class, writer);
	}

	@Override
	public void initializeImport(@NonNull Context context) {
		context.getContentResolver().delete(CollectionViews.CONTENT_URI, null, null);
	}

	@Override
	public void importRecord(@NonNull Context context, @NonNull Gson gson, @NonNull JsonReader reader) {
		CollectionView cv = gson.fromJson(reader, CollectionView.class);

		ContentResolver resolver = context.getContentResolver();

		ContentValues values = new ContentValues();
		values.put(CollectionViews.NAME, cv.getName());
		values.put(CollectionViews.STARRED, cv.isStarred());
		values.put(CollectionViews.SORT_TYPE, cv.getSortType());
		Uri uri = resolver.insert(CollectionViews.CONTENT_URI, values);

		if (cv.getFilters() == null || cv.getFilters().size() == 0) {
			return;
		}

		int viewId = CollectionViews.getViewId(uri);
		Uri filterUri = CollectionViews.buildViewFilterUri(viewId);

		ArrayList<ContentProviderOperation> batch = new ArrayList<>();
		for (Filter filter : cv.getFilters()) {
			ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(filterUri);
			builder.withValue(CollectionViewFilters.TYPE, filter.getType());
			builder.withValue(CollectionViewFilters.DATA, filter.getData());
			batch.add(builder.build());
		}
		ResolverUtils.applyBatch(context, batch);
	}
}
