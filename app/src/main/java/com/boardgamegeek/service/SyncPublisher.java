package com.boardgamegeek.service;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Company;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Publishers;

import timber.log.Timber;

public class SyncPublisher extends UpdateTask {
	private final int publisherId;

	public SyncPublisher(int publisherId) {
		this.publisherId = publisherId;
	}

	@NonNull
	@Override
	public String getDescription(Context context) {
		if (isValid()) {
			return context.getString(R.string.sync_msg_publisher_valid, publisherId);
		}
		return context.getString(R.string.sync_msg_publisher_invalid);
	}

	@Override
	public boolean isValid() {
		return publisherId != BggContract.INVALID_ID;
	}

	@Override
	public void execute(@NonNull Context context) {
		BggService service = Adapter.create();
		Company company = service.company(BggService.COMPANY_TYPE_PUBLISHER, publisherId);
		Uri uri = Publishers.buildPublisherUri(publisherId);
		context.getContentResolver().update(uri, toValues(company), null, null);
		Timber.i("Synced Publisher " + publisherId);
	}

	@NonNull
	private static ContentValues toValues(@NonNull Company company) {
		ContentValues values = new ContentValues();
		values.put(Publishers.PUBLISHER_NAME, company.name);
		values.put(Publishers.PUBLISHER_DESCRIPTION, company.description);
		values.put(Publishers.UPDATED, System.currentTimeMillis());
		return values;
	}
}
