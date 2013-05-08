package com.boardgamegeek.util;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.ui.GameActivity;
import com.boardgamegeek.ui.LogPlayActivity;
import com.boardgamegeek.ui.PlayActivity;

public class ActivityUtils {
	// private static final String TAG = makeLogTag(ActivityUtils.class);

	@SuppressLint("CommitTransaction")
	public static void launchDialog(Fragment host, DialogFragment dialog, String tag, Bundle arguments) {
		FragmentTransaction ft = host.getFragmentManager().beginTransaction();
		Fragment prev = host.getFragmentManager().findFragmentByTag(tag);
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);

		dialog.setArguments(arguments);
		dialog.show(ft, tag);
	}

	public static Dialog createCancelDialog(final Activity activity) {
		return ActivityUtils.createConfirmationDialog(activity, R.string.are_you_sure_message,
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					activity.setResult(Activity.RESULT_CANCELED);
					activity.finish();
				}
			});
	}

	public static Dialog createConfirmationDialog(Context context, int messageId,
		DialogInterface.OnClickListener okListener) {
		return createConfirmationDialog(context, messageId, null, okListener, null);
	}

	public static Dialog createConfirmationDialog(Context context, int messageId, View view,
		DialogInterface.OnClickListener okListener, DialogInterface.OnClickListener cancelListener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context).setCancelable(true)
			.setNegativeButton(android.R.string.cancel, cancelListener)
			.setPositiveButton(android.R.string.ok, okListener).setTitle(R.string.are_you_sure_title);
		builder = addIcon(builder);
		if (messageId != -1) {
			builder.setMessage(messageId);
		}
		if (view != null) {
			builder.setView(view);
		}

		return builder.create();
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private static AlertDialog.Builder addIcon(AlertDialog.Builder builder) {
		if (VersionUtils.hasHoneycomb()) {
			return builder.setIconAttribute(android.R.attr.alertDialogIcon);
		} else {
			return builder.setIcon(android.R.drawable.ic_dialog_alert);
		}
	}

	public static void launchGame(Context context, int gameId, String gameName) {
		final Intent intent = createGameIntent(gameId, gameName);
		context.startActivity(intent);
	}

	public static void navigateUpToGame(Context context, int gameId, String gameName) {
		final Intent intent = createGameIntent(gameId, gameName);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		context.startActivity(intent);
	}

	private static Intent createGameIntent(int gameId, String gameName) {
		final Uri gameUri = Games.buildGameUri(gameId);
		final Intent intent = new Intent(Intent.ACTION_VIEW, gameUri);
		intent.putExtra(GameActivity.KEY_GAME_NAME, gameName);
		return intent;
	}

	public static void share(Context context, String subject, String text, int titleResId) {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_SUBJECT, subject);
		intent.putExtra(Intent.EXTRA_TEXT, text);
		context.startActivity(Intent.createChooser(intent, context.getResources().getString(titleResId)));
	}

	public static void shareGame(Context context, int gameId, String gameName) {
		Resources r = context.getResources();
		share(
			context,
			String.format(r.getString(R.string.share_subject), gameName),
			String.format(r.getString(R.string.share_text), gameName, "http://www.boardgamegeek.com/boardgame/"
				+ gameId), R.string.share_title);
	}

	public static void launchPlay(Context context, int playId, int gameId, String gameName) {
		Uri playUri = Plays.buildPlayUri(playId);
		Intent intent = new Intent(Intent.ACTION_VIEW, playUri);
		intent.putExtra(PlayActivity.KEY_GAME_ID, gameId);
		intent.putExtra(PlayActivity.KEY_GAME_NAME, gameName);
		context.startActivity(intent);
	}

	public static void editPlay(Activity activity, int playId, int gameId, String gameName, int requestCode) {
		Intent intent = new Intent(activity, LogPlayActivity.class);
		intent.setAction(Intent.ACTION_EDIT);
		intent.putExtra(LogPlayActivity.KEY_PLAY_ID, playId);
		intent.putExtra(LogPlayActivity.KEY_GAME_ID, gameId);
		intent.putExtra(LogPlayActivity.KEY_GAME_NAME, gameName);
		activity.startActivityForResult(intent, requestCode);
	}

	public static void logPlay(Context context, boolean quick, int gameId, String gameName) {
		Intent intent = new Intent(context, LogPlayActivity.class);
		intent.setAction(quick ? Intent.ACTION_VIEW : Intent.ACTION_EDIT);
		intent.putExtra(LogPlayActivity.KEY_GAME_ID, gameId);
		intent.putExtra(LogPlayActivity.KEY_GAME_NAME, gameName);
		context.startActivity(intent);
	}

	public static void linkBgg(Context context, int gameId) {
		if (gameId <= 0) {
			return;
		}
		link(context, "http://www.boardgamegeek.com/boardgame/" + gameId);
	}

	public static void linkBgPrices(Context context, String gameName) {
		if (TextUtils.isEmpty(gameName)) {
			return;
		}
		link(context, "http://boardgameprices.com/iphone/?s=" + HttpUtils.encode(gameName));
	}

	public static void linkAmazon(Context context, String gameName) {
		if (TextUtils.isEmpty(gameName)) {
			return;
		}
		link(context, "http://www.amazon.com/gp/aw/s.html/?m=aps&k=" + HttpUtils.encode(gameName)
			+ "&i=toys-and-games&submitSearch=GO");
	}

	public static void linkEbay(Context context, String gameName) {
		if (TextUtils.isEmpty(gameName)) {
			return;
		}
		link(context, "http://shop.mobileweb.ebay.com/searchresults?kw=" + HttpUtils.encode(gameName));
	}

	private static void link(Context context, String link) {
		context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
	}

	public static void setActionBarText(Menu menu, int id, String text) {
		MenuItem item = menu.findItem(id);
		if (item != null) {
			TextView tv = (TextView) item.getActionView().findViewById(R.id.actionbar_text);
			if (tv != null) {
				tv.setText(text);
			}
		}
	}

	public static void setCustomActionBarText(ActionBar actionBar, int id, String text) {
		if (actionBar != null) {
			TextView tv = (TextView) actionBar.getCustomView().findViewById(R.id.actionbar_text);
			if (tv != null) {
				tv.setText(text);
			}
		}
	}

	public static Intent createShortcut(Context context, int gameId, String gameName) {
		Intent intent = new Intent(Intent.ACTION_VIEW, Games.buildGameUri(gameId));
		intent.putExtra(GameActivity.KEY_GAME_NAME, gameName);

		Intent shortcut = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, gameName);

		BitmapDrawable d = ImageUtils.processDrawableFromResolver(context, Games.buildThumbnailUri(gameId), null);
		if (d == null) {
			shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
				Intent.ShortcutIconResource.fromContext(context, R.drawable.ic_launcher));
		} else {
			Bitmap icon = squarifyBitmap(d);

			// load and size bezel drawables
			Rect bounds = new Rect(0, 0, icon.getWidth(), icon.getHeight());
			Drawable maskDrawable = context.getResources().getDrawable(R.drawable.bezel_mask);
			maskDrawable.setBounds(bounds);
			Drawable borderDrawable = context.getResources().getDrawable(R.drawable.bezel_border);
			borderDrawable.setBounds(bounds);

			// setup paints
			Paint copyPaint = new Paint();
			Paint maskedPaint = new Paint();
			maskedPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));

			// create composite bitmap
			Bitmap compositeBitmap = Bitmap.createBitmap(icon.getWidth(), icon.getHeight(), Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(compositeBitmap);

			// assemble bitmaps
			RectF boundsF = new RectF(bounds);
			int sc = canvas.saveLayer(boundsF, copyPaint, Canvas.HAS_ALPHA_LAYER_SAVE_FLAG
				| Canvas.FULL_COLOR_LAYER_SAVE_FLAG);
			maskDrawable.draw(canvas);
			canvas.saveLayer(boundsF, maskedPaint, 0);
			canvas.drawBitmap(icon, 0, 0, copyPaint);
			canvas.restoreToCount(sc);
			borderDrawable.draw(canvas);

			shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON, Bitmap.createScaledBitmap(compositeBitmap, 72, 72, true));
		}
		return shortcut;
	}

	private static Bitmap squarifyBitmap(BitmapDrawable d) {
		Bitmap b = d.getBitmap();
		int w = b.getWidth();
		int h = b.getHeight();
		int min = Math.min(w, h);
		int x = (w - min) / 2;
		int y = (h - min) / 2;
		return Bitmap.createBitmap(b, x, y, min, min);
	}
}
