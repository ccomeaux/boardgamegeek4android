package com.boardgamegeek.service;

import android.support.annotation.NonNull;

import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.service.model.CollectionItem;

import okhttp3.OkHttpClient;

public class CollectionWishlistCommentUploadTask extends CollectionTextUploadTask {
	public CollectionWishlistCommentUploadTask(OkHttpClient client) {
		super(client);
	}

	@Override
	@NonNull
	protected String getTextColumn() {
		return Collection.WISHLIST_COMMENT;
	}

	@Override
	public String getTimestampColumn() {
		return Collection.WISHLIST_COMMENT_DIRTY_TIMESTAMP;
	}

	@Override
	@NonNull
	protected String getFieldName() {
		return "wishlistcomment";
	}

	@Override
	protected String getValue(CollectionItem collectionItem) {
		return collectionItem.getWishlistComment();
	}

	@Override
	public boolean isDirty() {
		return collectionItem.getWishlistCommentDirtyTimestamp() > 0;
	}
}
