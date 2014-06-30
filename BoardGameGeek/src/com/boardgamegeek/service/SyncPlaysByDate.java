package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.accounts.Account;
import android.content.Context;

import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.PlaysResponse;
import com.boardgamegeek.model.persister.PlayPersister;

public class SyncPlaysByDate extends UpdateTask {
	private static final String TAG = makeLogTag(SyncPlaysByDate.class);
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

		PlaysResponse response = null;
		try {
			long startTime = System.currentTimeMillis();
			response = service.playsByDate(account.name, mDate, mDate);
			PlayPersister.save(context, response.plays, startTime);
			SyncService.hIndex(context);
			LOGI(TAG, "Synced plays for date " + mDate);
		} catch (Exception e) {
			// TODO bubble error up?
			LOGW(TAG, "Problem syncing plays by date", e);
		}
	}
}
