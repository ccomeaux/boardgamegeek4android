package com.boardgamegeek.tasks;


import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Company;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Publishers;
import com.boardgamegeek.tasks.SyncPublisherTask.Event;

import retrofit2.Call;
import timber.log.Timber;

public class SyncPublisherTask extends SyncTask<Company, Event> {
	private final int publisherId;

	public SyncPublisherTask(Context context, int publisherId) {
		super(context);
		this.publisherId = publisherId;
	}

	@Override
	@StringRes
	protected int getTypeDescriptionResId() {
		return R.string.title_publisher;
	}

	@Override
	protected Call<Company> createCall() {
		return bggService.company(BggService.COMPANY_TYPE_PUBLISHER, publisherId);
	}

	@Override
	protected boolean isRequestParamsValid() {
		return super.isRequestParamsValid() && publisherId != BggContract.INVALID_ID;
	}

	@Override
	protected boolean isResponseBodyValid(Company company) {
		return company != null;
	}

	@Override
	protected void persistResponse(Company company) {
		Uri uri = Publishers.buildPublisherUri(publisherId);
		context.getContentResolver().update(uri, toValues(company), null, null);
		Timber.i("Synced publisher: %s", publisherId);
	}

	@NonNull
	@Override
	protected Event createEvent(String errorMessage) {
		return new Event(errorMessage, publisherId);
	}

	@NonNull
	private static ContentValues toValues(@NonNull Company company) {
		ContentValues values = new ContentValues();
		values.put(Publishers.PUBLISHER_NAME, company.name);
		values.put(Publishers.PUBLISHER_DESCRIPTION, company.description);
		values.put(Publishers.UPDATED, System.currentTimeMillis());
		return values;
	}

	public class Event extends SyncTask.Event {
		private final int publisherId;

		public Event(String errorMessage, int publisherId) {
			super(errorMessage);
			this.publisherId = publisherId;
		}

		public int getPublisherId() {
			return publisherId;
		}
	}
}
