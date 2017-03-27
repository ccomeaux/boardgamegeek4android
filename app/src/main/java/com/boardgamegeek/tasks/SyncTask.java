package com.boardgamegeek.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
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

public abstract class SyncTask<T, E extends Event> extends AsyncTask<Void, Void, String> {
	protected final Context context;
	protected final BggService bggService;
	protected final long startTime;
	private Call<T> call;
	private int page = 1;

	SyncTask(Context context) {
		this.context = context;
		bggService = Adapter.createForXml();
		startTime = System.currentTimeMillis();
	}

	@DebugLog
	@Override
	protected String doInBackground(Void... params) {
		if (!isRequestParamsValid())
			return context.getString(R.string.msg_update_invalid_request, context.getString(getTypeDescriptionResId()));
		if (NetworkUtils.isOffline(context)) return context.getString(R.string.msg_offline);
		try {
			boolean hasMorePages;
			page = 0;
			do {
				page++;
				call = createCall();
				Response<T> response = call.execute();
				if (response.isSuccessful()) {
					if (isResponseBodyValid(response.body())) {
						persistResponse(response.body());
					} else {
						return context.getString(R.string.msg_update_invalid_response,
							context.getString(getTypeDescriptionResId())); // TODO: 3/26/17 include key
					}
				} else {
					return context.getString(R.string.msg_update_unsuccessful_response,
						context.getString(getTypeDescriptionResId()),
						response.code());
				}
				hasMorePages = hasMorePages(response.body());
			} while (hasMorePages);
			finishSync();
		} catch (IOException e) {
			Timber.w(e,
				context.getString(R.string.msg_update_exception),
				context.getString(getTypeDescriptionResId()),
				e.getMessage());
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

	protected int getCurrentPage(){
		return page;
	}

	@StringRes
	protected abstract int getTypeDescriptionResId();

	protected abstract Call<T> createCall();

	protected boolean isRequestParamsValid() {
		return context != null;
	}

	protected boolean isResponseBodyValid(T body) {
		return body != null;
	}

	protected abstract void persistResponse(T body);

	protected boolean hasMorePages(T body) {
		return false;
	}

	protected void finishSync() {
	}

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
