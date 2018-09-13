package com.boardgamegeek.tasks.sync;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.text.TextUtils;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.auth.AccountUtils;
import com.boardgamegeek.io.model.PlaysResponse;
import com.boardgamegeek.mappers.PlayMapper;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.persister.PlayPersister;
import com.boardgamegeek.tasks.CalculatePlayStatsTask;
import com.boardgamegeek.tasks.sync.SyncPlaysByDateTask.CompletedEvent;
import com.boardgamegeek.util.TaskUtils;

import java.util.List;

import retrofit2.Call;
import timber.log.Timber;

public class SyncPlaysByDateTask extends SyncTask<PlaysResponse, CompletedEvent> {
	private final BggApplication application;
	private final String date;
	private final String username;

	public SyncPlaysByDateTask(BggApplication application, String date) {
		super(application.getApplicationContext());
		this.application = application;
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
		PlayMapper mapper = new PlayMapper();
		List<Play> plays = mapper.map(body.plays);
		persister.save(plays, startTime);
		Timber.i("Synced plays for %s (page %,d)", date, getCurrentPage());
	}

	@Override
	protected boolean hasMorePages(PlaysResponse body) {
		return body.hasMorePages();
	}

	@Override
	protected void finishSync() {
		TaskUtils.executeAsyncTask(new CalculatePlayStatsTask(application));
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
