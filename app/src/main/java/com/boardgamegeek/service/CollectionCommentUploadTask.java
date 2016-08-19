package com.boardgamegeek.service;

import android.support.annotation.NonNull;

import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.service.model.CollectionItem;

import okhttp3.OkHttpClient;

public class CollectionCommentUploadTask extends CollectionTextUploadTask {
	public CollectionCommentUploadTask(OkHttpClient client) {
		super(client);
	}

	@NonNull
	protected String getTextColumn() {
		return Collection.COMMENT;
	}

	@Override
	public String getTimestampColumn() {
		return Collection.COMMENT_DIRTY_TIMESTAMP;
	}

	@NonNull
	protected String getFieldName() {
		return "comment";
	}

	protected String getValue(CollectionItem collectionItem) {
		return collectionItem.getComment();
	}

	@Override
	public boolean isDirty() {
		return collectionItem.getCommentTimestamp() > 0;
	}
}
