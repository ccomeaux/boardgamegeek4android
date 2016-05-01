package com.boardgamegeek.service;

import android.content.ContentValues;

import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.service.model.CollectionItem;
import com.boardgamegeek.util.StringUtils;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;

public class CollectionRatingUploadTask extends CollectionUploadTask {
	private static final String N_A_SPAN = "<span>N/A</span>";
	private static final String RATING_DIV = "<div class='ratingtext'>";
	public static final double INVALID_RATING = -1.0;
	private double rating;

	public CollectionRatingUploadTask(OkHttpClient client) {
		super(client);
	}

	@Override
	public String getTimestampColumn() {
		return Collection.RATING_DIRTY_TIMESTAMP;
	}

	@Override
	public void addCollectionItem(CollectionItem collectionItem) {
		super.addCollectionItem(collectionItem);
		rating = INVALID_RATING;
	}

	@Override
	public boolean isDirty() {
		return collectionItem.getRatingTimestamp() > 0;
	}

	@Override
	protected FormBody createForm(CollectionItem collectionItem) {
		return createFormBuilder()
			.add("fieldname", "rating")
			.add("rating", String.valueOf(collectionItem.getRating()))
			.build();
	}

	@Override
	protected void saveContent(String content) {
		if (content.contains(N_A_SPAN)) {
			rating = CollectionRatingUploadTask.INVALID_RATING;
		} else if (content.contains(RATING_DIV)) {
			int index = content.indexOf(RATING_DIV) + RATING_DIV.length();
			String message = content.substring(index);
			index = message.indexOf("<");
			if (index > 0) {
				message = message.substring(0, index);
			}
			rating = StringUtils.parseDouble(message.trim(), CollectionRatingUploadTask.INVALID_RATING);
		} else {
			rating = CollectionRatingUploadTask.INVALID_RATING;
		}
	}

	@Override
	public void appendContentValues(ContentValues contentValues) {
		contentValues.put(Collection.RATING, rating);
		contentValues.put(Collection.RATING_DIRTY_TIMESTAMP, 0);
	}
}
