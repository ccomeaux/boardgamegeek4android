package com.boardgamegeek.util;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

	public static void createCollectionShortcut(Context context, long viewId, String viewName) {
		CollectionShortcutTask task = new CollectionShortcutTask(context, viewId, viewName);
		TaskUtils.executeAsyncTask(task);
	}

	public static void createGameShortcut(Context context, int gameId, String gameName, String thumbnailUrl) {
		GameShortcutTask task = new GameShortcutTask(context, gameId, gameName, thumbnailUrl);
		TaskUtils.executeAsyncTask(task);
	}

	@Nullable
	public static File getThumbnailFile(Context context, String url) {
		if (!TextUtils.isEmpty(url)) {
			String filename = FileUtils.getFileNameFromUrl(url);
			if (filename != null) {
				return new File(FileUtils.generateContentPath(context, BggContract.PATH_THUMBNAILS), filename);
			}
		}
		return null;
	}

	@NonNull
	public static Intent createShortcutIntent(Context context, String shortcutName, Intent intent) {
		return createShortcutIntent(context, shortcutName, intent, R.drawable.ic_launcher_old);
	}

	@NonNull
	public static Intent createShortcutIntent(Context context, String shortcutName, Intent intent, @DrawableRes int shortcutIconResId) {
		Intent shortcut = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutName);
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context, shortcutIconResId));
		return shortcut;
	}
}
