package com.boardgamegeek.export;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;

import com.boardgamegeek.events.ExportProgressEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class ImporterExporterTask extends AsyncTask<Void, Integer, String> {
	private static final int PROGRESS_TOTAL = 0;
	private static final int PROGRESS_CURRENT = 1;
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
	protected String doInBackground(Void... params) {
		return null;
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		EventBus.getDefault().post(new ExportProgressEvent(
			values[PROGRESS_TOTAL],
			values[PROGRESS_CURRENT],
			values[PROGRESS_STEP]));
	}

	public static boolean shouldUseDefaultFolders() {
		return VERSION.SDK_INT < VERSION_CODES.KITKAT;
	}

	protected void closePfd(ParcelFileDescriptor pfd) {
		if (pfd != null) {
			try {
				pfd.close();
			} catch (IOException e) {
				Timber.w(e);
			}
		}
	}
}
