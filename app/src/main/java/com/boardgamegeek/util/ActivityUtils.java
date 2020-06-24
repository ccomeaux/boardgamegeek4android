package com.boardgamegeek.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Pair;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.persister.PlayPersister;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.service.SyncService;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.analytics.FirebaseAnalytics.Event;
import com.google.firebase.analytics.FirebaseAnalytics.Param;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.StringRes;
import androidx.core.app.ShareCompat;
import timber.log.Timber;

public class ActivityUtils {
	private static final String BOARDGAME_PATH = "boardgame";
	private static final Uri BGG_URI = Uri.parse("https://www.boardgamegeek.com/");

	public static void share(Activity activity, String subject, CharSequence text, @StringRes int titleResId) {
		Intent intent = ShareCompat.IntentBuilder.from(activity)
			.setType("text/plain")
			.setSubject(subject.trim())
			.setText(text)
			.setChooserTitle(titleResId)
			.createChooserIntent();
		if (intent.resolveActivity(activity.getPackageManager()) != null) {
			activity.startActivity(intent);
		}
	}

	public static void shareGame(Activity activity, int gameId, String gameName, String method) {
		Resources r = activity.getResources();
		String subject = String.format(r.getString(R.string.share_game_subject), gameName);
		String text = r.getString(R.string.share_game_text) + "\n\n" + formatGameLink(gameId, gameName);
		share(activity, subject, text, R.string.title_share_game);
		Bundle bundle = new Bundle();
		bundle.putString(Param.METHOD, method);
		bundle.putString(Param.CONTENT_TYPE, "Game");
		bundle.putString(Param.ITEM_ID, String.valueOf(gameId));
		bundle.putString(Param.ITEM_NAME, gameName);
		FirebaseAnalytics.getInstance(activity).logEvent(Event.SHARE, bundle);
	}

	public static void shareGames(Activity activity, List<Pair<Integer, String>> games, String method) {
		Resources r = activity.getResources();
		StringBuilder text = new StringBuilder(r.getString(R.string.share_games_text));
		text.append("\n").append("\n");
		List<String> gameNames = new ArrayList<>();
		List<String> gameIds = new ArrayList<>();
		for (Pair<Integer, String> game : games) {
			text.append(formatGameLink(game.first, game.second));
			gameNames.add(game.second);
			gameIds.add(String.valueOf(game.first));
		}
		share(activity, r.getString(R.string.share_games_subject), text.toString(), R.string.title_share_games);
		Bundle bundle = new Bundle();
		bundle.putString(Param.METHOD, method);
		bundle.putString(Param.CONTENT_TYPE, "Game");
		bundle.putString(Param.ITEM_ID, StringUtils.formatList(gameIds));
		bundle.putString(Param.ITEM_NAME, StringUtils.formatList(gameNames));
		FirebaseAnalytics.getInstance(activity).logEvent(Event.SHARE, bundle);
	}

	public static String formatGameLink(int id, String name) {
		return String.format("%s (%s)\n", name, createBggUri(BOARDGAME_PATH, id));
	}

	public static void logQuickPlay(Context context, int gameId, String gameName) {
		Play play = new Play(gameId, gameName);
		play.setCurrentDate();
		play.updateTimestamp = System.currentTimeMillis();
		new PlayPersister(context).save(play, BggContract.INVALID_ID, false);
		SyncService.sync(context, SyncService.FLAG_SYNC_PLAYS_UPLOAD);
	}

	public static void linkBgg(Context context, int gameId) {
		if (gameId == BggContract.INVALID_ID) return;
		linkToBgg(context, BOARDGAME_PATH, gameId);
	}

	public static void linkToBgg(Context context, String path, int id) {
		link(context, createBggUri(path, id));
	}

	public static void link(Context context, String url) {
		link(context, Uri.parse(url));
	}

	private static void link(Context context, Uri link) {
		final Intent intent = new Intent(Intent.ACTION_VIEW, link);
		if (isIntentAvailable(context, intent)) {
			context.startActivity(intent);
			Bundle bundle = new Bundle();
			bundle.putString("Uri", link.toString());
			FirebaseAnalytics.getInstance(context).logEvent("Link", bundle);
		} else {
			String message = "Can't figure out how to launch " + link;
			Timber.w(message);
			Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
		}
	}

	public static Uri createBggUri(String path, int id) {
		return BGG_URI.buildUpon().appendPath(path).appendPath(String.valueOf(id)).build();
	}

	private static boolean isIntentAvailable(Context context, Intent intent) {
		final PackageManager packageManager = context.getPackageManager();
		List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}
}