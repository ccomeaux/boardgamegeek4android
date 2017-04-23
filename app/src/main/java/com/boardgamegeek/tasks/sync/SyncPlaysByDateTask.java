package com.boardgamegeek.tasks.sync;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.AccountUtils;
import com.boardgamegeek.model.PlaysResponse;
import com.boardgamegeek.model.persister.PlayPersister;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.tasks.CalculatePlayStatsTask;
import com.boardgamegeek.tasks.sync.SyncPlaysByDateTask.CompletedEvent;
import com.boardgamegeek.util.TaskUtils;

import retrofit2.Call;
import timber.log.Timber;

public class SyncPlaysByDateTask extends SyncTask<PlaysResponse, CompletedEvent> {
	private final String date;
	private final String username;

	public SyncPlaysByDateTask(Context context, String date) {
		super(context);
		this.date = date;
		username = AccountUtils.getUsername(context);
	}

	@Override
	@StringRes
	protected int getTypeDescriptionResId() {
		return R.string.title_plays;
	}

	@Override
	protected Call<PlaysResponse> createCall() {
		return bggService.playsByDate(username, date, date, getCurrentPage());
	}

	@Override
	protected boolean isRequestParamsValid() {
		return super.isRequestParamsValid() &&
			!TextUtils.isEmpty(date) &&
			!TextUtils.isEmpty(username);
	}

	@Override
	protected void persistResponse(PlaysResponse body) {
		PlayPersister persister = new PlayPersister(context);
		persister.save(body.plays, startTime);
		Timber.i("Synced plays for %s (page %,d)", date, getCurrentPage());
	}

	@Override
	protected boolean hasMorePages(PlaysResponse body) {
		return body.hasMorePages();
	}

	@Override
	protected void finishSync() {
		if (SyncService.isPlaysSyncUpToDate(context)) {
			TaskUtils.executeAsyncTask(new CalculatePlayStatsTask(context));
		}
	}

	@NonNull
	@Override
	protected CompletedEvent createEvent(String errorMessage) {
		return new CompletedEvent(errorMessage);
	}

	public class CompletedEvent extends SyncTask.CompletedEvent {
		public CompletedEvent(String errorMessage) {
			super(errorMessage);
		}
	}
}
