package com.boardgamegeek.util;

import java.util.Random;

import android.annotation.TargetApi;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
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
import android.widget.TextView;

import com.boardgamegeek.R;

public class UIUtils {
	public static final String HELP_GAME_KEY = "help.game";

	public static final int MENU_ITEM_VIEW = Menu.FIRST;
	public static final int MENU_ITEM_LOG_PLAY = Menu.FIRST + 1;
	public static final int MENU_ITEM_QUICK_LOG_PLAY = Menu.FIRST + 2;
	public static final int MENU_ITEM_SHARE = Menu.FIRST + 3;
	public static final int MENU_ITEM_LINK_BGG = Menu.FIRST + 4;

	private static Random mRandom;

	public static Random getRandom() {
		if (mRandom == null) {
			mRandom = new Random();
		}
		return mRandom;
	}

	public static void createBoardgameContextMenu(Context context, ContextMenu menu, ContextMenuInfo menuInfo,
		String gameName) {
		// Setup the menu header
		menu.setHeaderTitle(gameName);

		menu.add(0, MENU_ITEM_VIEW, 0, R.string.menu_display_game);
		if (PreferencesUtils.showLogPlay(context)) {
			menu.add(0, MENU_ITEM_LOG_PLAY, 0, R.string.menu_log_play);
		}
		if (PreferencesUtils.showQuickLogPlay(context)) {
			menu.add(0, MENU_ITEM_QUICK_LOG_PLAY, 0, R.string.menu_log_play_quick);
		}
		menu.add(0, MENU_ITEM_SHARE, 0, R.string.menu_share);
		menu.add(0, MENU_ITEM_LINK_BGG, 0, R.string.menu_link_bgg);
	}

	public static void showHelpDialog(final Context context, final String key, final int version, int messageId) {
		if (HelpUtils.showHelp(context, key, version)) {
			Builder builder = new Builder(context);
			builder.setTitle(R.string.help_title).setCancelable(false).setIcon(android.R.drawable.ic_dialog_info)
				.setMessage(messageId).setPositiveButton(R.string.help_button_close, null)
				.setNegativeButton(R.string.help_button_hide, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						HelpUtils.updateHelp(context, key, version);
					}
				});
			builder.create().show();
		}
	}

	private static String KEY_DATA = "_uri";
	private static String KEY_ACTION = "_action";

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

		final String action = intent.getAction();
		if (action != null) {
			arguments.putString(KEY_ACTION, action);
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

		final String action = arguments.getString(KEY_ACTION);
		if (action != null) {
			intent.setAction(action);
		}

		intent.putExtras(arguments);
		intent.removeExtra(KEY_DATA);
		intent.removeExtra(KEY_ACTION);
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
			// Fix up problematic HTML
			// replace DIVs with BR
			text = text.replaceAll("[<]div[^>]*[>]", "");
			text = text.replaceAll("[<]/div[>]", "<br/>");
			// remove all P tags
			text = text.replaceAll("[<](/)?p[>]", "");
			// remove trailing BRs
			text = text.replaceAll("(<br\\s?/>)+$", "");
			// replace multiple BRs with single
			text = text.replaceAll("(<br\\s?/>){2,}", "<br/>");

			Spanned spanned = Html.fromHtml(text);
			view.setText(spanned);
			view.setMovementMethod(LinkMovementMethod.getInstance());
		} else {
			view.setText(text);
		}
	}
}
