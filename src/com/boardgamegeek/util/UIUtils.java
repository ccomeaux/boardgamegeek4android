package com.boardgamegeek.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.ui.HomeActivity;

public class UIUtils {
	
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
	public static void setTitle(Activity activity){
		setTitle(activity, activity.getTitle());
	}
	
	/**
	 * Sets the custom title since the default is hidden.
	 */
	public static void setTitle(Activity activity, CharSequence title){
		((TextView) activity.findViewById(R.id.title_text)).setText(title);
	}
	
	public static void allowTypeToSearch(Activity activity){
		activity.setDefaultKeyMode(Activity.DEFAULT_KEYS_SEARCH_LOCAL);
	}
}
