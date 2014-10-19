package com.boardgamegeek.util;

import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

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
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.boardgamegeek.R;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.persister.PlayPersister;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.ui.GameActivity;
import com.boardgamegeek.ui.LogPlayActivity;
import com.boardgamegeek.ui.PlayActivity;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class ActivityUtils {
	private static final String TAG = makeLogTag(ActivityUtils.class);

	public final static String KEY_TITLE = "TITLE";
	public final static String KEY_GAME_ID = "GAME_ID";
	public final static String KEY_GAME_NAME = "GAME_NAME";
	public final static String KEY_QUERY_TOKEN = "QUERY_TOKEN";
	public static final String IMAGE_URL_PREFIX = "http://cf.geekdo-images.com/images/pic";

	private static final String BGG_URL_BASE = "http://www.boardgamegeek.com/";
	private static final Uri BGG_URI = Uri.parse(BGG_URL_BASE);
	private static final String BOARDGAME_URL_PREFIX = BGG_URL_BASE + "boardgame/";

	private static final String SUFFIX_SQUARE = "_sq";
	private static final String SUFFIX_SMALL = "_t";
	private static final String SUFFIX_MEDIUM = "_md";
	private static final String SUFFIX_LARGE = "_lg";

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
		builder = addAlertIcon(builder);
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
	public static AlertDialog.Builder addAlertIcon(AlertDialog.Builder builder) {
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

	public static void logPlayAgain(Context context, int playId, int gameId, String gameName, String thumbnailUrl,
		String imageUrl) {
		Intent intent = createEditPlayIntent(context, playId, gameId, gameName, thumbnailUrl, imageUrl);
		intent.putExtra(LogPlayActivity.KEY_PLAY_AGAIN, true);
		context.startActivity(intent);
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
		PlayPersister.save(context, play);
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
		intent.putExtra(GameActivity.KEY_GAME_NAME, gameName);
		intent.putExtra(GameActivity.KEY_FROM_SHORTCUT, true);

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
			mThumbnailUrl = thumbnailUrl;
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
						bitmap = Picasso.with(mContext).load(mThumbnailUrl).resize(128, 128).centerCrop().get();
					} catch (IOException e) {
						LOGE(TAG, "Error downloading the thumbnail.", e);
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
						LOGE(TAG, "Error saving the thumbnail file.", e);
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

	public static void setDoneCancelActionBarView(SherlockFragmentActivity activity, View.OnClickListener listener) {
		activity.getSupportActionBar().setDisplayOptions(
			ActionBar.DISPLAY_SHOW_CUSTOM,
			ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE
				| ActionBar.DISPLAY_SHOW_CUSTOM);
		View actionBarButtons = activity.getLayoutInflater().inflate(R.layout.actionbar_done_cancel,
			new LinearLayout(activity), false);
		View cancelActionView = actionBarButtons.findViewById(R.id.menu_cancel);
		cancelActionView.setOnClickListener(listener);
		View doneActionView = actionBarButtons.findViewById(R.id.menu_done);
		doneActionView.setOnClickListener(listener);
		activity.getSupportActionBar().setCustomView(actionBarButtons);
	}

	public static String createThumbnailJpg(int imageId) {
		return IMAGE_URL_PREFIX + imageId + SUFFIX_SMALL + ".jpg";
	}

	public static String createThumbnailPng(int imageId) {
		return IMAGE_URL_PREFIX + imageId + SUFFIX_SMALL + ".png";
	}

	public static void safelyLoadImage(ImageView imageView, int imageId) {
		Queue<String> imageUrls = new LinkedList<String>();
		String imageUrl = IMAGE_URL_PREFIX + imageId + ".jpg";
		imageUrls.add(appendImageUrl(imageUrl, SUFFIX_MEDIUM));
		imageUrls.add(appendImageUrl(imageUrl, SUFFIX_SMALL));
		imageUrls.add(imageUrl);
		imageUrl = IMAGE_URL_PREFIX + imageId + ".png";
		imageUrls.add(appendImageUrl(imageUrl, SUFFIX_MEDIUM));
		imageUrls.add(appendImageUrl(imageUrl, SUFFIX_SMALL));
		imageUrls.add(imageUrl);
		safelyLoadImage(imageView, imageUrls);
	}

	public static void safelyLoadImage(ImageView imageView, String imageUrl) {
		Queue<String> imageUrls = new LinkedList<String>();
		imageUrls.add(appendImageUrl(imageUrl, SUFFIX_MEDIUM));
		imageUrls.add(appendImageUrl(imageUrl, SUFFIX_SMALL));
		imageUrls.add(imageUrl);
		safelyLoadImage(imageView, imageUrls);
	}

	public static void safelyLoadImage(final ImageView imageView, final Queue<String> imageUrls) {
		String imageUrl = imageUrls.poll();
		if (TextUtils.isEmpty(imageUrl)) {
			return;
		}

		final Context context = imageView.getContext();
		Picasso.with(context).load(imageUrl).fit().centerCrop().into(imageView, new Callback() {
			@Override
			public void onSuccess() {
			}

			@Override
			public void onError() {
				safelyLoadImage(imageView, imageUrls);
			}
		});
	}

	private static String appendImageUrl(String imageUrl, String suffix) {
		if (TextUtils.isEmpty(imageUrl)) {
			return "";
		}
		if (TextUtils.isEmpty(suffix)) {
			return imageUrl;
		}
		int dot = imageUrl.lastIndexOf('.');
		if (dot == -1) {
			return imageUrl + suffix;
		} else {
			return imageUrl.substring(0, dot) + suffix + imageUrl.substring(dot, imageUrl.length());
		}
	}
}
