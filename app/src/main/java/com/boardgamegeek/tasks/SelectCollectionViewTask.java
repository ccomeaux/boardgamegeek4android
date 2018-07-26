package com.boardgamegeek.tasks;


import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.database.Cursor;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.CollectionViews;
import com.boardgamegeek.ui.CollectionActivity;
import com.boardgamegeek.util.ShortcutUtils;
import com.boardgamegeek.util.StringUtils;

import java.util.ArrayList;
import java.util.List;


public class SelectCollectionViewTask extends AsyncTask<Void, Void, Void> {
	private static final int SHORTCUT_COUNT = 3;
	@SuppressLint("StaticFieldLeak") @Nullable private final Context context;
	private final long viewId;
	@Nullable private final ShortcutManager shortcutManager;

	public SelectCollectionViewTask(@Nullable Context context, long viewId) {
		this.context = context == null ? null : context.getApplicationContext();
		this.viewId = viewId;
		if (context != null && VERSION.SDK_INT >= VERSION_CODES.N_MR1) {
			shortcutManager = context.getSystemService(ShortcutManager.class);
		} else {
			shortcutManager = null;
		}
	}

	@Nullable
	@Override
	protected Void doInBackground(Void... params) {
		if (context == null) return null;
		updateSelection();
		if (shortcutManager != null && VERSION.SDK_INT >= VERSION_CODES.N_MR1) {
			shortcutManager.reportShortcutUsed(createShortcutName(viewId));
			setShortcuts();
		}
		return null;
	}

	private void updateSelection() {
		if (context == null) return;
		Cursor cursor = null;
		try {
			Uri uri = CollectionViews.buildViewUri(viewId);
			cursor = context.getContentResolver().query(uri,
				new String[] { CollectionViews.SELECTED_COUNT },
				null, null, null);
			if (cursor != null && cursor.moveToFirst()) {
				int currentCount = cursor.getInt(0);
				ContentValues cv = new ContentValues(2);
				cv.put(CollectionViews.SELECTED_COUNT, currentCount + 1);
				cv.put(CollectionViews.SELECTED_TIMESTAMP, System.currentTimeMillis());
				context.getContentResolver().update(uri, cv, null, null);
			}
		} finally {
			if (cursor != null) cursor.close();
		}
	}

	@RequiresApi(VERSION_CODES.N_MR1)
	private void setShortcuts() {
		if (context == null || shortcutManager == null) return;
		List<ShortcutInfo> shortcuts = new ArrayList<>(SHORTCUT_COUNT);
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(CollectionViews.CONTENT_URI,
				new String[] { CollectionViews._ID, CollectionViews.NAME },
				null, null,
				CollectionViews.SELECTED_COUNT + " DESC, " + CollectionViews.SELECTED_TIMESTAMP + " DESC");
			while (cursor != null && cursor.moveToNext()) {
				String name = cursor.getString(1);
				if (!TextUtils.isEmpty(name)) {
					shortcuts.add(createShortcutInfo(cursor.getLong(0), name));
					if (shortcuts.size() >= SHORTCUT_COUNT) break;
				}
			}
		} finally {
			if (cursor != null) cursor.close();
		}

		shortcutManager.setDynamicShortcuts(shortcuts);
	}

	@RequiresApi(VERSION_CODES.N_MR1)
	@NonNull
	private ShortcutInfo createShortcutInfo(long viewId, @NonNull String viewName) {
		return new ShortcutInfo.Builder(context, createShortcutName(viewId))
			.setShortLabel(StringUtils.limitText(viewName, ShortcutUtils.SHORT_LABEL_LENGTH))
			.setLongLabel(StringUtils.limitText(viewName, ShortcutUtils.LONG_LABEL_LENGTH))
			.setIcon(Icon.createWithResource(context, R.drawable.ic_shortcut_ic_collection))
			.setIntent(CollectionActivity.createIntentAsShortcut(context, viewId))
			.build();
	}

	@NonNull
	private static String createShortcutName(long viewId) {
		return "collection-view-" + viewId;
	}
}
