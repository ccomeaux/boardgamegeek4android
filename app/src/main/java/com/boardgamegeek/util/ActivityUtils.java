package com.boardgamegeek.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.StringRes;
import android.support.v4.app.ShareCompat;
import android.text.TextUtils;
import android.util.Pair;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.persister.PlayPersister;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.service.SyncService;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.crashlytics.android.answers.ShareEvent;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class ActivityUtils {
	public static final String LINK_AMAZON_COM = "www.amazon.com";
	public static final String LINK_AMAZON_UK = "www.amazon.co.uk";
	public static final String LINK_AMAZON_DE = "www.amazon.de";
	private static final String BOARDGAME_PATH = "boardgame";
	private static final Uri BGG_URI = Uri.parse("https://www.boardgamegeek.com/");

	public static void share(Activity activity, String subject, String text, @StringRes int titleResId) {
		Intent intent = ShareCompat.IntentBuilder.from(activity)
			.setType("text/plain")
			.setSubject(subject.trim())
			.setText(text.trim())
			.setChooserTitle(titleResId)
			.createChooserIntent();
		activity.startActivity(intent);
	}

	public static void shareGame(Activity activity, int gameId, String gameName, String method) {
		Resources r = activity.getResources();
		String subject = String.format(r.getString(R.string.share_game_subject), gameName);
		String text = r.getString(R.string.share_game_text) + "\n\n" + formatGameLink(gameId, gameName);
		share(activity, subject, text, R.string.title_share_game);
		Answers.getInstance().logShare(new ShareEvent()
			.putMethod(method)
			.putContentType("Game")
			.putContentName(gameName)
			.putContentId(String.valueOf(gameId)));
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
			gameNames.add(String.valueOf(game.first));
		}
		share(activity, r.getString(R.string.share_games_subject), text.toString(), R.string.title_share_games);
		Answers.getInstance().logShare(new ShareEvent()
			.putMethod(method)
			.putContentType("Games")
			.putContentName(StringUtils.formatList(gameNames))
			.putContentId(String.valueOf(StringUtils.formatList(gameIds))));
	}

	public static String formatGameLink(int id, String name) {
		return String.format("%s (%s)\n", name, createBggUri(BOARDGAME_PATH, id));
	}

	public static void shareGeekList(Activity activity, int id, String title) {
		String description = String.format(activity.getString(R.string.share_geeklist_text), title);
		Uri uri = ActivityUtils.createBggUri("geeklist", id);
		ActivityUtils.share(activity, activity.getString(R.string.share_geeklist_subject), description + "\n\n" + uri, R.string.title_share);
		Answers.getInstance().logShare(new ShareEvent()
			.putContentType("GeekList")
			.putContentName(title)
			.putContentId(String.valueOf(id)));
	}

	public static void logQuickPlay(Context context, int gameId, String gameName) {
		Play play = new Play(gameId, gameName);
		play.setCurrentDate();
		play.updateTimestamp = System.currentTimeMillis();
		new PlayPersister(context).save(play, BggContract.INVALID_ID, false);
		SyncService.sync(context, SyncService.FLAG_SYNC_PLAYS_UPLOAD);
	}

	public static void linkBgg(Context context, int gameId) {
		if (gameId == BggContract.INVALID_ID) {
			return;
		}
		linkToBgg(context, BOARDGAME_PATH, gameId);
	}

	public static void linkBgPrices(Context context, String gameName) {
		if (TextUtils.isEmpty(gameName)) return;
		link(context, "http://boardgameprices.com/compare-prices-for?q=" + HttpUtils.encode(gameName));
	}

	public static void linkBgPricesUk(Context context, String gameName) {
		if (TextUtils.isEmpty(gameName)) return;
		link(context, "https://boardgameprices.co.uk/item/search?search=" + HttpUtils.encode(gameName));
	}

	public static void linkAmazon(Context context, String gameName, String domain) {
		if (TextUtils.isEmpty(gameName)) {
			return;
		}
		link(context, String.format("http://%s/gp/aw/s/?i=toys&keywords=%s", domain, HttpUtils.encode(gameName)));
	}

	public static void linkEbay(Context context, String gameName) {
		if (TextUtils.isEmpty(gameName)) {
			return;
		}
		link(context, "http://m.ebay.com/sch/i.html?_sacat=233&cnm=Games&_nkw=" + HttpUtils.encode(gameName));
	}

	public static void linkToBgg(Context context, String path) {
		link(context, createBggUri(path));
		Answers.getInstance().logCustom(new CustomEvent("Link")
			.putCustomAttribute("Path", path));
	}

	public static void linkToBgg(Context context, String path, int id) {
		link(context, createBggUri(path, id));
		Answers.getInstance().logCustom(new CustomEvent("Link")
			.putCustomAttribute("Path", path));
	}

	public static void link(Context context, String url) {
		link(context, Uri.parse(url));
		Answers.getInstance().logCustom(new CustomEvent("Link")
			.putCustomAttribute("Url", url));
	}

	private static void link(Context context, Uri link) {
		final Intent intent = new Intent(Intent.ACTION_VIEW, link);
		if (isIntentAvailable(context, intent)) {
			context.startActivity(intent);
		} else {
			String message = "Can't figure out how to launch " + link;
			Timber.w(message);
			Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
		}
	}

	public static Uri createBggUri(String path) {
		return BGG_URI.buildUpon().appendPath(path).build();
	}

	public static Uri createBggUri(String path, int id) {
		return BGG_URI.buildUpon().appendPath(path).appendPath(String.valueOf(id)).build();
	}

	public static boolean isIntentAvailable(Context context, Intent intent) {
		final PackageManager packageManager = context.getPackageManager();
		List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}
}