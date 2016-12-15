package com.boardgamegeek.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.squareup.picasso.Picasso;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import timber.log.Timber;

/**
 * Helps create shortcuts.
 */
public class ShortcutUtils {
	private ShortcutUtils() {
	}

	public static void createShortcut(Context context, int gameId, String gameName, String thumbnailUrl) {
		shortcutTask task = new ShortcutUtils.shortcutTask(context, gameId, gameName, thumbnailUrl);
		TaskUtils.executeAsyncTask(task);
	}

	public static Intent createIntent(Context context, int gameId, String gameName, String thumbnailUrl) {
		Intent shortcut = createShortcutIntent(context, gameId, gameName);
		File file = getThumbnailFile(context, thumbnailUrl);
		if (file != null && file.exists()) {
			shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON, BitmapFactory.decodeFile(file.getAbsolutePath()));
		}
		return shortcut;
	}

	private static Intent createShortcutIntent(Context context, int gameId, String gameName) {
		Intent intent = ActivityUtils.createGameIntent(gameId, gameName);
		intent.putExtra(ActivityUtils.KEY_FROM_SHORTCUT, true);

		Intent shortcut = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, gameName);
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
			Intent.ShortcutIconResource.fromContext(context, R.drawable.ic_launcher));

		return shortcut;
	}

	private static File getThumbnailFile(Context context, String url) {
		if (!TextUtils.isEmpty(url)) {
			String filename = FileUtils.getFileNameFromUrl(url);
			if (filename != null) {
				return new File(FileUtils.generateContentPath(context, BggContract.PATH_THUMBNAILS), filename);
			}
		}
		return null;
	}

	private static class shortcutTask extends AsyncTask<Void, Void, Void> {
		private final Context context;
		private final int gameId;
		private final String gameName;
		private final String thumbnailUrl;

		public shortcutTask(Context context, int gameId, String gameName, String thumbnailUrl) {
			this.context = context.getApplicationContext();
			this.gameId = gameId;
			this.gameName = gameName;
			this.thumbnailUrl = HttpUtils.ensureScheme(thumbnailUrl);
		}

		@Override
		protected Void doInBackground(Void... params) {
			Intent shortcutIntent = createShortcutIntent(context, gameId, gameName);
			if (!TextUtils.isEmpty(thumbnailUrl)) {
				Bitmap bitmap = fetchThumbnail();
				if (bitmap != null) {
					shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap);
				}
			}

			context.sendBroadcast(shortcutIntent);
			return null;
		}

		@Override
		protected void onPostExecute(Void nothing) {
		}

		private Bitmap fetchThumbnail() {
			Bitmap bitmap = null;
			File file = getThumbnailFile(context, thumbnailUrl);
			if (file != null) {
				if (file.exists()) {
					bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
				} else {
					try {
						bitmap = Picasso.with(context)
							.load(thumbnailUrl)
							.resizeDimen(R.dimen.shortcut_icon_size, R.dimen.shortcut_icon_size)
							.centerCrop().get();
					} catch (IOException e) {
						Timber.e(e, "Error downloading the thumbnail.");
					}
					if (bitmap != null) {
						try {
							OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
							bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
							out.close();
						} catch (IOException e) {
							Timber.e(e, "Error saving the thumbnail file.");
						}
					}
				}
			}
			return bitmap;
		}
	}
}
