package com.boardgamegeek.export;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.events.ExportFinishedEvent;
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
	protected String doInBackground(Void... params) {
		if (shouldUseDefaultFolders()) {
			if (!FileUtils.isExtStorageAvailable()) {
				return context.getString(R.string.msg_export_failed_external_unavailable);
			}

			// TODO: Ensure no large database ops are running?

			File importPath = FileUtils.getExportPath();
			if (!importPath.exists()) {
				return context.getString(R.string.msg_import_failed_external_not_exist, importPath);
			}
		}

		int stepIndex = 0;
		for (Step step : steps) {
			if (isCancelled()) return context.getString(R.string.cancelled);
			publishProgress(-1, 0, stepIndex);
			String result = importFile(step);
			if (!TextUtils.isEmpty(result)) return result;
			stepIndex++;
		}

		return null;
	}

	@Override
	protected void onPostExecute(String errorMessage) {
		Timber.i(errorMessage);
		EventBus.getDefault().post(new ExportFinishedEvent(errorMessage));
	}

	private String importFile(@NonNull Step step) {
		FileInputStream in;
		ParcelFileDescriptor pfd = null;
		if (shouldUseDefaultFolders()) {
			File file = new File(FileUtils.getExportPath(), step.getFileName());
			if (!file.exists()) return context.getString(R.string.msg_import_failed_file_not_exist, file);
			if (!file.canRead()) return context.getString(R.string.msg_import_failed_file_not_read, file);

			try {
				in = new FileInputStream(file);
			} catch (FileNotFoundException e) {
				String error = context.getString(R.string.msg_import_failed_file_not_exist, file);
				Timber.w(e, error);
				return error;
			}
		} else {
			Uri uri = PreferencesUtils.getUri(context, step.getPreferenceKey());
			if (uri == null) {
				Timber.i("Null uri for '%s'; skipping", step.getDescription(context));
				return null;
			}

			try {
				pfd = context.getContentResolver().openFileDescriptor(uri, "r");
			} catch (FileNotFoundException e) {
				String error = context.getString(R.string.msg_import_failed_file_not_exist, uri);
				Timber.w(e, error);
				return error;
			}
			if (pfd == null) {
				return context.getString(R.string.msg_export_failed_null_pfd, uri);
			}

			in = new FileInputStream(pfd.getFileDescriptor());
		}

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
		} catch (JsonParseException | IOException e) {
			// the given Json might not be valid or unreadable
			String error = context.getString(R.string.msg_export_failed_step, step.getDescription(context));
			Timber.e(e, error);
			return error;
		}

		closePfd(pfd);

		return null;
	}
}
