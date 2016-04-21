package com.boardgamegeek.service;

import android.content.ContentValues;

import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.service.model.CollectionItem;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;

public class CollectionCommentUploadTask extends CollectionUploadTask {
	private String comment;

	public CollectionCommentUploadTask(OkHttpClient client) {
		super(client);
	}

	@Override
	public String getTimestampColumn() {
		return Collection.COMMENT_DIRTY_TIMESTAMP;
	}

	@Override
	public boolean isDirty() {
		return collectionItem.getCommentTimestamp() > 0;
	}

	@Override
	protected FormBody createForm(CollectionItem collectionItem) {
		return createFormBuilder()
			.add("fieldname", "comment")
			.add("value", collectionItem.getComment())
			.build();
	}

	@Override
	protected void saveContent(String content) {
		comment = content;
	}

	@Override
	public void appendContentValues(ContentValues contentValues) {
		contentValues.put(Collection.COMMENT, comment);
		contentValues.put(Collection.COMMENT_DIRTY_TIMESTAMP, 0);
	}
}
