package com.boardgamegeek.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.shortcut.CollectionShortcutTask;
import com.boardgamegeek.util.shortcut.GameShortcutTask;

import java.io.File;

/**
 * Helps create shortcuts.
 */
public class ShortcutUtils {
	private ShortcutUtils() {
	}

	public static void createCollectionShortcut(Context context, long viewId, String viewName, String thumbnailUrl) {
		CollectionShortcutTask task = new CollectionShortcutTask(context, viewId, viewName, thumbnailUrl);
		TaskUtils.executeAsyncTask(task);
	}

	public static void createGameShortcut(Context context, int gameId, String gameName, String thumbnailUrl) {
		GameShortcutTask task = new GameShortcutTask(context, gameId, gameName, thumbnailUrl);
		TaskUtils.executeAsyncTask(task);
	}

	public static Intent createGameIntent(Context context, int gameId, String gameName, String thumbnailUrl) {
		Intent shortcut = createGameShortcutIntent(context, gameId, gameName);
		File file = getThumbnailFile(context, thumbnailUrl);
		if (file != null && file.exists()) {
			shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON, BitmapFactory.decodeFile(file.getAbsolutePath()));
		}
		return shortcut;
	}

	public static File getThumbnailFile(Context context, String url) {
		if (!TextUtils.isEmpty(url)) {
			String filename = FileUtils.getFileNameFromUrl(url);
			if (filename != null) {
				return new File(FileUtils.generateContentPath(context, BggContract.PATH_THUMBNAILS), filename);
			}
		}
		return null;
	}

	public static Intent createGameShortcutIntent(Context context, int gameId, String gameName) {
		Intent intent = ActivityUtils.createGameIntent(gameId, gameName);
		intent.putExtra(ActivityUtils.KEY_FROM_SHORTCUT, true);
		return createShortcutIntent(context, gameName, intent);
	}

	@NonNull
	public static Intent createShortcutIntent(Context context, String shortcutName, Intent intent) {
		Intent shortcut = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutName);
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
			Intent.ShortcutIconResource.fromContext(context, R.drawable.ic_launcher));

		return shortcut;
	}
}
