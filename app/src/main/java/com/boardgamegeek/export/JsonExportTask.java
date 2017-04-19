package com.boardgamegeek.export;

import android.Manifest.permission;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import com.boardgamegeek.R;
import com.boardgamegeek.events.ExportFinishedEvent;
import com.boardgamegeek.events.ExportProgressEvent;
import com.boardgamegeek.export.model.Model;
import com.boardgamegeek.util.FileUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import timber.log.Timber;

public abstract class JsonExportTask<T extends Model> extends AsyncTask<Void, Integer, String> {
	private static final int PROGRESS_TOTAL = 0;
	private static final int PROGRESS_CURRENT = 1;

	protected final Context context;
	private final String type;
	private final Uri uri;

	public JsonExportTask(Context context, String type, Uri uri) {
		this.context = context.getApplicationContext();
		this.type = type;
		this.uri = uri;
	}

	protected abstract Cursor getCursor(Context context);

	protected abstract void writeJsonRecord(Context context, Cursor cursor, Gson gson, JsonWriter writer);

	@Override
	protected String doInBackground(Void... params) {
		if (uri == null) {
			int permissionCheck = ContextCompat.checkSelfPermission(context, permission.WRITE_EXTERNAL_STORAGE);
			if (permissionCheck == PackageManager.PERMISSION_DENIED) {
				return context.getString(R.string.msg_export_failed_external_permissions);
			}

			if (!FileUtils.isExtStorageAvailable()) {
				return context.getString(R.string.msg_export_failed_external_unavailable);
			}

			File exportPath = FileUtils.getExportPath();
			if (!exportPath.exists()) {
				if (!exportPath.mkdirs()) {
					return context.getString(R.string.msg_export_failed_external_not_created, exportPath);
				}
			}
		}

		if (isCancelled()) return context.getString(R.string.cancelled);

		OutputStream out;
		ParcelFileDescriptor pfd = null;
		if (uri == null) {
			File file = FileUtils.getExportFile(type);
			try {
				out = new FileOutputStream(file);
			} catch (FileNotFoundException e) {
				String error = context.getString(R.string.msg_export_failed_file_not_found, file);
				Timber.w(e, error);
				return error;
			}
		} else {
			try {
				pfd = context.getContentResolver().openFileDescriptor(uri, "w");
			} catch (SecurityException e) {
				String error = context.getString(R.string.msg_export_failed_permissions, uri);
				Timber.w(e, error);
				return error;
			} catch (FileNotFoundException e) {
				String error = context.getString(R.string.msg_export_failed_file_not_found, uri);
				Timber.w(e, error);
				return error;
			}
			if (pfd == null) {
				return context.getString(R.string.msg_export_failed_null_pfd, uri);
			}

			out = new FileOutputStream(pfd.getFileDescriptor());
		}

		final Cursor cursor = getCursor(context);
		if (cursor == null) return context.getString(R.string.msg_export_failed_null_cursor);

		try {
			writeJsonStream(out, cursor);
		} catch (Exception e) {
			String error = context.getString(R.string.msg_export_failed_write_json);
			Timber.e(e, error);
			return error;
		} finally {
			cursor.close();
		}

		FileUtils.closePfd(pfd);

		return null;
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		EventBus.getDefault().post(new ExportProgressEvent(
			values[PROGRESS_TOTAL],
			values[PROGRESS_CURRENT],
			type));
	}

	@Override
	protected void onPostExecute(String errorMessage) {
		Timber.i(errorMessage);
		EventBus.getDefault().post(new ExportFinishedEvent(type, errorMessage));
	}

	private void writeJsonStream(@NonNull OutputStream out, @NonNull Cursor cursor) throws IOException {
		Gson gson = new GsonBuilder()
			.excludeFieldsWithoutExposeAnnotation()
			.create();
		JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
		writer.setIndent("  ");
		writer.beginArray();

		int numTotal = cursor.getCount();
		int numExported = 0;
		while (cursor.moveToNext()) {
			if (isCancelled()) break;
			publishProgress(numTotal, numExported++);
			try {
				writeJsonRecord(context, cursor, gson, writer);
			} catch (RuntimeException e) {
				Timber.e(e);
			}
		}

		writer.endArray();
		writer.close();
	}
}
