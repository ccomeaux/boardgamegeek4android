package com.boardgamegeek.service;

import android.support.annotation.NonNull;

import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.service.model.CollectionItem;

import okhttp3.OkHttpClient;

public class CollectionWantPartsUploadTask extends CollectionTextUploadTask {
	public CollectionWantPartsUploadTask(OkHttpClient client) {
		super(client);
	}

	@Override
	@NonNull
	protected String getTextColumn() {
		return Collection.WANTPARTS_LIST;
	}

	@Override
	public String getTimestampColumn() {
		return Collection.WANT_PARTS_DIRTY_TIMESTAMP;
	}

	@Override
	@NonNull
	protected String getFieldName() {
		return "wantpartslist";
	}

	@Override
	protected String getValue(CollectionItem collectionItem) {
		return collectionItem.getWantParts();
	}

	@Override
	public boolean isDirty() {
		return collectionItem.getWantPartsDirtyTimestamp() > 0;
	}
}
