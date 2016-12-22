package com.boardgamegeek.export;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.boardgamegeek.events.ExportProgressEvent;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

public class ImporterExporterTask extends AsyncTask<Void, Integer, Integer> {
	protected static final int ERROR_FILE_ACCESS = 1;
	protected static final int SUCCESS = 0;
	protected static final int ERROR = -1;
	protected static final int ERROR_STORAGE_ACCESS = -2;
	private static final int PROGRESS_CURRENT = 0;
	private static final int PROGRESS_TOTAL = 1;
	private static final int PROGRESS_STEP = 2;

	protected final Context context;
	protected final List<Step> steps = new ArrayList<>();

	public ImporterExporterTask(@NonNull Context context) {
		this.context = context.getApplicationContext();

		steps.clear();
		steps.add(new CollectionViewStep());
		steps.add(new GameStep());
		steps.add(new UserStep());
	}

	@NonNull
	public List<Step> getSteps() {
		return steps;
	}

	@Override
	protected Integer doInBackground(Void... params) {
		return ERROR;
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		EventBus.getDefault().post(new ExportProgressEvent(
			values[PROGRESS_CURRENT],
			values[PROGRESS_TOTAL],
			values[PROGRESS_STEP]));
	}
}
