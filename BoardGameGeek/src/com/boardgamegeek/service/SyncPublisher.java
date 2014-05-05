package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Company;
import com.boardgamegeek.provider.BggContract.Publishers;

public class SyncPublisher extends UpdateTask {
	private static final String TAG = makeLogTag(SyncPublisher.class);
	private int mPublisherId;

	public SyncPublisher(int publisherId) {
		mPublisherId = publisherId;
	}

	@Override
	public void execute(Context context) {
		BggService service = Adapter.get().create(BggService.class);
		Company company = service.company(BggService.COMPANY_TYPE_PUBLISHER, mPublisherId);
		Uri uri = Publishers.buildPublisherUri(mPublisherId);
		context.getContentResolver().update(uri, toValues(company), null, null);
		LOGI(TAG, "Synced Publisher " + mPublisherId);
	}

	private static ContentValues toValues(Company company) {
		ContentValues values = new ContentValues();
		values.put(Publishers.PUBLISHER_NAME, company.name);
		values.put(Publishers.PUBLISHER_DESCRIPTION, company.description);
		values.put(Publishers.UPDATED, System.currentTimeMillis());
		return values;
	}
}
