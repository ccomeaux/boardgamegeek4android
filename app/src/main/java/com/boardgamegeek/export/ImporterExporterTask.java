package com.boardgamegeek.export;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.boardgamegeek.events.ExportProgressEvent;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

public class ImporterExporterTask extends AsyncTask<Void, Integer, Integer> {
	protected static final int ERROR_FILE_ACCESS = 1;
	protected static final int SUCCESS = 0;
	protected static final int ERROR = -1;
	protected static final int ERROR_STORAGE_ACCESS = -2;

	protected final Context mContext;
	protected final boolean mIsAutoBackupMode;
	protected final List<Step> mSteps = new ArrayList<>();

	public ImporterExporterTask(@NonNull Context context, boolean isAutoBackupMode) {
		mContext = context.getApplicationContext();
		mIsAutoBackupMode = isAutoBackupMode;

		mSteps.clear();
		mSteps.add(new CollectionViewStep());
		mSteps.add(new GameStep());
		mSteps.add(new UserStep());
	}

	@NonNull
	public List<Step> getSteps() {
		return mSteps;
	}

	@Override
	protected Integer doInBackground(Void... params) {
		return ERROR;
	}

	// 0 = current progress
	// 1 = total progress
	// 2 = current step
	@Override
	protected void onProgressUpdate(Integer... values) {
		EventBus.getDefault().post(new ExportProgressEvent(values[0], values[1], values[2]));
	}
}
