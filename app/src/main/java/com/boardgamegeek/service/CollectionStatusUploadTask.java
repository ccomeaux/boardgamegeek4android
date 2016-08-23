package com.boardgamegeek.service;

import android.content.ContentValues;

import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.service.model.CollectionItem;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import timber.log.Timber;

public class CollectionStatusUploadTask extends CollectionUploadTask {
	//	<div class='owned'>Owned</div>
	//	<div class='wishlist'>Wishlist(3)
	//	<br>&nbsp;(Like&nbsp;to&nbsp;have)
	//	</div>

	public CollectionStatusUploadTask(OkHttpClient client) {
		super(client);
	}

	@Override
	public String getTimestampColumn() {
		return Collection.STATUS_DIRTY_TIMESTAMP;
	}

	@Override
	public boolean isDirty() {
		return collectionItem.getStatusTimestamp() > 0;
	}

	@Override
	protected FormBody createForm(CollectionItem collectionItem) {
		return createFormBuilder()
			.add("fieldname", "status")
			.add("own", collectionItem.owned() ? "1" : "0")
			.add("prevown", collectionItem.previouslyOwned() ? "1" : "0")
			.add("fortrade", collectionItem.forTrade() ? "1" : "0")
			.add("want", collectionItem.wantInTrade() ? "1" : "0")
			.add("wanttobuy", collectionItem.wantToBuy() ? "1" : "0")
			.add("wanttoplay", collectionItem.wantToPlay() ? "1" : "0")
			.add("preordered", collectionItem.preordered() ? "1" : "0")
			.add("wishlist", collectionItem.wishlist() ? "1" : "0")
			.add("wishlistpriority", String.valueOf(collectionItem.wishlistPriority()))
			.build();
	}

	@Override
	protected void saveContent(String content) {
		Timber.d(content);
	}

	@Override
	public void appendContentValues(ContentValues contentValues) {
		contentValues.put(Collection.STATUS_DIRTY_TIMESTAMP, 0);
	}
}
