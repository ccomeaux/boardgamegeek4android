package com.boardgamegeek.util;

import java.net.URLEncoder;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;

import com.boardgamegeek.R;
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
}
