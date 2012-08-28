package com.boardgamegeek.util;

import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

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
import android.text.TextUtils;
import android.widget.Toast;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.ui.BoardgameActivity;
import com.boardgamegeek.ui.CommentsActivity;
import com.boardgamegeek.ui.LogPlayActivity;

public class ActivityUtils {
	private static final String TAG = makeLogTag(ActivityUtils.class);

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

	public static void logPlay(Context context, int playId, int gameId, String gameName) {
		Intent intent = new Intent(context, LogPlayActivity.class);
		intent.setAction(Intent.ACTION_EDIT);
		intent.putExtra(LogPlayActivity.KEY_PLAY_ID, playId);
		intent.putExtra(LogPlayActivity.KEY_GAME_ID, gameId);
		intent.putExtra(LogPlayActivity.KEY_GAME_NAME, gameName);
		context.startActivity(intent);
	}

	public static void logPlay(Context context, boolean quick, int gameId, String gameName) {
		Intent intent = new Intent(context, LogPlayActivity.class);
		intent.setAction(quick ? Intent.ACTION_VIEW : Intent.ACTION_EDIT);
		intent.putExtra(LogPlayActivity.KEY_GAME_ID, gameId);
		intent.putExtra(LogPlayActivity.KEY_GAME_NAME, gameName);
		context.startActivity(intent);
	}

	public static boolean deletePlay(Context context, CookieStore cookieStore, int playId) {
		UrlEncodedFormEntity entity = null;
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("ajax", "1"));
		nvps.add(new BasicNameValuePair("action", "delete"));
		nvps.add(new BasicNameValuePair("playid", String.valueOf(playId)));
		try {
			entity = new UrlEncodedFormEntity(nvps, HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {
			LOGE(TAG, "Trying to encode play for deletion", e);
		}

		HttpClient client = HttpUtils.createHttpClient(context, cookieStore);
		HttpPost post = new HttpPost(BggApplication.siteUrl + "geekplay.php");
		post.setEntity(entity);

		String message = "";
		HttpResponse response = null;
		try {
			Resources r = context.getResources();
			response = client.execute(post);
			if (response == null) {
				message = r.getString(R.string.logInError) + " : " + r.getString(R.string.logInErrorSuffixNoResponse);
			} else if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				message = r.getString(R.string.logInError) + " : " + r.getString(R.string.logInErrorSuffixBadResponse)
					+ " " + response.toString() + ".";
			} else {
				message = HttpUtils.parseResponse(response);
				if (message.contains("<title>Plays ") || message.contains("That play doesn't exist")) {
					message = "";
				}
			}
		} catch (ClientProtocolException e) {
			message = e.toString();
		} catch (IOException e) {
			message = e.toString();
		} finally {
			if (client != null && client.getConnectionManager() != null) {
				client.getConnectionManager().shutdown();
			}
		}

		if (TextUtils.isEmpty(message)) {
			context.getContentResolver().delete(Plays.buildPlayUri(playId), null, null);
			Toast.makeText(context, R.string.msg_play_deleted, Toast.LENGTH_LONG).show();
			return true;
		} else {
			LOGE(TAG, message);
			Toast.makeText(context, message, Toast.LENGTH_LONG).show();
			return false;
		}
	}

	public static void linkBgg(Context context, int gameId) {
		link(context, "http://www.boardgamegeek.com/boardgame/" + gameId);
	}

	public static void linkBgPrices(Context context, String gameName) {
		link(context, "http://boardgameprices.com/iphone/?s=" + HttpUtils.encode(gameName));
	}

	public static void linkAmazon(Context context, String gameName) {
		link(context, "http://www.amazon.com/gp/aw/s.html/?m=aps&k=" + HttpUtils.encode(gameName)
			+ "&i=toys-and-games&submitSearch=GO");
	}

	public static void linkEbay(Context context, String gameName) {
		link(context, "http://shop.mobileweb.ebay.com/searchresults?kw=" + HttpUtils.encode(gameName));
	}

	private static void link(Context context, String link) {
		context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
	}

	public static void showComments(Context context, int gameId, String gameName) {
		Intent intent = new Intent(context, CommentsActivity.class);
		intent.putExtra(CommentsActivity.KEY_GAME_ID, gameId);
		intent.putExtra(CommentsActivity.KEY_GAME_NAME, gameName);
		context.startActivity(intent);
	}

	public static Intent createShortcut(Context context, int gameId, String gameName) {
		Intent intent = new Intent(Intent.ACTION_VIEW, Games.buildGameUri(gameId));
		intent.putExtra(BoardgameActivity.KEY_GAME_NAME, gameName);

		Intent shortcut = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, gameName);

		BitmapDrawable d = (BitmapDrawable) ImageCache.getDrawable(context, Games.buildThumbnailUri(gameId));
		if (d == null) {
			shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
				Intent.ShortcutIconResource.fromContext(context, R.drawable.bgg_logo));
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
