package com.boardgamegeek.service;

import android.content.ContentValues;

import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.service.model.CollectionItem;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;

public class CollectionAddTask extends CollectionTask {

	public CollectionAddTask(OkHttpClient client) {
		super(client);
	}

	@Override
	protected FormBody createForm(CollectionItem collectionItem) {
		return super.createFormBuilder()
			.add("action", "additem")
			.build();
	}

	@Override
	public void appendContentValues(ContentValues contentValues) {
		contentValues.put(Collection.COLLECTION_DIRTY_TIMESTAMP, 0);
	}
}
