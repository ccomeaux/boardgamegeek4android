package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.accounts.Account;
import android.content.Context;

import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemotePlaysParser;
import com.boardgamegeek.model.persister.PlayPersister;

public class SyncPlaysByDate extends UpdateTask {
	private static final String TAG = makeLogTag(SyncPlaysByDate.class);
	private String mDate;

	public SyncPlaysByDate(String date) {
		mDate = date;
	}

	@Override
	public void execute(RemoteExecutor executor, Context context) {
		Account account = Authenticator.getAccount(context);
		if (account == null) {
			return;
		}

		RemotePlaysParser parser = new RemotePlaysParser(account.name).setDate(mDate);
		executor.safelyExecuteGet(parser);
		if (!parser.hasError()) {
			PlayPersister.save(context.getContentResolver(), parser.getPlays());
			SyncService.hIndex(context);
		}
		LOGI(TAG, "Synced plays for date " + mDate);
	}
}
