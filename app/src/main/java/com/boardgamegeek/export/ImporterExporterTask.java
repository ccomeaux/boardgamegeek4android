package com.boardgamegeek.export;

import android.content.Context;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;

public class ImporterExporterTask extends AsyncTask<Void, Integer, Integer> {
	protected static final int ERROR_FILE_ACCESS = 1;
	protected static final int SUCCESS = 0;
	protected static final int ERROR = -1;
	protected static final int ERROR_STORAGE_ACCESS = -2;

	protected final Context mContext;
	protected final boolean mIsAutoBackupMode;
	protected final List<ImporterExporter> mExporters = new ArrayList<>();

	public ImporterExporterTask(Context context, boolean isAutoBackupMode) {
		mContext = context.getApplicationContext();
		mIsAutoBackupMode = isAutoBackupMode;

		mExporters.clear();
		mExporters.add(new CollectionViewImporterExporter());
		mExporters.add(new GameImporterExporter());
		mExporters.add(new UserImporterExporter());
	}

	public List<ImporterExporter> getTypes() {
		return mExporters;
	}

	@Override
	protected Integer doInBackground(Void... params) {
		return ERROR;
	}
}
