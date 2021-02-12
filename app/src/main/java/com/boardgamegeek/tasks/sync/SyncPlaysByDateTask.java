package com.boardgamegeek.tasks.sync;

import android.text.TextUtils;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.auth.AccountUtils;
import com.boardgamegeek.extensions.TaskUtils;
import com.boardgamegeek.io.model.PlaysResponse;
import com.boardgamegeek.mappers.PlayMapper;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.persister.PlayPersister;
import com.boardgamegeek.tasks.CalculatePlayStatsTask;
import com.boardgamegeek.util.DateTimeUtils;

import java.util.List;

import androidx.annotation.StringRes;
import retrofit2.Call;
import timber.log.Timber;

public class SyncPlaysByDateTask extends SyncTask<PlaysResponse> {
	private final BggApplication application;
	private final String date;
	private final String username;

	public SyncPlaysByDateTask(BggApplication application, Integer year, Integer month, Integer day) {
		super(application.getApplicationContext());
		this.application = application;
		this.date = DateTimeUtils.formatDateForApi(year, month, day);
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
		return !TextUtils.isEmpty(date) && !TextUtils.isEmpty(username);
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
}
