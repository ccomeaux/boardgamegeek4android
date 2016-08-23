package com.boardgamegeek.service;

import android.support.annotation.NonNull;

import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.service.model.CollectionItem;

import okhttp3.OkHttpClient;

public class CollectionTradeConditionUploadTask extends CollectionTextUploadTask {
	public CollectionTradeConditionUploadTask(OkHttpClient client) {
		super(client);
	}

	@Override
	@NonNull
	protected String getTextColumn() {
		return Collection.CONDITION;
	}

	@Override
	public String getTimestampColumn() {
		return Collection.TRADE_CONDITION_DIRTY_TIMESTAMP;
	}

	@Override
	@NonNull
	protected String getFieldName() {
		return "conditiontext";
	}

	@Override
	protected String getValue(CollectionItem collectionItem) {
		return collectionItem.getTradeCondition();
	}

	@Override
	public boolean isDirty() {
		return collectionItem.getTradeConditionDirtyTimestamp() > 0;
	}
}
