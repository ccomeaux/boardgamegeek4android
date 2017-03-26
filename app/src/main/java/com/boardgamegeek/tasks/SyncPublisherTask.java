package com.boardgamegeek.tasks;


import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Company;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Publishers;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

public class SyncPublisherTask extends SyncTask {
	private final Call<Company> call;
	private final int publisherId;

	public SyncPublisherTask(Context context, int publisherId) {
		super(context);
		this.publisherId = publisherId;
		BggService bggService = Adapter.createForXml();
		call = bggService.company(BggService.COMPANY_TYPE_PUBLISHER, publisherId);
	}

	@Override
	protected String doInBackground() {
		if (publisherId == BggContract.INVALID_ID) return "Tried to sync an unknown publisher.";

		try {
			Response<Company> response = call.execute();
			if (response.isSuccessful()) {
				Company company = response.body();
				if (company == null) {
					return String.format("Invalid publisher '%s'", publisherId);
				}
				Uri uri = Publishers.buildPublisherUri(publisherId);
				context.getContentResolver().update(uri, toValues(company), null, null);
				Timber.i("Synced Publisher: %s", publisherId);
			} else {
				return String.format("Unsuccessful publisher fetch with HTTP response code: %s", response.code());
			}
		} catch (IOException e) {
			Timber.w(e, "Unsuccessful publisher fetch");
			return (e.getLocalizedMessage());
		}
		return "";
	}

	@Override
	protected void onCancelled() {
		if (call != null) call.cancel();
	}

	@Override
	protected void onPostExecute(String errorMessage) {
		Timber.w(errorMessage);
		EventBus.getDefault().post(new SyncPublisherTask.Event(errorMessage, publisherId));
	}

	@NonNull
	private static ContentValues toValues(@NonNull Company company) {
		ContentValues values = new ContentValues();
		values.put(Publishers.PUBLISHER_NAME, company.name);
		values.put(Publishers.PUBLISHER_DESCRIPTION, company.description);
		values.put(Publishers.UPDATED, System.currentTimeMillis());
		return values;
	}

	public class Event {
		private final String errorMessage;
		private final int publisherId;

		public Event(String errorMessage, int publisherId) {
			this.errorMessage = errorMessage;
			this.publisherId = publisherId;
		}

		public String getErrorMessage() {
			return errorMessage;
		}

		public int getPublisherId() {
			return publisherId;
		}
	}
}
