package com.boardgamegeek.util;

import java.net.URLEncoder;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.ui.BoardgameActivity;
import com.boardgamegeek.ui.CommentsActivity;
import com.boardgamegeek.ui.LogPlayActivity;

public class ActivityUtils {

	public static void shareGame(Context context, int gameId, String gameName) {
		Resources r = context.getResources();
		Intent shareIntent = new Intent(Intent.ACTION_SEND);
		shareIntent.setType("text/plain");
		shareIntent.putExtra(Intent.EXTRA_SUBJECT, String.format(r.getString(R.string.share_subject), gameName));
		shareIntent.putExtra(
				Intent.EXTRA_TEXT,
				String.format(r.getString(R.string.share_text), gameName, "http://www.boardgamegeek.com/boardgame/"
						+ gameId));
		context.startActivity(Intent.createChooser(shareIntent, r.getString(R.string.share_title)));
	}

	public static void logPlay(Context context, boolean quick, int id, String name, String thumbnailUrl) {
		Intent intent = new Intent(context, LogPlayActivity.class);
		intent.setAction(quick ? Intent.ACTION_VIEW : Intent.ACTION_EDIT);
		intent.putExtra(LogPlayActivity.KEY_GAME_ID, id);
		intent.putExtra(LogPlayActivity.KEY_GAME_NAME, name);
		intent.putExtra(LogPlayActivity.KEY_THUMBNAIL_URL, thumbnailUrl);
		context.startActivity(intent);
	}

	public static void linkBgg(Context context, int gameId) {
		link(context, "http://www.boardgamegeek.com/boardgame/" + gameId);
	}

	public static void linkBgPrices(Context context, String gameName) {
		link(context, "http://boardgameprices.com/iphone/?s=" + URLEncoder.encode(gameName));
	}

	public static void linkAmazon(Context context, String gameName) {
		link(context, "http://www.amazon.com/gp/aw/s.html/?m=aps&k=" + URLEncoder.encode(gameName)
				+ "&i=toys-and-games&submitSearch=GO");
	}

	public static void linkEbay(Context context, String gameName) {
		link(context, "http://shop.mobileweb.ebay.com/searchresults?kw=" + URLEncoder.encode(gameName));
	}

	private static void link(Context context, String link) {
		context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
	}

	public static void showComments(Context context, int gameId, String gameName, String thumbnailUrl) {
		Intent intent = new Intent(context, CommentsActivity.class);
		intent.putExtra(CommentsActivity.KEY_GAME_ID, gameId);
		intent.putExtra(CommentsActivity.KEY_GAME_NAME, gameName);
		intent.putExtra(CommentsActivity.KEY_THUMBNAIL_URL, thumbnailUrl);
		context.startActivity(intent);
	}

	public static Intent createShortcut(Context context, int gameId, String gameName, String iconUrl) {
		Intent intent = new Intent(Intent.ACTION_VIEW, Games.buildGameUri(gameId));
		intent.putExtra(BoardgameActivity.KEY_GAME_NAME, gameName);

		Intent shortcut = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, gameName);

		BitmapDrawable d = (BitmapDrawable) ImageCache.getDrawableFromCache(iconUrl);
		if (d == null) {
			shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
					Intent.ShortcutIconResource.fromContext(context, R.drawable.bgg_logo));
		} else {
			Bitmap croppedBitmap = cropBitmap(d);

			// load and size bezel drawables
			Rect bounds = new Rect(0, 0, croppedBitmap.getWidth(), croppedBitmap.getHeight());
			Drawable maskDrawable = context.getResources().getDrawable(R.drawable.bezel_mask);
			maskDrawable.setBounds(bounds);
			Drawable borderDrawable = context.getResources().getDrawable(R.drawable.bezel_border);
			borderDrawable.setBounds(bounds);

			// setup paints
			Paint copyPaint = new Paint();
			Paint maskedPaint = new Paint();
			maskedPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));

			// create composite bitmap
			Bitmap compositeBitmap = Bitmap.createBitmap(croppedBitmap.getWidth(), croppedBitmap.getHeight(),
					Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(compositeBitmap);

			// assemble bitmaps
			RectF boundsF = new RectF(bounds);
			int sc = canvas.saveLayer(boundsF, copyPaint, Canvas.HAS_ALPHA_LAYER_SAVE_FLAG
					| Canvas.FULL_COLOR_LAYER_SAVE_FLAG);
			maskDrawable.draw(canvas);
			canvas.saveLayer(boundsF, maskedPaint, 0);
			canvas.drawBitmap(croppedBitmap, 0, 0, copyPaint);
			canvas.restoreToCount(sc);
			borderDrawable.draw(canvas);

			Bitmap icon = Bitmap.createScaledBitmap(compositeBitmap, 128, 128, true);
			shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON, icon);
		}
		return shortcut;
	}

	private static Bitmap cropBitmap(BitmapDrawable d) {
		Bitmap b = d.getBitmap();
		int w = b.getWidth();
		int h = b.getHeight();
		int min = Math.min(w, h);
		int x = (w - min) / 2;
		int y = (h - min) / 2;
		Bitmap croppedBitmap = Bitmap.createBitmap(b, x, y, min, min);
		return croppedBitmap;
	}
}
