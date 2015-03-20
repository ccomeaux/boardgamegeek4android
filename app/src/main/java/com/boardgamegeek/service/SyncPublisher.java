package com.boardgamegeek.service;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Company;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Publishers;

import timber.log.Timber;

public class SyncPublisher extends UpdateTask {
	private int mPublisherId;

	public SyncPublisher(int publisherId) {
		mPublisherId = publisherId;
	}

	@Override
	public String getDescription() {
		if (mPublisherId == BggContract.INVALID_ID){
			return "update an unknown publisher";
		}
		return "update publisher " + mPublisherId;
	}

	@Override
	public void execute(Context context) {
		BggService service = Adapter.create();
		Company company = service.company(BggService.COMPANY_TYPE_PUBLISHER, mPublisherId);
		Uri uri = Publishers.buildPublisherUri(mPublisherId);
		context.getContentResolver().update(uri, toValues(company), null, null);
		Timber.i("Synced Publisher " + mPublisherId);
	}

	private static ContentValues toValues(Company company) {
		ContentValues values = new ContentValues();
		values.put(Publishers.PUBLISHER_NAME, company.name);
		values.put(Publishers.PUBLISHER_DESCRIPTION, company.description);
		values.put(Publishers.UPDATED, System.currentTimeMillis());
		return values;
	}
}
