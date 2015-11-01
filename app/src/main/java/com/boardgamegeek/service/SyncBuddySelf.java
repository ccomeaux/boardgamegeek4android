package com.boardgamegeek.service;

import android.accounts.Account;
import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.AccountUtils;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.User;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.PresentationUtils;

import timber.log.Timber;

public class SyncBuddySelf extends UpdateTask {
	@NonNull
	@Override
	public String getDescription(Context context) {
		return context.getString(R.string.sync_msg_buddy_self);
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public void execute(Context context) {
		Account account = Authenticator.getAccount(context);
		if (account == null) {
			Timber.i("Tried to sync self without an account set.");
			return;
		}

		BggService service = Adapter.create();
		User user = service.user(account.name);

		if (user == null || user.getId() == 0 || user.getId() == BggContract.INVALID_ID) {
			Timber.i("Invalid user: " + account.name);
			return;
		}

		AccountUtils.setUsername(context, user.name);
		AccountUtils.setFullName(context, PresentationUtils.buildFullName(user.firstName, user.lastName));
		AccountUtils.setAvatarUrl(context, user.avatarUrl);

		Timber.i("Synced self: " + account.name);
	}
}
