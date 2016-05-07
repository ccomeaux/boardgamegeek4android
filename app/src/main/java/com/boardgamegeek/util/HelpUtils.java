package com.boardgamegeek.util;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ListView;

import com.boardgamegeek.R;
import com.github.amlcurran.showcaseview.ShowcaseView;

import timber.log.Timber;

/**
 * Methods to display help text.
 */
public class HelpUtils {
	public static final String HELP_HOME_KEY = "help.home";
	public static final String HELP_GAME_KEY = "help.game";
	public static final String HELP_COLLECTION_KEY = "help.collection";
	public static final String HELP_PLAYS_KEY = "help.plays";
	public static final String HELP_SEARCHRESULTS_KEY = "help.searchresults";
	public static final String HELP_LOGPLAY_KEY = "help.logplay";
	public static final String HELP_LOGPLAYER_KEY = "help.logplayer";
	//public static final String HELP_COLORS_KEY = "help.colors";
	public static final String HELP_THREAD_KEY = "help.thread";

	private HelpUtils() {
	}

	/**
	 * Display this key's help text in a dialog.
	 */
	public static void showHelpDialog(final Context context, final String key, final int version, int messageId) {
		if (HelpUtils.shouldShowHelp(context, key, version)) {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder
				.setTitle(R.string.help_title)
				.setCancelable(false)
				.setMessage(messageId)
				.setPositiveButton(R.string.help_button_close, null)
				.setNegativeButton(R.string.help_button_hide, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						HelpUtils.updateHelp(context, key, version);
					}
				});
			builder = DialogUtils.addAlertIcon(builder);
			builder.create().show();
		}
	}

	/**
	 * Determines if this version of the help key should be shown.
	 */
	public static boolean shouldShowHelp(Context context, String key, int version) {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		final int shownVersion = preferences.getInt(key, 0);
		return version > shownVersion;
	}

	public static void updateHelp(Context context, String key, int version) {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		preferences.edit().putInt(key, version).apply();
	}

	/**
	 * Get the version name of the package, or "?.?" if not found.
	 */
	public static String getVersionName(Context context) {
		try {
			PackageManager pm = context.getPackageManager();
			PackageInfo pInfo = pm.getPackageInfo(context.getPackageName(), 0);
			return pInfo.versionName;
		} catch (NameNotFoundException e) {
			return "?.?";
		}
	}

	public static ShowcaseView.Builder getShowcaseBuilder(Activity activity) {
		return new ShowcaseView.Builder(activity)
			.withMaterialShowcase()
			.hideOnTouchOutside()
			.setStyle(R.style.BggShowcaseTheme)
			.setContentTitle(R.string.help_title);
	}

	public static View getListViewVisibleChild(ListView listView) {
		if (listView == null) return null;

		final int first = listView.getFirstVisiblePosition();
		final int last = listView.getLastVisiblePosition();
		int position = (first + last) / 2 - first;
		if (position == 0 && (last - first) > 0) {
			position = 1;
		}
		final View child = listView.getChildAt(position);
		if (child == null) {
			Timber.w("No child available at position " + position);
		}
		return child;
	}
}
