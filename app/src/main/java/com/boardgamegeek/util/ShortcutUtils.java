package com.boardgamegeek.util;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.extensions.TaskUtils;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.shortcut.CollectionShortcutTask;
import com.boardgamegeek.util.shortcut.GameShortcutTask;

import java.io.File;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Helps create shortcuts.
 */
public class ShortcutUtils {
	public static final int SHORT_LABEL_LENGTH = 12;
	public static final int LONG_LABEL_LENGTH = 25;

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
		return createShortcutIntent(context, shortcutName, intent, R.mipmap.ic_launcher_foreground);
	}

	@NonNull
	public static Intent createShortcutIntent(Context context, String shortcutName, Intent intent, @DrawableRes int shortcutIconResId) {
		Intent shortcut = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutName);
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context, shortcutIconResId));
		return shortcut;
	}

	public static @NonNull String createGameShortcutId(int gameId) {
		return "game-" + gameId;
	}
}
