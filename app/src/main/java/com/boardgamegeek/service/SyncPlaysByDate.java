package com.boardgamegeek.service;

import android.accounts.Account;
import android.content.Context;

import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.PlaysResponse;
import com.boardgamegeek.model.persister.PlayPersister;

import timber.log.Timber;

public class SyncPlaysByDate extends UpdateTask {
	private String mDate;

	public SyncPlaysByDate(String date) {
		mDate = date;
	}

	@Override
	public String getDescription() {
		return "Sync plays for date=" + mDate;
	}

	@Override
	public void execute(Context context) {
		Account account = Authenticator.getAccount(context);
		if (account == null) {
			return;
		}

		BggService service = Adapter.create();
		PlayPersister persister = new PlayPersister(context);
		PlaysResponse response;
		try {
			long startTime = System.currentTimeMillis();
			response = service.playsByDate(account.name, mDate, mDate);
			persister.save(response.plays, startTime);
			SyncService.hIndex(context);
			Timber.i("Synced plays for date " + mDate);
		} catch (Exception e) {
			// TODO bubble error up?
			Timber.i("Problem syncing plays by date", e);
		}
	}
}
