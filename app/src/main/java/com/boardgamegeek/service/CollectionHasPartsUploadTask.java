package com.boardgamegeek.service;

import android.support.annotation.NonNull;

import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.service.model.CollectionItem;

import okhttp3.OkHttpClient;

public class CollectionHasPartsUploadTask extends CollectionTextUploadTask {
	public CollectionHasPartsUploadTask(OkHttpClient client) {
		super(client);
	}

	@Override
	@NonNull
	protected String getTextColumn() {
		return Collection.HASPARTS_LIST;
	}

	@Override
	public String getTimestampColumn() {
		return Collection.HAS_PARTS_DIRTY_TIMESTAMP;
	}

	@Override
	@NonNull
	protected String getFieldName() {
		return "haspartslist";
	}

	@Override
	protected String getValue(CollectionItem collectionItem) {
		return collectionItem.getHasParts();
	}

	@Override
	public boolean isDirty() {
		return collectionItem.getHasPartsDirtyTimestamp() > 0;
	}
}
