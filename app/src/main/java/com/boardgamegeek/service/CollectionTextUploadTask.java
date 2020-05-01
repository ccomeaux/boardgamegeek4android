package com.boardgamegeek.service;

import android.content.ContentValues;

import com.boardgamegeek.service.model.CollectionItem;

import androidx.annotation.NonNull;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;

public abstract class CollectionTextUploadTask extends CollectionUploadTask {
	protected String text;

	public CollectionTextUploadTask(OkHttpClient client) {
		super(client);
	}

	@NonNull
	protected abstract String getFieldName();

	protected abstract String getValue(CollectionItem collectionItem);

	@Override
	protected FormBody createForm(CollectionItem collectionItem) {
		return createFormBuilder()
			.add("fieldname", getFieldName())
			.add("value", getValue(collectionItem))
			.build();
	}

	@Override
	protected void saveContent(String content) {
		text = content;
	}

	@Override
	public void appendContentValues(ContentValues contentValues) {
		// Don't save text. The response to the POST translates markdown into HTML
		contentValues.put(getTimestampColumn(), 0);
	}
}
