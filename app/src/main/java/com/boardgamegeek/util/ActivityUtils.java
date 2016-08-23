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
import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.persister.PlayPersister;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.ui.BuddyActivity;
import com.boardgamegeek.ui.ImageActivity;
import com.boardgamegeek.ui.LocationActivity;
import com.boardgamegeek.ui.LogPlayActivity;
import com.boardgamegeek.ui.PlayActivity;
import com.boardgamegeek.ui.PlayerPlaysActivity;

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
	public static final String KEY_CUSTOM_PLAYER_SORT = "CUSTOM_PLAYER_SORT";
	public static final String KEY_FROM_SHORTCUT = "FROM_SHORTCUT";
	public static final String KEY_QUERY_TOKEN = "QUERY_TOKEN";
	public static final String KEY_SORT = "SORT";
	public static final String KEY_BUDDY_NAME = "BUDDY_NAME";
	public static final String KEY_PLAYER_NAME = "PLAYER_NAME";
	public static final String KEY_USER = "USER";
	public static final String KEY_USERNAME = "USERNAME";
	public static final String KEY_FORUM_ID = "FORUM_ID";
	public static final String KEY_FORUM_TITLE = "FORUM_TITLE";
	public static final String KEY_THREAD_ID = "THREAD_ID";
	public static final String KEY_THREAD_SUBJECT = "THREAD_SUBJECT";
	public static final String KEY_POST_DATE = "POST_DATE";
	public static final String KEY_EDIT_DATE = "EDIT_DATE";
	public static final String KEY_EDIT_COUNT = "EDIT_COUNT";
	public static final String KEY_TEXT = "TEXT";
	public static final String KEY_BODY = "BODY";
	public static final String KEY_LINK = "LINK";
	public static final String KEY_LOCATION_NAME = "LOCATION_NAME";
	public static final String KEY_TYPE = "TYPE";
	public static final String KEY_GEEKLIST = "GEEKLIST";
	public static final String KEY_ID = "GEEKLIST_ID";
	public static final String KEY_ORDER = "GEEKLIST_ORDER";
	public static final String KEY_NAME = "GEEKLIST_NAME";
	public static final String KEY_THUMBS = "GEEKLIST_THUMBS";
	public static final String KEY_IMAGE_ID = "GEEKLIST_IMAGE_ID";
	public static final String KEY_POSTED_DATE = "GEEKLIST_POSTED_DATE";
	public static final String KEY_EDITED_DATE = "GEEKLIST_EDITED_DATE";
	public static final String KEY_OBJECT_ID = "GEEKLIST_OBJECT_ID";
	public static final String KEY_OBJECT_URL = "GEEKLIST_OBJECT_URL";
	public static final String KEY_IS_BOARD_GAME = "GEEKLIST_IS_BOARD_GAME";
	public static final String KEY_HEADER_COLOR = "HEADER_COLOR";
	private static final String BGG_URL_BASE = "https://www.boardgamegeek.com/";
	private static final Uri BGG_URI = Uri.parse(BGG_URL_BASE);
	private static final String BOARDGAME_URL_PREFIX = BGG_URL_BASE + "boardgame/";

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

	public static void startPlayerPlaysActivity(Context context, String name, String username) {
		Intent intent = new Intent(context, PlayerPlaysActivity.class);
		intent.putExtra(PlayerPlaysActivity.KEY_PLAYER_NAME, name);
		intent.putExtra(PlayerPlaysActivity.KEY_PLAYER_USERNAME, username);
		context.startActivity(intent);
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

	public static void shareGame(Activity activity, int gameId, String gameName) {
		Resources r = activity.getResources();
		String subject = String.format(r.getString(R.string.share_game_subject), gameName);
		String text = r.getString(R.string.share_game_text) + "\n\n" + formatGameLink(gameId, gameName);
		share(activity, subject, text, R.string.title_share_game);
	}

	public static void shareGames(Activity activity, List<Pair<Integer, String>> games) {
		Resources r = activity.getResources();
		StringBuilder text = new StringBuilder(r.getString(R.string.share_games_text));
		text.append("\n").append("\n");
		for (Pair<Integer, String> game : games) {
			text.append(formatGameLink(game.first, game.second));
		}
		share(activity, r.getString(R.string.share_games_subject), text.toString(), R.string.title_share_games);
	}

	private static String formatGameLink(int id, String name) {
		return name + " (" + BOARDGAME_URL_PREFIX + id + ")\n";
	}

	public static void startPlayActivity(Context context, int playId, int gameId, String gameName, String thumbnailUrl, String imageUrl) {
		Intent intent = createPlayIntent(context, playId, gameId, gameName, thumbnailUrl, imageUrl);
		context.startActivity(intent);
	}

	public static Intent createPlayIntent(Context context, int playId, int gameId, String gameName, String thumbnailUrl, String imageUrl) {
		Intent intent = new Intent(context, PlayActivity.class);
		intent.putExtra(PlayActivity.KEY_PLAY_ID, playId);
		intent.putExtra(PlayActivity.KEY_GAME_ID, gameId);
		intent.putExtra(PlayActivity.KEY_GAME_NAME, gameName);
		intent.putExtra(PlayActivity.KEY_THUMBNAIL_URL, thumbnailUrl);
		intent.putExtra(PlayActivity.KEY_IMAGE_URL, imageUrl);
		return intent;
	}

	public static void editPlay(Context context, int playId, int gameId, String gameName, String thumbnailUrl, String imageUrl) {
		Intent intent = createEditPlayIntent(context, playId, gameId, gameName, thumbnailUrl, imageUrl);
		context.startActivity(intent);
	}

	public static void endPlay(Context context, int playId, int gameId, String gameName, String thumbnailUrl, String imageUrl) {
		Intent intent = createEditPlayIntent(context, playId, gameId, gameName, thumbnailUrl, imageUrl);
		intent.putExtra(LogPlayActivity.KEY_END_PLAY, true);
		context.startActivity(intent);
	}

	public static void rematch(Context context, int playId, int gameId, String gameName, String thumbnailUrl, String imageUrl) {
		Intent intent = createRematchIntent(context, playId, gameId, gameName, thumbnailUrl, imageUrl);
		context.startActivity(intent);
	}

	public static Intent createRematchIntent(Context context, int playId, int gameId, String gameName, String thumbnailUrl, String imageUrl) {
		Intent intent = createEditPlayIntent(context, playId, gameId, gameName, thumbnailUrl, imageUrl);
		intent.putExtra(LogPlayActivity.KEY_REMATCH, true);
		return intent;
	}

	public static void logPlay(Context context, int gameId, String gameName, String thumbnailUrl, String imageUrl, boolean customPlayerSort) {
		Intent intent = createEditPlayIntent(context, 0, gameId, gameName, thumbnailUrl, imageUrl);
		intent.putExtra(LogPlayActivity.KEY_CUSTOM_PLAYER_SORT, customPlayerSort);
		context.startActivity(intent);
	}

	public static Intent createEditPlayIntent(Context context, int playId, int gameId, String gameName, String thumbnailUrl, String imageUrl) {
		Intent intent = new Intent(context, LogPlayActivity.class);
		intent.putExtra(LogPlayActivity.KEY_PLAY_ID, playId);
		intent.putExtra(LogPlayActivity.KEY_GAME_ID, gameId);
		intent.putExtra(LogPlayActivity.KEY_GAME_NAME, gameName);
		intent.putExtra(LogPlayActivity.KEY_THUMBNAIL_URL, thumbnailUrl);
		intent.putExtra(LogPlayActivity.KEY_IMAGE_URL, imageUrl);
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
		play.syncStatus = Play.SYNC_STATUS_PENDING_UPDATE;
		new PlayPersister(context).save(play);
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
		link(context, "http://boardgameprices.com/compare-prices-for-" + HttpUtils.encode(gameName));
	}

	public static void linkAmazon(Context context, String gameName) {
		if (TextUtils.isEmpty(gameName)) {
			return;
		}
		link(context, "http://www.amazon.com/gp/aw/s/?i=toys-and-games&keywords=" + HttpUtils.encode(gameName));
	}

	public static void linkEbay(Context context, String gameName) {
		if (TextUtils.isEmpty(gameName)) {
			return;
		}
		link(context, "http://m.ebay.com/sch/i.html?_sacat=233&cnm=Games&_nkw=" + HttpUtils.encode(gameName));
	}

	public static void linkToBgg(Context context, String path) {
		link(context, createBggUri(path));
	}

	public static void link(Context context, String link) {
		link(context, Uri.parse(link));
	}

	public static void link(Context context, Uri link) {
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