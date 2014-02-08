package com.boardgamegeek.util;

import java.util.List;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
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
import android.util.Pair;
import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.boardgamegeek.R;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.persister.PlayPersister;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.ui.GameActivity;
import com.boardgamegeek.ui.LogPlayActivity;
import com.boardgamegeek.ui.PlayActivity;

public class ActivityUtils {
	// private static final String TAG = makeLogTag(ActivityUtils.class);

	public final static String KEY_TITLE = "TITLE";
	public final static String KEY_GAME_ID = "GAME_ID";
	public final static String KEY_GAME_NAME = "GAME_NAME";
	public final static String KEY_QUERY_TOKEN = "QUERY_TOKEN";

	private static final String BGG_URL_BASE = "http://www.boardgamegeek.com/";
	private static final Uri BGG_URI = Uri.parse(BGG_URL_BASE);
	private static final String BOARDGAME_URL_PREFIX = BGG_URL_BASE + "boardgame/";

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
		return ActivityUtils.createConfirmationDialog(activity, R.string.are_you_sure_cancel,
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					activity.setResult(Activity.RESULT_CANCELED);
					activity.finish();
				}
			});
	}

	public static Dialog createConfirmationDialog(Context context, String message,
		DialogInterface.OnClickListener okListener) {
		return createConfirmationDialog(context, -1, message, null, okListener, null);
	}

	public static Dialog createConfirmationDialog(Context context, int messageId,
		DialogInterface.OnClickListener okListener) {
		return createConfirmationDialog(context, messageId, null, null, okListener, null);
	}

	private static Dialog createConfirmationDialog(Context context, int messageId, String message, View view,
		DialogInterface.OnClickListener okListener, DialogInterface.OnClickListener cancelListener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context).setCancelable(true)
			.setNegativeButton(android.R.string.cancel, cancelListener)
			.setPositiveButton(android.R.string.ok, okListener).setTitle(R.string.are_you_sure_title);
		builder = addIcon(builder);
		if (messageId != -1) {
			builder.setMessage(messageId);
		} else {
			builder.setMessage(message);
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
		intent.putExtra(Intent.EXTRA_SUBJECT, subject.trim());
		intent.putExtra(Intent.EXTRA_TEXT, text.trim());
		context.startActivity(Intent.createChooser(intent, context.getResources().getString(titleResId)));
	}

	public static void shareGame(Context context, int gameId, String gameName) {
		Resources r = context.getResources();
		String subject = String.format(r.getString(R.string.share_game_subject), gameName);
		String text = r.getString(R.string.share_game_text) + "\n\n" + formatGameLink(gameId, gameName);
		share(context, subject, text, R.string.title_share_game);
	}

	public static void shareGames(Context context, List<Pair<Integer, String>> games) {
		Resources r = context.getResources();
		StringBuilder text = new StringBuilder(r.getString(R.string.share_games_text));
		text.append("\n").append("\n");
		for (Pair<Integer, String> game : games) {
			text.append(formatGameLink(game.first, game.second));
		}
		share(context, r.getString(R.string.share_games_subject), text.toString(), R.string.title_share_games);
	}

	private static String formatGameLink(int id, String name) {
		return name + " (" + BOARDGAME_URL_PREFIX + id + ")\n";
	}

	public static void launchPlay(Context context, int playId, int gameId, String gameName, String thumbnailUrl) {
		Intent intent = createPlayIntent(playId, gameId, gameName, thumbnailUrl);
		context.startActivity(intent);
	}

	public static Intent createPlayIntent(int playId, int gameId, String gameName) {
		return createPlayIntent(playId, gameId, gameName, null);
	}

	public static Intent createPlayIntent(int playId, int gameId, String gameName, String thumbnailUrl) {
		Uri playUri = Plays.buildPlayUri(playId);
		Intent intent = new Intent(Intent.ACTION_VIEW, playUri);
		intent.putExtra(PlayActivity.KEY_GAME_ID, gameId);
		intent.putExtra(PlayActivity.KEY_GAME_NAME, gameName);
		intent.putExtra(PlayActivity.KEY_THUMBNAIL_URL, thumbnailUrl);
		return intent;
	}

	public static void editPlay(Context context, int playId, int gameId, String gameName) {
		Intent intent = createEditPlayIntent(context, playId, gameId, gameName);
		context.startActivity(intent);
	}

	public static void endPlay(Context context, int playId, int gameId, String gameName) {
		Intent intent = createEditPlayIntent(context, playId, gameId, gameName);
		intent.putExtra(LogPlayActivity.KEY_END_PLAY, true);
		context.startActivity(intent);
	}

	public static void logPlayAgain(Context context, int playId, int gameId, String gameName) {
		Intent intent = createEditPlayIntent(context, playId, gameId, gameName);
		intent.putExtra(LogPlayActivity.KEY_PLAY_AGAIN, true);
		context.startActivity(intent);
	}

	public static void logPlay(Context context, int gameId, String gameName) {
		Intent intent = createEditPlayIntent(context, 0, gameId, gameName);
		context.startActivity(intent);
	}

	private static Intent createEditPlayIntent(Context context, int playId, int gameId, String gameName) {
		return createEditPlayIntent(context, playId, gameId, gameName, null);
	}

	private static Intent createEditPlayIntent(Context context, int playId, int gameId, String gameName,
		String thumbnailUrl) {
		Intent intent = createPlayIntent(playId, gameId, gameName, thumbnailUrl);
		intent.setAction(Intent.ACTION_EDIT);
		return intent;
	}

	public static void logQuickPlay(Context context, int gameId, String gameName) {
		Play play = new Play(gameId, gameName);
		play.SyncStatus = Play.SYNC_STATUS_PENDING_UPDATE;
		PlayPersister.save(context.getContentResolver(), play);
		SyncService.sync(context, SyncService.FLAG_SYNC_PLAYS_UPLOAD);
	}

	public static void linkBgg(Context context, int gameId) {
		if (gameId <= 0) {
			return;
		}
		link(context, BOARDGAME_URL_PREFIX + gameId);
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

	public static void link(Context context, String link) {
		Intent intent = new Intent(Intent.ACTION_EDIT, Uri.parse(link));
		intent.addCategory(Intent.CATEGORY_BROWSABLE);
		List<ResolveInfo> activities = context.getPackageManager().queryIntentActivities(intent, 0);
		if (activities.size() > 0) {
			context.startActivity(intent);
		}
	}

	public static void linkToBgg(Context context, String path) {
		context.startActivity(new Intent(Intent.ACTION_VIEW, createBggUri(path)));
	}

	public static Uri createBggUri(String path) {
		return Uri.withAppendedPath(BGG_URI, path);
	}

	public static void setActionBarText(Menu menu, int id, String text) {
		setActionBarText(menu, id, text, null);
	}

	public static void setActionBarText(Menu menu, int id, String text1, String text2) {
		MenuItem item = menu.findItem(id);
		if (item != null) {
			TextView tv1 = (TextView) item.getActionView().findViewById(android.R.id.text1);
			if (tv1 != null) {
				tv1.setText(text1);
			}
			TextView tv2 = (TextView) item.getActionView().findViewById(android.R.id.text2);
			if (tv2 != null) {
				tv2.setText(text2);
			}
		}
	}

	public static void setCustomActionBarText(ActionBar actionBar, String text) {
		if (actionBar != null) {
			setCustomTextView(actionBar, android.R.id.text1, text);
		}
	}

	public static void setCustomActionBarText(ActionBar actionBar, String text1, String text2) {
		if (actionBar != null) {
			setCustomTextView(actionBar, android.R.id.text1, text1);
			setCustomTextView(actionBar, android.R.id.text2, text2);
		}
	}

	private static void setCustomTextView(ActionBar actionBar, int id, String text) {
		TextView tv = (TextView) actionBar.getCustomView().findViewById(id);
		if (tv != null) {
			tv.setText(text);
		}
	}

	public static Intent createGameShortcut(Context context, int gameId, String gameName) {
		Intent intent = new Intent(Intent.ACTION_VIEW, Games.buildGameUri(gameId));
		intent.putExtra(GameActivity.KEY_GAME_NAME, gameName);
		intent.putExtra(GameActivity.KEY_FROM_SHORTCUT, true);

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
