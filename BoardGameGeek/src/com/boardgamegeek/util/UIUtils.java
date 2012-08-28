package com.boardgamegeek.util;

import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.SubMenu;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.ui.HomeActivity;
import com.boardgamegeek.ui.widget.BezelImageView;

public class UIUtils {
	private Activity mActivity;
	private static Random mRandom;

	public static final int MENU_ITEM_VIEW = Menu.FIRST;
	public static final int MENU_ITEM_LOG_PLAY = Menu.FIRST + 1;
	public static final int MENU_ITEM_QUICK_LOG_PLAY = Menu.FIRST + 2;
	public static final int MENU_ITEM_SHARE = Menu.FIRST + 3;
	public static final int MENU_ITEM_LINKS = Menu.FIRST + 4;
	public static final int MENU_ITEM_LINK_BGG = Menu.FIRST + 5;
	public static final int MENU_ITEM_LINK_BG_PRICES = Menu.FIRST + 6;
	public static final int MENU_ITEM_LINK_AMAZON = Menu.FIRST + 7;
	public static final int MENU_ITEM_LINK_EBAY = Menu.FIRST + 8;
	public static final int MENU_ITEM_COMMENTS = Menu.FIRST + 9;

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

	public static AlertDialog createCancelDialog(final Activity activity) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(R.string.are_you_sure_title).setMessage(R.string.are_you_sure_message).setCancelable(false)
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					activity.setResult(Activity.RESULT_CANCELED);
					activity.finish();
				}
			}).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});
		return builder.create();
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
		menu.add(0, MENU_ITEM_COMMENTS, 0, R.string.menu_comments);
		menu.add(0, MENU_ITEM_SHARE, 0, R.string.menu_share);
		SubMenu links = menu.addSubMenu(0, MENU_ITEM_LINKS, 0, R.string.menu_links);
		links.add(0, MENU_ITEM_LINK_BGG, 0, R.string.menu_link_bgg);
		links.add(0, MENU_ITEM_LINK_BG_PRICES, 0, R.string.menu_link_bg_prices);
		links.add(0, MENU_ITEM_LINK_AMAZON, 0, R.string.menu_link_amazon);
		links.add(0, MENU_ITEM_LINK_EBAY, 0, R.string.menu_link_ebay);
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
}
