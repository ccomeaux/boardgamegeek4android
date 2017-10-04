package com.boardgamegeek.util.shortcut;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.ShortcutUtils;
import com.boardgamegeek.util.StringUtils;
import com.squareup.picasso.Picasso;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import timber.log.Timber;

public abstract class ShortcutTask extends AsyncTask<Void, Void, Void> {
	@SuppressLint("StaticFieldLeak") @Nullable protected final Context context;
	private final String thumbnailUrl;

	public ShortcutTask(@Nullable Context context) {
		this(context, null);
	}

	public ShortcutTask(@Nullable Context context, String thumbnailUrl) {
		this.context = context == null ? null : context.getApplicationContext();
		this.thumbnailUrl = HttpUtils.ensureScheme(thumbnailUrl);
	}

	protected abstract String getShortcutName();

	protected abstract Intent createIntent();

	protected int getShortcutIconResId() {
		return R.mipmap.ic_launcher_foreground;
	}

	protected abstract String getId();

	@Nullable
	@Override
	protected Void doInBackground(Void... params) {
		if (context == null) return null;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			createShortcutForOreo();
		} else {
			Intent shortcutIntent = ShortcutUtils.createShortcutIntent(context, getShortcutName(), createIntent(), getShortcutIconResId());
			if (!TextUtils.isEmpty(thumbnailUrl)) {
				Bitmap bitmap = fetchThumbnail();
				if (bitmap != null) {
					shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap);
				}
			}
			context.sendBroadcast(shortcutIntent);
		}
		return null;
	}

	@RequiresApi(api = VERSION_CODES.O)
	private void createShortcutForOreo() {
		if (context == null) return;
		ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
		if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported()) {
			ShortcutInfo.Builder builder = new ShortcutInfo.Builder(context, getId())
				.setShortLabel(StringUtils.limitText(getShortcutName(), ShortcutUtils.SHORT_LABEL_LENGTH))
				.setLongLabel(StringUtils.limitText(getShortcutName(), ShortcutUtils.LONG_LABEL_LENGTH))
				.setIntent(createIntent());
			if (!TextUtils.isEmpty(thumbnailUrl)) {
				Bitmap bitmap = fetchThumbnail();
				if (bitmap != null) {
					builder.setIcon(Icon.createWithAdaptiveBitmap(bitmap));
				} else {
					builder.setIcon(Icon.createWithResource(context, getShortcutIconResId()));
				}
			} else {
				builder.setIcon(Icon.createWithResource(context, getShortcutIconResId()));
			}
			shortcutManager.requestPinShortcut(builder.build(), null);
		}
	}

	@Override
	protected void onPostExecute(Void nothing) {
	}

	@Nullable
	private Bitmap fetchThumbnail() {
		Bitmap bitmap = null;
		File file = ShortcutUtils.getThumbnailFile(context, thumbnailUrl);
		if (file != null && file.exists()) {
			bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
		} else {
			try {
				bitmap = Picasso.with(context)
					.load(thumbnailUrl)
					.resizeDimen(R.dimen.shortcut_icon_size, R.dimen.shortcut_icon_size)
					.centerCrop()
					.get();
			} catch (IOException e) {
				Timber.e(e, "Error downloading the thumbnail.");
			}
		}
		if (bitmap != null && file != null) {
			try {
				OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
				bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
				out.close();
			} catch (IOException e) {
				Timber.e(e, "Error saving the thumbnail file.");
			}
		}
		return bitmap;
	}
}
