package com.boardgamegeek.util;

import java.util.Random;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.ui.HomeActivity;
import com.boardgamegeek.ui.widget.BezelImageView;

public class UIUtils {
	public static final String HELP_GAME_KEY = "help.game";

	public static final int MENU_ITEM_VIEW = Menu.FIRST;
	public static final int MENU_ITEM_LOG_PLAY = Menu.FIRST + 1;
	public static final int MENU_ITEM_QUICK_LOG_PLAY = Menu.FIRST + 2;
	public static final int MENU_ITEM_SHARE = Menu.FIRST + 3;
	public static final int MENU_ITEM_LINK_BGG = Menu.FIRST + 4;

	private Activity mActivity;
	private static Random mRandom;

	public UIUtils(Activity activity) {
		mActivity = activity;
	}

	/**
	 * Sets the current activity to the home screen.
	 */
	public static void goHome(Context context) {
		final Intent intent = new Intent(context, HomeActivity.class);
		context.startActivity(intent);
	}

	/**
	 * Sets the current activity to the home screen, clearing the activity stack.
	 */
	public static void resetToHome(Context context) {
		final Intent intent = new Intent(context, HomeActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		context.startActivity(intent);
	}

	/**
	 * Sets the custom title since the default is hidden.
	 */
	public static void setTitle(Activity activity) {
		setTitle(activity, activity.getTitle());
	}

	/**
	 * Sets the custom title since the default is hidden.
	 */
	public static void setTitle(Activity activity, int titleId) {
		((TextView) activity.findViewById(R.id.title_text)).setText(titleId);
	}

	/**
	 * Sets the custom title since the default is hidden.
	 */
	public static void setTitle(Activity activity, CharSequence title) {
		((TextView) activity.findViewById(R.id.title_text)).setText(title);
	}

	public static void setGameName(Activity activity, CharSequence gameName) {
		((TextView) activity.findViewById(R.id.game_name)).setText(gameName);
	}

	public static void setGameHeader(Activity activity, CharSequence gameName, int gameId) {
		setTitle(activity);
		setGameName(activity, gameName);
		UIUtils u = new UIUtils(activity);
		u.setThumbnail(gameId);
		activity.findViewById(R.id.game_thumbnail).setClickable(false);
		allowTypeToSearch(activity);
	}

	public void setGameName(CharSequence gameName) {
		setGameName(mActivity, gameName);
	}

	public void setThumbnail(int gameId) {
		if (BggApplication.getInstance().getImageLoad() && gameId > 0) {
			new ThumbnailTask(mActivity).execute(Games.buildThumbnailUri(gameId));
		}
	}

	private class ThumbnailTask extends AsyncTask<Uri, Void, Drawable> {
		private Activity mActivity;
		private BezelImageView mThumbnail;
		private View mProgress;

		public ThumbnailTask(Activity activity) {
			if (activity != null) {
				mActivity = activity;
				mThumbnail = (BezelImageView) mActivity.findViewById(R.id.game_thumbnail);
				mProgress = mActivity.findViewById(R.id.thumbnail_progress);
			}
		}

		@Override
		protected void onPreExecute() {
			if (mProgress != null) {
				mProgress.setVisibility(View.VISIBLE);
			}
			if (mThumbnail != null) {
				mThumbnail.setVisibility(View.GONE);
			}
		}

		@Override
		protected Drawable doInBackground(Uri... params) {
			if (mActivity == null || mThumbnail == null) {
				return null;
			}
			return ImageUtils.getGameThumbnail(mActivity, params[0]);
		}

		@Override
		protected void onPostExecute(Drawable result) {
			if (mProgress != null) {
				mProgress.setVisibility(View.GONE);
			}
			if (mThumbnail != null) {
				mThumbnail.setVisibility(View.VISIBLE);
				if (result != null) {
					mThumbnail.setImageDrawable(result);
				} else {
					mThumbnail.setImageResource(R.drawable.noimage);
				}
			}
		}
	}

	public static void allowTypeToSearch(Activity activity) {
		activity.setDefaultKeyMode(Activity.DEFAULT_KEYS_SEARCH_LOCAL);
	}

	public static String[] projectionFromEnums(Enum<?> e[]) {
		String projection[] = new String[e.length];

		int index = 0;
		for (Enum<?> currentEnum : e) {
			projection[index] = currentEnum.toString();
			index++;
		}

		return projection;
	}

	public static void showListMessage(Activity activity, int messageResourceId) {
		showListMessage(activity, messageResourceId, true);
	}

	public static void showListMessage(Activity activity, int messageResourceId, boolean hideProgressBar) {
		TextView tv = (TextView) activity.findViewById(R.id.list_message);
		tv.setText(messageResourceId);

		hideProgressBar(activity, hideProgressBar);
	}

	public static void showListMessage(Activity activity, String message) {
		showListMessage(activity, message, true);
	}

	public static void showListMessage(Activity activity, String message, boolean hideProgressBar) {
		TextView tv = (TextView) activity.findViewById(R.id.list_message);
		tv.setText(message);

		hideProgressBar(activity, hideProgressBar);
	}

	private static void hideProgressBar(Activity activity, boolean hide) {
		ProgressBar pb = (ProgressBar) activity.findViewById(R.id.list_progress);
		if (hide) {
			pb.setVisibility(View.GONE);
		} else {
			pb.setVisibility(View.VISIBLE);
		}
	}

	public static Random getRandom() {
		if (mRandom == null) {
			mRandom = new Random();
		}
		return mRandom;
	}

	public static void createBoardgameContextMenu(ContextMenu menu, ContextMenuInfo menuInfo, String gameName) {
		// Setup the menu header
		menu.setHeaderTitle(gameName);

		menu.add(0, MENU_ITEM_VIEW, 0, R.string.menu_display_game);
		if (!BggApplication.getInstance().getPlayLoggingHideMenu()) {
			menu.add(0, MENU_ITEM_LOG_PLAY, 0, R.string.menu_log_play);
		}
		if (!BggApplication.getInstance().getPlayLoggingHideQuickMenu()) {
			menu.add(0, MENU_ITEM_QUICK_LOG_PLAY, 0, R.string.menu_log_play_quick);
		}
		menu.add(0, MENU_ITEM_SHARE, 0, R.string.menu_share);
		menu.add(0, MENU_ITEM_LINK_BGG, 0, R.string.menu_link_bgg);
	}

	public static void showHelpDialog(Context context, final String key, final int version, int messageId) {
		if (BggApplication.getInstance().showHelp(key, version)) {
			Builder builder = new Builder(context);
			builder.setTitle(R.string.help_title).setCancelable(false).setIcon(android.R.drawable.ic_dialog_info)
				.setMessage(messageId).setPositiveButton(R.string.help_button_close, null)
				.setNegativeButton(R.string.help_button_hide, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						BggApplication.getInstance().updateHelp(key, version);
					}
				});
			builder.create().show();
		}
	}

	private static String KEY_DATA = "_uri";

	/**
	 * Converts an intent into a {@link Bundle} suitable for use as fragment arguments.
	 */
	public static Bundle intentToFragmentArguments(Intent intent) {
		Bundle arguments = new Bundle();
		if (intent == null) {
			return arguments;
		}

		final Uri data = intent.getData();
		if (data != null) {
			arguments.putParcelable(KEY_DATA, data);
		}

		final Bundle extras = intent.getExtras();
		if (extras != null) {
			arguments.putAll(intent.getExtras());
		}

		return arguments;
	}

	/**
	 * Converts a fragment arguments bundle into an intent.
	 */
	public static Intent fragmentArgumentsToIntent(Bundle arguments) {
		Intent intent = new Intent();
		if (arguments == null) {
			return intent;
		}

		final Uri data = arguments.getParcelable(KEY_DATA);
		if (data != null) {
			intent.setData(data);
		}

		intent.putExtras(arguments);
		intent.removeExtra(KEY_DATA);
		return intent;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static void setActivatedCompat(View view, boolean activated) {
		if (VersionUtils.hasHoneycomb()) {
			view.setActivated(activated);
		}
	}

	public static ImageFetcher getImageFetcher(final FragmentActivity activity) {
		// The ImageFetcher takes care of loading remote images into our ImageView
		ImageFetcher fetcher = new ImageFetcher(activity);
		fetcher.addImageCache(activity);
		return fetcher;
	}

	/**
	 * Populate the given {@link TextView} with the requested text, formatting through {@link Html#fromHtml(String)}
	 * when applicable. Also sets {@link TextView#setMovementMethod} so inline links are handled.
	 */
	public static void setTextMaybeHtml(TextView view, String text) {
		if (TextUtils.isEmpty(text)) {
			view.setText("");
			return;
		}
		if (text.contains("<") && text.contains(">")) {
			// remove extra BRs that add unnecessary white space at the end
			while (text.length() > 5 && text.endsWith("<br/>")) {
				text = text.substring(0, text.length() - 5);
			}
			Spanned spanned = Html.fromHtml(text);
			view.setText(spanned);
			view.setMovementMethod(LinkMovementMethod.getInstance());
		} else {
			view.setText(text);
		}
	}
}
