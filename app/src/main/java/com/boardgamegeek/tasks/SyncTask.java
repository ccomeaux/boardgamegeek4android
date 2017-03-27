package com.boardgamegeek.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.tasks.SyncTask.Event;
import com.boardgamegeek.util.NetworkUtils;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;

import hugo.weaving.DebugLog;
import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

abstract class SyncTask<T, E extends Event> extends AsyncTask<Void, Void, String> {
	protected final Context context;
	protected final BggService bggService;
	protected Call<T> call;

	SyncTask(Context context) {
		this.context = context;
		bggService = Adapter.createForXml();
	}

	@DebugLog
	@Override
	protected String doInBackground(Void... params) {
		if (context == null) return "Null context";
		if (NetworkUtils.isOffline(context)) return "Offline";
		if (!isRequestParamsValid()) return String.format("Tried to sync an unknown %s.", getTypeDescription());
		try {
			call = createCall();
			Response<T> response = call.execute();
			if (response.isSuccessful()) {
				if (isResponseBodyValid(response.body())) {
					persistResponse(response.body());
				} else {
					return String.format("Invalid %s received", getTypeDescription()); // TODO: 3/26/17 include key
				}
			} else {
				return String.format("Unsuccessful %s fetch with HTTP response code: %s", getTypeDescription(), response.code());
			}
		} catch (IOException e) {
			Timber.w(e, "Unsuccessful %s fetch", getTypeDescription());
			return (e.getLocalizedMessage());
		}
		return "";
	}

	@Override
	protected void onPostExecute(String errorMessage) {
		Timber.w(errorMessage);
		EventBus.getDefault().post(createEvent(errorMessage));
	}

	@Override
	protected void onCancelled() {
		if (call != null) call.cancel();
	}

	protected abstract String getTypeDescription();

	protected abstract Call<T> createCall();

	protected abstract boolean isRequestParamsValid();

	protected abstract boolean isResponseBodyValid(T body);

	protected abstract void persistResponse(T body);

	protected abstract E createEvent(String errorMessage);

	protected class Event {
		private final String errorMessage;

		public Event(String errorMessage) {
			this.errorMessage = errorMessage;
		}

		public String getErrorMessage() {
			return errorMessage;
		}
	}
}
