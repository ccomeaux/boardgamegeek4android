package com.boardgamegeek.export;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.events.ExportFinishedEvent;
import com.boardgamegeek.events.ExportProgressEvent;
import com.boardgamegeek.export.model.CollectionView;
import com.boardgamegeek.export.model.Filter;
import com.boardgamegeek.provider.BggContract.CollectionViewFilters;
import com.boardgamegeek.provider.BggContract.CollectionViews;
import com.boardgamegeek.util.FileUtils;
import com.boardgamegeek.util.ResolverUtils;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

public class JsonImportTask extends AsyncTask<Void, Integer, Integer> {
	private static final int SUCCESS = 1;
	private static final int ERROR = 0;
	private static final int ERROR_STORAGE_ACCESS = -1;
	private static final int ERROR_FILE_ACCESS = -2;
	private Context mContext;
	private boolean mIsAutoBackupMode;

	public JsonImportTask(Context context, boolean isAutoBackupMode) {
		mContext = context.getApplicationContext();
		mIsAutoBackupMode = isAutoBackupMode;
	}

	@Override
	protected Integer doInBackground(Void... params) {
		// Ensure external storage
		if (!FileUtils.isExtStorageAvailable()) {
			return ERROR_STORAGE_ACCESS;
		}

		// Ensure no large database ops are running?

		// Ensure JSON file is available
		File importPath = JsonExportTask.getExportPath(mIsAutoBackupMode);

		int result = importCollectionViews(importPath);
		if (result == ERROR || isCancelled()) {
			return ERROR;
		}

		return SUCCESS;
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		EventBus.getDefault().post(new ExportProgressEvent(values[0], values[1]));
	}

	@Override
	protected void onPostExecute(Integer result) {
		int messageId;
		switch (result) {
			case SUCCESS:
				messageId = R.string.pref_advanced_import_msg_success;
				break;
			case ERROR_STORAGE_ACCESS:
				messageId = R.string.pref_advanced_import_msg_failed_nosd;
				break;
			case ERROR_FILE_ACCESS:
				messageId = R.string.pref_advanced_import_msg_failed_nofile;
				break;
			default:
				messageId = R.string.pref_advanced_import_msg_failed;
				break;
		}
		Toast.makeText(mContext, messageId, Toast.LENGTH_LONG).show();

		EventBus.getDefault().post(new ExportFinishedEvent());
	}

	private int importCollectionViews(File importPath) {
		File file = new File(importPath, JsonExportTask.EXPORT_JSON_FILE_COLLECTION_VIEWS);
		if (!file.exists() || !file.canRead()) {
			return ERROR_FILE_ACCESS;
		}

		mContext.getContentResolver().delete(CollectionViews.CONTENT_URI, null, null);

		// Access JSON from backup folder to create new database
		try {
			InputStream in = new FileInputStream(file);

			Gson gson = new Gson();

			JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
			reader.beginArray();

			while (reader.hasNext()) {
				CollectionView cv = gson.fromJson(reader, CollectionView.class);
				add(cv);
			}

			reader.endArray();
			reader.close();
		} catch (JsonParseException | IOException e) {
			// the given Json might not be valid or unreadable
			Timber.e(e, "JSON show import failed");
			return ERROR;
		}

		return SUCCESS;
	}

	private void add(CollectionView cv) {

		ContentResolver resolver = mContext.getContentResolver();

		ContentValues values = new ContentValues();
		values.put(CollectionViews.NAME, cv.getName());
		values.put(CollectionViews.STARRED, cv.isStarred());
		values.put(CollectionViews.SORT_TYPE, cv.getSortType());
		Uri uri = resolver.insert(CollectionViews.CONTENT_URI, values);

		if (cv.getFilters() == null || cv.getFilters().size() == 0) {
			return;
		}

		int viewId = CollectionViews.getViewId(uri);
		Uri filterUri = CollectionViews.buildViewFilterUri(viewId);

		ArrayList<ContentProviderOperation> batch = new ArrayList<>();
		for (Filter filter : cv.getFilters()) {
			ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(filterUri);
			builder.withValue(CollectionViewFilters.TYPE, filter.getType());
			builder.withValue(CollectionViewFilters.DATA, filter.getData());
			batch.add(builder.build());
		}
		ResolverUtils.applyBatch(mContext, batch);
	}
}
