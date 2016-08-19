package com.boardgamegeek.service;

import android.content.ContentValues;
import android.support.annotation.NonNull;

import com.boardgamegeek.service.model.CollectionItem;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;

public abstract class CollectionTextUploadTask extends CollectionUploadTask {
	protected String text;

	public CollectionTextUploadTask(OkHttpClient client) {
		super(client);
	}

	@NonNull
	protected abstract String getTextColumn();

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
		contentValues.put(getTextColumn(), text);
		contentValues.put(getTimestampColumn(), 0);
	}
}
