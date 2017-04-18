package com.boardgamegeek.export;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;

import com.boardgamegeek.R;
import com.boardgamegeek.events.ImportFinishedEvent;
import com.boardgamegeek.events.ImportProgressEvent;
import com.boardgamegeek.util.FileUtils;
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

public class JsonImportTask extends AsyncTask<Void, Integer, String> {
	protected final Context context;
	private final int requestCode;
	private final Step step;
	private final Uri uri;

	public JsonImportTask(Context context, int requestCode, Step step, Uri uri) {
		this.context = context.getApplicationContext();
		this.requestCode = requestCode;
		this.step = step;
		this.uri = uri;
	}

	@Override
	protected String doInBackground(Void... params) {
		FileInputStream in;
		ParcelFileDescriptor pfd = null;
		if (uri == null) {
			if (!FileUtils.isExtStorageAvailable()) {
				return context.getString(R.string.msg_export_failed_external_unavailable);
			}

			// TODO: Ensure no large database ops are running?

			File importPath = FileUtils.getExportPath();
			if (!importPath.exists()) {
				return context.getString(R.string.msg_import_failed_external_not_exist, importPath);
			}

			File file = FileUtils.getExportFile(step.getName());
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

		if (isCancelled()) return context.getString(R.string.cancelled);
		publishProgress(-1, 0, requestCode);

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

		FileUtils.closePfd(pfd);

		return null;
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		EventBus.getDefault().post(new ImportProgressEvent(values[0]));
	}

	@Override
	protected void onPostExecute(String errorMessage) {
		Timber.i(errorMessage);
		EventBus.getDefault().post(new ImportFinishedEvent(requestCode, errorMessage));
	}
}
