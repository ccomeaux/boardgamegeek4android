package com.boardgamegeek.export;

import android.Manifest.permission;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import com.boardgamegeek.R;
import com.boardgamegeek.events.ExportFinishedEvent;
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

public class JsonExportTask extends ImporterExporterTask {
	private final Step step;
	private final Uri uri;

	public JsonExportTask(Context context, Step step, Uri uri) {
		super(context);
		this.step = step;
		this.uri = uri;
	}

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
		if (shouldUseDefaultFolders()) {
			File file = new File(FileUtils.getExportPath(), step.getFileName());
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

		final Cursor cursor = step.getCursor(context);
		if (cursor == null) return context.getString(R.string.msg_export_failed_null_cursor);

		try {
			writeJsonStream(out, cursor, step);
		} catch (Exception e) {
			String error = context.getString(R.string.msg_export_failed_step, step.getDescription(context));
			Timber.e(e, error);
			return error;
		} finally {
			cursor.close();
		}

		closePfd(pfd);

		return null;
	}

	@Override
	protected void onPostExecute(String errorMessage) {
		Timber.i(errorMessage);
		EventBus.getDefault().post(new ExportFinishedEvent(errorMessage));
	}

	private void writeJsonStream(@NonNull OutputStream out, @NonNull Cursor cursor, @NonNull Step step) throws IOException {
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
				step.writeJsonRecord(context, cursor, gson, writer);
			} catch (RuntimeException e) {
				Timber.e(e);
			}
		}

		writer.endArray();
		writer.close();
	}
}
