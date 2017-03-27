package com.boardgamegeek.tasks;

import android.accounts.Account;
import android.content.Context;
import android.support.annotation.StringRes;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.AccountUtils;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.model.User;
import com.boardgamegeek.model.persister.BuddyPersister;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.tasks.SyncUserTask.Event;
import com.boardgamegeek.util.PresentationUtils;

import retrofit2.Call;
import timber.log.Timber;

public class SyncUserTask extends SyncTask<User, Event> {
	private final String username;

	public SyncUserTask(Context context, String username) {
		super(context);
		this.username = username;
	}

	@Override
	@StringRes
	protected int getTypeDescriptionResId() {
		return R.string.title_user;
	}

	@Override
	protected Call<User> createCall() {
		return bggService.user(username);
	}

	@Override
	protected boolean isRequestParamsValid() {
		return super.isRequestParamsValid() && !TextUtils.isEmpty(username);
	}

	@Override
	protected boolean isResponseBodyValid(User user) {
		return user != null && user.getId() != 0 && user.getId() != BggContract.INVALID_ID;
	}

	@Override
	protected void persistResponse(User user) {
		BuddyPersister persister = new BuddyPersister(context);
		persister.saveUser(user);

		Account account = Authenticator.getAccount(context);
		if (account != null && username.equals(account.name)) {
			AccountUtils.setUsername(context, user.name);
			AccountUtils.setFullName(context, PresentationUtils.buildFullName(user.firstName, user.lastName));
			AccountUtils.setAvatarUrl(context, user.avatarUrl);
		}

		Timber.i("Synced user '%s'", username);
	}

	@Override
	protected Event createEvent(String errorMessage) {
		return new Event(errorMessage, username);
	}

	public class Event extends SyncTask.Event {
		private final String username;

		public Event(String errorMessage, String username) {
			super(errorMessage);
			this.username = username;
		}

		public String getUsername() {
			return username;
		}
	}
}
