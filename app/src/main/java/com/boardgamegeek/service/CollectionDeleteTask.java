package com.boardgamegeek.service;

import android.content.ContentValues;

import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.service.model.CollectionItem;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;

public class CollectionDeleteTask extends CollectionTask {

	public CollectionDeleteTask(OkHttpClient client) {
		super(client);
	}

	@Override
	protected FormBody createForm(CollectionItem collectionItem) {
		return super.createFormBuilder()
			.add("collid", String.valueOf(collectionItem.getCollectionId()))
			.add("action", "delete")
			.build();
	}

	@Override
	public void appendContentValues(ContentValues contentValues) {
		contentValues.put(Collection.COLLECTION_DIRTY_TIMESTAMP, 0);
	}
}
