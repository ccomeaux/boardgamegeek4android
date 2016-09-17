package com.boardgamegeek.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import com.boardgamegeek.R;
import com.github.amlcurran.showcaseview.ShowcaseView;

import hugo.weaving.DebugLog;
import timber.log.Timber;

/**
 * Methods to display help text.
 */
public class HelpUtils {
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
	 * Determines if this version of the help key should be shown.
	 */
	@DebugLog
	public static boolean shouldShowHelp(Context context, String key, int version) {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		final int shownVersion = preferences.getInt(key, 0);
		return version > shownVersion;
	}

	@DebugLog
	public static void updateHelp(Context context, String key, int version) {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		preferences.edit().putInt(key, version).apply();
	}

	/**
	 * Get the version name of the package, or "?.?" if not found.
	 */
	@DebugLog
	public static String getVersionName(Context context) {
		try {
			PackageManager pm = context.getPackageManager();
			PackageInfo pInfo = pm.getPackageInfo(context.getPackageName(), 0);
			return pInfo.versionName;
		} catch (NameNotFoundException e) {
			return "?.?";
		}
	}

	@DebugLog
	public static ShowcaseView.Builder getShowcaseBuilder(Activity activity) {
		return new ShowcaseView.Builder(activity)
			.withMaterialShowcase()
			.hideOnTouchOutside()
			.setStyle(R.style.BggShowcaseTheme)
			.setContentTitle(R.string.help_title);
	}

	public static View getRecyclerViewVisibleChild(RecyclerView view) {
		View child = null;
		int position = 1;
		LinearLayoutManager layoutManager = (LinearLayoutManager) view.getLayoutManager();
		if (layoutManager != null) {
			position = layoutManager.findFirstCompletelyVisibleItemPosition();
			child = layoutManager.findViewByPosition(position);
		}
		if (child == null) {
			Timber.w("No child available at position " + position);
		}
		return child;
	}

	@DebugLog
	@NonNull
	public static LayoutParams getLowerLeftLayoutParams(Context context) {
		LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		int margin = ((Number) (context.getResources().getDisplayMetrics().density * 12)).intValue();
		layoutParams.setMargins(margin, margin, margin, margin);
		return layoutParams;
	}
}
