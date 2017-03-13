package com.boardgamegeek.tasks;

import android.content.Context;
import android.text.TextUtils;

import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.User;
import com.boardgamegeek.model.persister.BuddyPersister;
import com.boardgamegeek.provider.BggContract;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

public class SyncUserTask extends SyncTask {
	private final String username;

	public SyncUserTask(Context context, String username) {
		super(context);
		this.username = username;
	}

	@Override
	protected String doInBackground() {
		if (TextUtils.isEmpty(username)) return "Tried to sync an unknown user.";

		BggService bggService = Adapter.createForXml();
		try {
			Call<User> call = bggService.user(username);
			Response<User> response = call.execute();
			if (response.isSuccessful()) {
				BuddyPersister persister = new BuddyPersister(context);
				User user = response.body();
				if (user == null || user.getId() == 0 || user.getId() == BggContract.INVALID_ID) {
					return String.format("Invalid user '%s'", username);
				}
				persister.save(user);
				Timber.i("Synced user '%s'", username);
			} else {
				return String.format("Unsuccessful user fetch with HTTP response code: %s", response.code());
			}
		} catch (IOException e) {
			Timber.w(e, "Unsuccessful user fetch");
			return (e.getLocalizedMessage());
		}
		return "";
	}

	@Override
	protected void onPostExecute(String errorMessage) {
		Timber.w(errorMessage);
		EventBus.getDefault().post(new Event(errorMessage, username));
	}

	public class Event {
		private final String errorMessage;
		private final String username;

		public Event(String errorMessage, String username) {
			this.errorMessage = errorMessage;
			this.username = username;
		}

		public String getErrorMessage() {
			return errorMessage;
		}

		public String getUsername() {
			return username;
		}
	}
}
