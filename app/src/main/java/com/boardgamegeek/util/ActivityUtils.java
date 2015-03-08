package com.boardgamegeek.util;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.persister.PlayPersister;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.ui.LogPlayActivity;
import com.boardgamegeek.ui.PlayActivity;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import timber.log.Timber;

public class ActivityUtils {
	public final static String KEY_TITLE = "TITLE";
	public final static String KEY_GAME_ID = "GAME_ID";
	public final static String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_COLLECTION_ID = "COLLECTION_ID";
	public static final String KEY_COLLECTION_NAME = "COLLECTION_NAME";
	public final static String KEY_IMAGE_URL = "IMAGE_URL";
	public static final String KEY_FROM_SHORTCUT = "FROM_SHORTCUT";
	public final static String KEY_QUERY_TOKEN = "QUERY_TOKEN";
	public final static String KEY_SORT = "SORT";
	public static final String KEY_BUDDY_NAME = "BUDDY_NAME";
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

	private static Intent createGameIntent(int gameId, String gameName) {
		final Uri gameUri = Games.buildGameUri(gameId);
		final Intent intent = new Intent(Intent.ACTION_VIEW, gameUri);
		intent.putExtra(ActivityUtils.KEY_GAME_NAME, gameName);
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

	public static void startPlayActivity(Context context, int playId, int gameId, String gameName, String thumbnailUrl,
										 String imageUrl) {
		Intent intent = createPlayIntent(context, playId, gameId, gameName, thumbnailUrl, imageUrl);
		context.startActivity(intent);
	}

	public static Intent createPlayIntent(Context context, int playId, int gameId, String gameName,
										  String thumbnailUrl, String imageUrl) {
		Intent intent = new Intent(context, PlayActivity.class);
		intent.putExtra(PlayActivity.KEY_PLAY_ID, playId);
		intent.putExtra(PlayActivity.KEY_GAME_ID, gameId);
		intent.putExtra(PlayActivity.KEY_GAME_NAME, gameName);
		intent.putExtra(PlayActivity.KEY_THUMBNAIL_URL, thumbnailUrl);
		intent.putExtra(PlayActivity.KEY_IMAGE_URL, imageUrl);
		return intent;
	}

	public static void editPlay(Context context, int playId, int gameId, String gameName, String thumbnailUrl,
								String imageUrl) {
		Intent intent = createEditPlayIntent(context, playId, gameId, gameName, thumbnailUrl, imageUrl);
		context.startActivity(intent);
	}

	public static void endPlay(Context context, int playId, int gameId, String gameName, String thumbnailUrl,
							   String imageUrl) {
		Intent intent = createEditPlayIntent(context, playId, gameId, gameName, thumbnailUrl, imageUrl);
		intent.putExtra(LogPlayActivity.KEY_END_PLAY, true);
		context.startActivity(intent);
	}

	public static void rematch(Context context, int playId, int gameId, String gameName, String thumbnailUrl,
							   String imageUrl) {
		Intent intent = createRematchIntent(context, playId, gameId, gameName, thumbnailUrl, imageUrl);
		context.startActivity(intent);
	}

	public static Intent createRematchIntent(Context context, int playId, int gameId, String gameName, String thumbnailUrl, String imageUrl) {
		Intent intent = createEditPlayIntent(context, playId, gameId, gameName, thumbnailUrl, imageUrl);
		intent.putExtra(LogPlayActivity.KEY_REMATCH, true);
		return intent;
	}

	public static void logPlay(Context context, int gameId, String gameName, String thumbnailUrl, String imageUrl,
							   boolean customPlayerSort) {
		Intent intent = createEditPlayIntent(context, 0, gameId, gameName, thumbnailUrl, imageUrl);
		intent.putExtra(LogPlayActivity.KEY_CUSTOM_PLAYER_SORT, customPlayerSort);
		context.startActivity(intent);
	}

	public static Intent createEditPlayIntent(Context context, int playId, int gameId, String gameName,
											  String thumbnailUrl, String imageUrl) {
		Intent intent = new Intent(context, LogPlayActivity.class);
		intent.putExtra(LogPlayActivity.KEY_PLAY_ID, playId);
		intent.putExtra(LogPlayActivity.KEY_GAME_ID, gameId);
		intent.putExtra(LogPlayActivity.KEY_GAME_NAME, gameName);
		intent.putExtra(LogPlayActivity.KEY_THUMBNAIL_URL, thumbnailUrl);
		intent.putExtra(LogPlayActivity.KEY_IMAGE_URL, imageUrl);
		return intent;
	}

	public static void logQuickPlay(Context context, int gameId, String gameName) {
		Play play = new Play(gameId, gameName);
		play.setCurrentDate();
		play.syncStatus = Play.SYNC_STATUS_PENDING_UPDATE;
		new PlayPersister(context).save(context, play);
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

	public static void link(Context context, Uri link) {
		context.startActivity(new Intent(Intent.ACTION_VIEW, link));
	}

	public static void link(Context context, String link) {
		context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
	}

	public static void linkToBgg(Context context, String path) {
		context.startActivity(new Intent(Intent.ACTION_VIEW, createBggUri(path)));
	}

	public static Uri createBggUri(String path) {
		return BGG_URI.buildUpon().appendPath(path).build();
	}

	public static Uri createBggUri(String path, int id) {
		return BGG_URI.buildUpon().appendPath(path).appendPath(String.valueOf(id)).build();
	}

	public static void setActionBarText(Menu menu, int id, String text) {
		setActionBarText(menu, id, text, null);
	}

	public static void setActionBarText(Menu menu, int id, String text1, String text2) {
		MenuItem item = menu.findItem(id);
		if (item != null) {
			View actionView = MenuItemCompat.getActionView(item);
			if (actionView != null) {
				TextView tv1 = (TextView) actionView.findViewById(android.R.id.text1);
				if (tv1 != null) {
					tv1.setText(text1);
				}
				TextView tv2 = (TextView) actionView.findViewById(android.R.id.text2);
				if (tv2 != null) {
					tv2.setText(text2);
				}
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

	public static Intent createGameShortcut(Context context, int gameId, String gameName, String thumbnailUrl) {
		Intent shortcut = createGameShortcut(context, gameId, gameName);
		if (!TextUtils.isEmpty(thumbnailUrl)) {
			File file = new File(context.getExternalFilesDir(BggContract.PATH_THUMBNAILS),
				FileUtils.getFileNameFromUrl(thumbnailUrl));
			if (file.exists()) {
				shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON, BitmapFactory.decodeFile(file.getAbsolutePath()));
			}
		}
		return shortcut;
	}

	public static void sendGameShortcut(Context context, int gameId, String gameName, String thumbnailUrl) {
		new shortcutTask(context, gameId, gameName, thumbnailUrl).execute();
	}

	private static Intent createGameShortcut(Context context, int gameId, String gameName) {
		Intent intent = new Intent(Intent.ACTION_VIEW, Games.buildGameUri(gameId));
		intent.putExtra(ActivityUtils.KEY_GAME_NAME, gameName);
		intent.putExtra(ActivityUtils.KEY_FROM_SHORTCUT, true);

		Intent shortcut = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, gameName);
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
			Intent.ShortcutIconResource.fromContext(context, R.drawable.ic_launcher));

		return shortcut;
	}

	private static class shortcutTask extends AsyncTask<Void, Void, Void> {
		private Context mContext;
		private String mThumbnailUrl;
		private Intent mShortcut;

		public shortcutTask(Context context, int gameId, String gameName, String thumbnailUrl) {
			mContext = context;
			mShortcut = createGameShortcut(context, gameId, gameName);
			mThumbnailUrl = HttpUtils.ensureScheme(thumbnailUrl);
		}

		@Override
		protected Void doInBackground(Void... params) {
			Bitmap bitmap = null;
			if (!TextUtils.isEmpty(mThumbnailUrl)) {
				File file = new File(mContext.getExternalFilesDir(BggContract.PATH_THUMBNAILS),
					FileUtils.getFileNameFromUrl(mThumbnailUrl));
				if (file.exists()) {
					bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
				} else {
					try {
						bitmap = Picasso.with(mContext).load(HttpUtils.ensureScheme(mThumbnailUrl)).resize(128, 128)
							.centerCrop().get();
					} catch (IOException e) {
						Timber.e("Error downloading the thumbnail.", e);
					}
					try {
						if (bitmap != null) {
							OutputStream out = null;
							try {
								out = new BufferedOutputStream(new FileOutputStream(file));
								bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
							} finally {
								if (out != null) {
									out.close();
								}
							}
						}
					} catch (IOException e) {
						Timber.e("Error saving the thumbnail file.", e);
					}
				}
			}
			if (bitmap != null) {
				mShortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			mContext.sendBroadcast(mShortcut);
			if (VersionUtils.hasJellyBean()) {
				Toast.makeText(mContext, R.string.msg_shortcut_created, Toast.LENGTH_SHORT).show();
			}
		}
	}

	public static void setDoneCancelActionBarView(ActionBarActivity activity, View.OnClickListener listener) {
		Toolbar toolbar = (Toolbar) activity.findViewById(R.id.toolbar_done_cancel);
		if (toolbar == null) {
			return;
		}
		toolbar.setContentInsetsAbsolute(0, 0);
		View cancelActionView = toolbar.findViewById(R.id.menu_cancel);
		cancelActionView.setOnClickListener(listener);
		View doneActionView = toolbar.findViewById(R.id.menu_done);
		doneActionView.setOnClickListener(listener);
		activity.setSupportActionBar(toolbar);
	}


}