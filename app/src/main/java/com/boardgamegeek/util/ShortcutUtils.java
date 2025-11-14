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
		task.execute();
	}

	public static void createGameShortcut(Context context, int gameId, String gameName, String thumbnailUrl) {
		GameShortcutTask task = new GameShortcutTask(context, gameId, gameName, thumbnailUrl);
		task.execute();
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

	public static @NonNull String createGameShortcutId(int gameId) {
		return "game-" + gameId;
	}
}
