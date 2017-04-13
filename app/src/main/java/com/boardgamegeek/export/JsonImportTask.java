package com.boardgamegeek.export;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.events.ImportFinishedEvent;
import com.boardgamegeek.util.FileUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import timber.log.Timber;

public class JsonImportTask extends ImporterExporterTask {
	public JsonImportTask(Context context) {
		super(context);
	}

	@Override
	protected Integer doInBackground(Void... params) {
		// Ensure external storage
		if (!FileUtils.isExtStorageAvailable()) {
			return ERROR_STORAGE_ACCESS;
		}

		// TODO: Ensure no large database ops are running?

		// Ensure the export directory exists
		File importPath = FileUtils.getExportPath();
		if (!importPath.exists()) {
			return ERROR_STORAGE_ACCESS;
		}

		int stepIndex = 0;
		for (Step step : steps) {
			publishProgress(-1, 0, stepIndex);
			int result = importFile(step);
			if (isCancelled()) return ERROR;
			if (isResultError(result)) return result;
			stepIndex++;
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

	private int importFile(@NonNull Step step) {
		FileInputStream in = null;
		if (shouldUseDefaultFolders()) {
			File file = new File(FileUtils.getExportPath(), step.getFileName());
			if (!file.exists() || !file.canRead()) {
				Timber.i("Unable to read file '%s'", file.toString());
				return ERROR_FILE_ACCESS;
			}

			try {
				in = new FileInputStream(file);
			} catch (FileNotFoundException e) {
				Timber.e(e, "File '%s' not found", file.toString());
			}
		} else {
			Uri backupFileUri = PreferencesUtils.getUri(context, step.getPreferenceKey());
			if (backupFileUri == null) {
				Timber.w("Null backupFileUri for '%s'", step.getDescription(context));
				return ERROR_FILE_ACCESS;
			}

			ParcelFileDescriptor pfd;
			try {
				pfd = context.getContentResolver().openFileDescriptor(backupFileUri, "r");
			} catch (FileNotFoundException | SecurityException e) {
				Timber.e(e, "Backup file '%s' not found.", backupFileUri.toString());
				return ERROR_FILE_ACCESS;
			}
			if (pfd == null) {
				Timber.e("File descriptor for '%s' is null.", backupFileUri.toString());
				return ERROR_FILE_ACCESS;
			}

			in = new FileInputStream(pfd.getFileDescriptor());
		}

		if (in == null) return ERROR;

		step.initializeImport(context);

		try {
			Gson gson = new Gson();
			JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));

			//noinspection TryFinallyCanBeTryWithResources
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
