package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.HashMap;
import java.util.Map;

import retrofit.RetrofitError;
import android.accounts.Account;
import android.content.Context;

import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.model.PlaysResponse;
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

		BggService service = Adapter.create();

		Map<String, String> options = new HashMap<String, String>();
		options.put("username", account.name);
		options.put("mindate", mDate);
		options.put("maxdate", mDate);
		PlaysResponse response = null;
		try {
			response = service.plays(options);
			PlayPersister.save(context.getContentResolver(), response.plays);
			SyncService.hIndex(context);
			LOGI(TAG, "Synced plays for date " + mDate);
		} catch (Exception e) {
			// TODO bubble error up?
			if (e instanceof RetrofitError) {
				RetrofitError re = (RetrofitError) e;
				LOGW(TAG, re.getUrl());
			}
			LOGW(TAG, "Problem syncing plays by date", e);
			if (response != null) {
				// LOGI(TAG, response.)
			}
		}
	}
}
