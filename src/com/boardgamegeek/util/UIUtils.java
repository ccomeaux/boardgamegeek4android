package com.boardgamegeek.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ProgressBar;
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
	 * Sets the current activity to the home screen, clearing the activity
	 * stack.
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
	public static void setTitle(Activity activity, CharSequence title) {
		((TextView) activity.findViewById(R.id.title_text)).setText(title);
	}

	public static void allowTypeToSearch(Activity activity) {
		activity.setDefaultKeyMode(Activity.DEFAULT_KEYS_SEARCH_LOCAL);
	}
		
	public static String[] projectionFromEnums(Enum<?> e[]) {		
		String projection[] = new String[e.length];
		
		int index = 0;		
		for(Enum<?> currentEnum : e) {			
			projection[index] = currentEnum.toString();
			index++;
		}
		
		return projection;		
	}

	public static void showListMessage(Activity activity, int messageResourceId) {
		showListMessage(activity, messageResourceId, true);
	}

	public static void showListMessage(Activity activity,
			int messageResourceId, boolean hideProgressBar) {
		TextView tv = (TextView) activity.findViewById(R.id.listMessage);
		tv.setText(messageResourceId);

		hideProgressBar(activity, hideProgressBar);
	}

	public static void showListMessage(Activity activity, String message) {
		showListMessage(activity, message, true);
	}

	public static void showListMessage(Activity activity, String message,
			boolean hideProgressBar) {
		TextView tv = (TextView) activity.findViewById(R.id.listMessage);
		tv.setText(message);

		hideProgressBar(activity, hideProgressBar);
	}

	private static void hideProgressBar(Activity activity, boolean hide) {
		ProgressBar pb = (ProgressBar) activity.findViewById(R.id.listProgress);
		if (hide) {
			pb.setVisibility(View.GONE);
		} else {
			pb.setVisibility(View.VISIBLE);
		}
	}
}
