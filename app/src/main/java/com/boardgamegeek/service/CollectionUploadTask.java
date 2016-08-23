package com.boardgamegeek.service;

import android.support.annotation.NonNull;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;

public abstract class CollectionUploadTask extends CollectionTask {

	public CollectionUploadTask(OkHttpClient client) {
		super(client);
	}

	public abstract String getTimestampColumn();

	public abstract boolean isDirty();

	@NonNull
	protected FormBody.Builder createFormBuilder() {
		return super.createFormBuilder()
			.add("action", "savedata")
			.add("collid", String.valueOf(collectionItem.getCollectionId()));
	}
}
