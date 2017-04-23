package com.boardgamegeek.export;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;

import com.boardgamegeek.R;
import com.boardgamegeek.events.ImportFinishedEvent;
import com.boardgamegeek.events.ImportProgressEvent;
import com.boardgamegeek.export.model.Model;
import com.boardgamegeek.util.FileUtils;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public abstract class JsonImportTask<T extends Model> extends AsyncTask<Void, Integer, String> {
	private static final int PROGRESS_TOTAL = 0;
	private static final int PROGRESS_CURRENT = 1;

	protected final Context context;
	private final String type;
	private final Uri uri;
	private final List<T> items;

	public JsonImportTask(Context context, String type, Uri uri) {
		this.context = context.getApplicationContext();
		this.type = type;
		this.uri = uri;
		items = new ArrayList<>();
	}

	protected void initializeImport() {
	}

	protected abstract T parseItem(Gson gson, JsonReader reader);

	protected abstract void importRecord(T item, int version);

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

			File file = FileUtils.getExportFile(type);
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

		initializeImport();
		int version = 0;

		JsonReader reader = null;
		try {
			reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
			if (reader.peek() == JsonToken.BEGIN_ARRAY) {
				parseItems(reader);
			} else {
				reader.beginObject();
				while (reader.hasNext()) {
					String name = reader.nextName();
					if (Constants.NAME_TYPE.equals(name)) {
						String type = reader.nextString();
						if (!type.equals(this.type)) {
							return context.getString(R.string.msg_import_failed_wrong_type, this.type, type);
						}
					} else if (Constants.NAME_VERSION.equals(name)) {
						version = reader.nextInt();
					} else if (Constants.NAME_ITEMS.equals(name)) {
						parseItems(reader);
					} else {
						reader.skipValue();
					}
				}
				reader.endObject();
			}
		} catch (Exception e) {
			Timber.w(e, "Importing %s JSON file.", type);
			return context.getString(R.string.msg_import_failed_parse_json);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					Timber.w(e, "Failed trying to close the JsonReader");
				}
			}
		}

		for (int i = 0; i < items.size(); i++) {
			publishProgress(items.size(), i);
			T item = items.get(i);
			importRecord(item, version);
		}

		FileUtils.closePfd(pfd);

		return null;
	}

	private void parseItems(JsonReader reader) throws IOException {
		Gson gson = new Gson();
		items.clear();
		reader.beginArray();
		while (reader.hasNext()) {
			items.add(parseItem(gson, reader));
		}
		reader.endArray();
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		EventBus.getDefault().post(new ImportProgressEvent(
			values[PROGRESS_TOTAL],
			values[PROGRESS_CURRENT],
			type));
	}

	@Override
	protected void onPostExecute(String errorMessage) {
		Timber.i(errorMessage);
		EventBus.getDefault().post(new ImportFinishedEvent(type, errorMessage));
	}
}
