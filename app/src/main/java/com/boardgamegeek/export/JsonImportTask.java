package com.boardgamegeek.export;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.events.ImportFinishedEvent;
import com.boardgamegeek.util.FileUtils;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

public class JsonImportTask extends ImporterExporterTask {
	public JsonImportTask(Context context, boolean isAutoBackupMode) {
		super(context, isAutoBackupMode);
	}

	@Override
	protected Integer doInBackground(Void... params) {
		// Ensure external storage
		if (!FileUtils.isExtStorageAvailable()) {
			return ERROR_STORAGE_ACCESS;
		}

		// TODO: Ensure no large database ops are running?

		// Ensure the export directory exists
		File importPath = FileUtils.getExportPath(isAutoBackupMode);
		if (!importPath.exists()) {
			return ERROR_STORAGE_ACCESS;
		}

		int progress = 0;
		for (Step importer : steps) {
			int result = importFile(importPath, importer);
			progress++;
			publishProgress(progress, steps.size(), progress - 1);
			if (result == ERROR || isCancelled()) {
				return ERROR;
			} else if (result < ERROR) {
				return result;
			}
		}

		return SUCCESS;
	}

	@Override
	protected void onPostExecute(@NonNull Integer result) {
		@StringRes int messageId;
		switch (result) {
			case SUCCESS:
				messageId = R.string.msg_import_success;
				break;
			case ERROR_STORAGE_ACCESS:
				messageId = R.string.msg_import_failed_nosd;
				break;
			case ERROR_FILE_ACCESS:
				messageId = R.string.msg_import_failed_nofile;
				break;
			default:
				messageId = R.string.msg_import_failed;
				break;
		}
		EventBus.getDefault().post(new ImportFinishedEvent(messageId));
	}

	private int importFile(File importPath, @NonNull Step step) {
		File file = new File(importPath, step.getFileName());
		if (!file.exists() || !file.canRead()) {
			return ERROR_FILE_ACCESS;
		}

		step.initializeImport(context);

		try {
			InputStream in = new FileInputStream(file);

			Gson gson = new Gson();

			JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));

			try {
				reader.beginArray();
				while (reader.hasNext()) {
					step.importRecord(context, gson, reader);
				}
				reader.endArray();
			} catch (IllegalStateException e) {
				Timber.w(e, "Problem trying to import a file.");
			} finally {
				reader.close();
			}
		} catch (@NonNull JsonParseException | IOException e) {
			// the given Json might not be valid or unreadable
			Timber.e(e, "JSON show import failed");
			return ERROR;
		}

		return SUCCESS;
	}
}
