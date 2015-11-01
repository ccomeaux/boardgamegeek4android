package com.boardgamegeek.service;

import android.accounts.Account;
import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.PlaysResponse;
import com.boardgamegeek.model.persister.PlayPersister;

import timber.log.Timber;

public class SyncPlaysByDate extends UpdateTask {
	private final String date;

	public SyncPlaysByDate(String date) {
		this.date = date;
	}

	@NonNull
	@Override
	public String getDescription() {
		// TODO use resources for description
		if (isValid()) {
			return "update plays for " + date;
		}
		return "update plays for an unknown date";
	}

	@Override
	public boolean isValid() {
		return !TextUtils.isEmpty(date);
	}

	@Override
	public void execute(@NonNull Context context) {
		Account account = Authenticator.getAccount(context);
		if (account == null) {
			return;
		}

		BggService service = Adapter.create();
		PlayPersister persister = new PlayPersister(context);
		PlaysResponse response;
		try {
			long startTime = System.currentTimeMillis();
			response = service.playsByDate(account.name, date, date);
			persister.save(response.plays, startTime);
			SyncService.hIndex(context);
			Timber.i("Synced plays for date " + date);
		} catch (Exception e) {
			// TODO bubble error up?
			Timber.i("Problem syncing plays by date", e);
		}
	}
}
