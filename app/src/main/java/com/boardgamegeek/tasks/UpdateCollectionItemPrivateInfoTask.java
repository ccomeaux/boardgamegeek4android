package com.boardgamegeek.tasks;


import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.events.CollectionItemUpdatedEvent;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.ui.model.PrivateInfo;

import org.greenrobot.eventbus.EventBus;

import hugo.weaving.DebugLog;
import timber.log.Timber;

public class UpdateCollectionItemPrivateInfoTask extends UpdateCollectionItemTask {
	private final PrivateInfo privateInfo;

	@DebugLog
	public UpdateCollectionItemPrivateInfoTask(Context context, int gameId, int collectionId, PrivateInfo privateInfo) {
		super(context, gameId, collectionId);
		this.privateInfo = privateInfo;
	}

	@DebugLog
	@Override
	protected Void doInBackground(Void... params) {
		final ContentResolver resolver = context.getContentResolver();
		long internalId = getCollectionItemInternalId(resolver, collectionId, gameId);
		if (internalId != BggContract.INVALID_ID) {
			updateResolver(resolver, internalId);
		}
		return null;
	}

	@DebugLog
	private void updateResolver(@NonNull ContentResolver resolver, long internalId) {
		ContentValues values = new ContentValues(9);
		values.put(Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY, privateInfo.getPriceCurrency());
		values.put(Collection.PRIVATE_INFO_PRICE_PAID, privateInfo.getPrice());
		values.put(Collection.PRIVATE_INFO_CURRENT_VALUE_CURRENCY, privateInfo.getCurrentValueCurrency());
		values.put(Collection.PRIVATE_INFO_CURRENT_VALUE, privateInfo.getCurrentValue());
		values.put(Collection.PRIVATE_INFO_QUANTITY, privateInfo.getQuantity());
		values.put(Collection.PRIVATE_INFO_ACQUISITION_DATE, privateInfo.getAcquisitionDate());
		values.put(Collection.PRIVATE_INFO_ACQUIRED_FROM, privateInfo.getAcquiredFrom());
		values.put(Collection.PRIVATE_INFO_COMMENT, privateInfo.getPrivateComment());
		values.put(Collection.PRIVATE_INFO_DIRTY_TIMESTAMP, System.currentTimeMillis());
		resolver.update(Collection.buildUri(internalId), values, null, null);
	}

	@DebugLog
	@Override
	protected void onPostExecute(Void result) {
		Timber.i("Updated game ID %1$s, collection ID %2$s with private info.", gameId, collectionId);
		EventBus.getDefault().post(new CollectionItemUpdatedEvent());
	}
}
