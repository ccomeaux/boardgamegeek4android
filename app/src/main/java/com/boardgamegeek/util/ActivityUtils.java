package com.boardgamegeek.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.ShareCompat;
import android.text.TextUtils;
import android.util.Pair;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.events.PlaySelectedEvent;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.persister.PlayPersister;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.ui.BuddyActivity;
import com.boardgamegeek.ui.ImageActivity;
import com.boardgamegeek.ui.LocationActivity;
import com.boardgamegeek.ui.LogPlayActivity;
import com.boardgamegeek.ui.PlayActivity;
import com.boardgamegeek.util.fabric.PlayManipulationEvent;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.crashlytics.android.answers.ShareEvent;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class ActivityUtils {
	public static final String KEY_INTERNAL_ID = "_ID";
	public static final String KEY_TITLE = "TITLE";
	public static final String KEY_GAME_ID = "GAME_ID";
	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_COLLECTION_ID = "COLLECTION_ID";
	public static final String KEY_COLLECTION_NAME = "COLLECTION_NAME";
	public static final String KEY_IMAGE_URL = "IMAGE_URL";
	public static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";
	public static final String KEY_END_PLAY = "END_PLAY";
	public static final String KEY_REMATCH = "REMATCH";
	public static final String KEY_CUSTOM_PLAYER_SORT = "CUSTOM_PLAYER_SORT";
	public static final String KEY_FROM_SHORTCUT = "FROM_SHORTCUT";
	public static final String KEY_QUERY_TOKEN = "QUERY_TOKEN";
	public static final String KEY_SORT = "SORT";
	public static final String KEY_LOCATION = "LOCATION";
	public static final String KEY_BUDDY_NAME = "BUDDY_NAME";
	public static final String KEY_PLAYER_NAME = "PLAYER_NAME";
	public static final String KEY_USER = "USER";
	public static final String KEY_USERNAME = "USERNAME";
	public static final String KEY_FORUM_ID = "FORUM_ID";
	public static final String KEY_FORUM_TITLE = "FORUM_TITLE";
	public static final String KEY_THREAD_ID = "THREAD_ID";
	public static final String KEY_THREAD_SUBJECT = "THREAD_SUBJECT";
	public static final String KEY_ARTICLE_ID = "ARTICLE_ID";
	public static final String KEY_POST_DATE = "POST_DATE";
	public static final String KEY_EDIT_DATE = "EDIT_DATE";
	public static final String KEY_EDIT_COUNT = "EDIT_COUNT";
	public static final String KEY_BODY = "BODY";
	public static final String KEY_LINK = "LINK";
	public static final String KEY_LOCATION_NAME = "LOCATION_NAME";
	public static final String KEY_TYPE = "TYPE";
	public static final String KEY_GEEK_LIST = "GEEK_LIST";
	public static final String KEY_ID = "GEEK_LIST_ID";
	public static final String KEY_ORDER = "GEEK_LIST_ORDER";
	public static final String KEY_NAME = "GEEK_LIST_NAME";
	public static final String KEY_THUMBS = "GEEK_LIST_THUMBS";
	public static final String KEY_IMAGE_ID = "GEEK_LIST_IMAGE_ID";
	public static final String KEY_POSTED_DATE = "GEEK_LIST_POSTED_DATE";
	public static final String KEY_EDITED_DATE = "GEEK_LIST_EDITED_DATE";
	public static final String KEY_OBJECT_ID = "GEEK_LIST_OBJECT_ID";
	public static final String KEY_OBJECT_URL = "GEEK_LIST_OBJECT_URL";
	public static final String KEY_IS_BOARD_GAME = "GEEK_LIST_IS_BOARD_GAME";
	public static final String KEY_HEADER_COLOR = "HEADER_COLOR";
	public static final String KEY_ICON_COLOR = "ICON_COLOR";
	public static final String LINK_AMAZON_COM = "www.amazon.com";
	public static final String LINK_AMAZON_UK = "www.amazon.co.uk";
	public static final String LINK_AMAZON_DE = "www.amazon.de";
	private static final String BOARDGAME_PATH = "boardgame";
	private static final Uri BGG_URI = Uri.parse("https://www.boardgamegeek.com/");

	public static void launchGame(Context context, int gameId, String gameName) {
		final Intent intent = createGameIntent(gameId, gameName);
		context.startActivity(intent);
	}

	public static void navigateUpToGame(Context context, int gameId, String gameName) {
		final Intent intent = createGameIntent(gameId, gameName);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		context.startActivity(intent);
	}

	public static Intent createGameIntent(int gameId, String gameName) {
		final Uri gameUri = Games.buildGameUri(gameId);
		final Intent intent = new Intent(Intent.ACTION_VIEW, gameUri);
		intent.putExtra(ActivityUtils.KEY_GAME_NAME, gameName);
		return intent;
	}

	public static void startBuddyActivity(Context context, String username, String playerName) {
		context.startActivity(createBuddyIntent(context, username, playerName));
	}

	public static void navigateUpToBuddy(Context context, String buddyName) {
		navigateUpToBuddy(context, buddyName, null);
	}

	public static void navigateUpToBuddy(Context context, String buddyName, String playerName) {
		Intent intent = createBuddyIntent(context, buddyName, playerName);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		context.startActivity(intent);
	}

	private static Intent createBuddyIntent(Context context, String username, String playerName) {
		Intent intent = new Intent(context, BuddyActivity.class);
		intent.putExtra(ActivityUtils.KEY_BUDDY_NAME, username);
		intent.putExtra(ActivityUtils.KEY_PLAYER_NAME, playerName);
		return intent;
	}

	@NonNull
	public static Intent createLocationIntent(Context context, String locationName) {
		Intent intent = new Intent(context, LocationActivity.class);
		intent.putExtra(ActivityUtils.KEY_LOCATION_NAME, locationName);
		return intent;
	}

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

	private static String formatGameLink(int id, String name) {
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

	public static void startPlayActivity(Context context, PlaySelectedEvent event) {
		Intent intent = createPlayIntent(context, event.getInternalId(), event.getGameId(), event.getGameName(), event.getThumbnailUrl(), event.getImageUrl());
		context.startActivity(intent);
	}

	public static Intent createPlayIntent(Context context, long internalId, int gameId, String gameName, String thumbnailUrl, String imageUrl) {
		Intent intent = new Intent(context, PlayActivity.class);
		intent.putExtra(KEY_ID, internalId);
		intent.putExtra(KEY_GAME_ID, gameId);
		intent.putExtra(KEY_GAME_NAME, gameName);
		intent.putExtra(KEY_THUMBNAIL_URL, thumbnailUrl);
		intent.putExtra(KEY_IMAGE_URL, imageUrl);
		return intent;
	}

	public static void editPlay(Context context, long internalId, int gameId, String gameName, String thumbnailUrl, String imageUrl) {
		PlayManipulationEvent.log("Edit", gameName);
		Intent intent = createEditPlayIntent(context, internalId, gameId, gameName, thumbnailUrl, imageUrl);
		context.startActivity(intent);
	}

	public static void endPlay(Context context, long internalId, int gameId, String gameName, String thumbnailUrl, String imageUrl) {
		Intent intent = createEditPlayIntent(context, internalId, gameId, gameName, thumbnailUrl, imageUrl);
		intent.putExtra(KEY_END_PLAY, true);
		context.startActivity(intent);
	}

	public static void rematch(Context context, long internalId, int gameId, String gameName, String thumbnailUrl, String imageUrl) {
		Intent intent = createRematchIntent(context, internalId, gameId, gameName, thumbnailUrl, imageUrl);
		context.startActivity(intent);
	}

	public static Intent createRematchIntent(Context context, long internalId, int gameId, String gameName, String thumbnailUrl, String imageUrl) {
		Intent intent = createEditPlayIntent(context, internalId, gameId, gameName, thumbnailUrl, imageUrl);
		intent.putExtra(KEY_REMATCH, true);
		return intent;
	}

	public static void logPlay(Context context, int gameId, String gameName, String thumbnailUrl, String imageUrl, boolean customPlayerSort) {
		Intent intent = createEditPlayIntent(context, gameId, gameName, thumbnailUrl, imageUrl);
		intent.putExtra(KEY_CUSTOM_PLAYER_SORT, customPlayerSort);
		context.startActivity(intent);
	}

	public static Intent createEditPlayIntent(Context context, int gameId, String gameName, String thumbnailUrl, String imageUrl) {
		return createEditPlayIntent(context, BggContract.INVALID_ID, gameId, gameName, thumbnailUrl, imageUrl);
	}

	public static Intent createEditPlayIntent(Context context, long internalId, int gameId, String gameName, String thumbnailUrl, String imageUrl) {
		Intent intent = new Intent(context, LogPlayActivity.class);
		intent.putExtra(KEY_ID, internalId);
		intent.putExtra(KEY_GAME_ID, gameId);
		intent.putExtra(KEY_GAME_NAME, gameName);
		intent.putExtra(KEY_THUMBNAIL_URL, thumbnailUrl);
		intent.putExtra(KEY_IMAGE_URL, imageUrl);
		return intent;
	}

	public static void startImageActivity(Context context, String imageUrl) {
		if (TextUtils.isEmpty(imageUrl)) {
			Timber.w("Missing the required image URL.");
			return;
		}
		Intent intent = new Intent(context, ImageActivity.class);
		intent.putExtra(KEY_IMAGE_URL, imageUrl);
		context.startActivity(intent);
	}

	public static void logQuickPlay(Context context, int gameId, String gameName) {
		Play play = new Play(gameId, gameName);
		play.setCurrentDate();
		play.updateTimestamp = System.currentTimeMillis();
		new PlayPersister(context).save(play);
		SyncService.sync(context, SyncService.FLAG_SYNC_PLAYS_UPLOAD);
	}

	public static void linkBgg(Context context, int gameId) {
		if (gameId == BggContract.INVALID_ID) {
			return;
		}
		linkToBgg(context, BOARDGAME_PATH, gameId);
	}

	public static void linkBgPrices(Context context, String gameName) {
		if (TextUtils.isEmpty(gameName)) {
			return;
		}
		link(context, "http://boardgameprices.com/compare-prices-for?q=" + HttpUtils.encode(gameName));
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