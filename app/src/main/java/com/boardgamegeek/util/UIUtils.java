package com.boardgamegeek.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Various static methods for use on views and fragments.
 */
public class UIUtils {
	private static final String KEY_DATA = "_uri";
	private static final String KEY_ACTION = "_action";

	private UIUtils() {
	}

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

	public static void setActivatedCompat(View view, boolean activated) {
		view.setActivated(activated);
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
		if ((text.contains("<") && text.contains(">")) || (text.contains("&") && text.contains(";"))) {
			// Fix up problematic HTML
			// replace DIVs with BR
			text = text.replaceAll("[<]div[^>]*[>]", "");
			text = text.replaceAll("[<]/div[>]", "<br/>");
			// remove all P tags
			text = text.replaceAll("[<](/)?p[>]", "");
			// remove trailing BRs
			text = text.replaceAll("(<br\\s?/>)+$", "");
			// replace 3+ BRs with a double
			text = text.replaceAll("(<br\\s?/>){3,}", "<br/><br/>");
			// use BRs instead of new line character
			text = text.replaceAll("\n", "<br/>");
			text = fixInternalLinks(text);

			Spanned spanned = Html.fromHtml(text);
			view.setText(spanned);
			view.setMovementMethod(LinkMovementMethod.getInstance());
		} else {
			view.setText(text);
		}
	}

	public static void setWebViewText(WebView view, String text) {
		view.loadDataWithBaseURL(null, fixInternalLinks(text), "text/html", "UTF-8", null);
	}

	private static String fixInternalLinks(String text) {
		// ensure internal, path-only links are complete with the hostname
		if (TextUtils.isEmpty(text)) return "";
		String fixedText = text.replaceAll("<a\\s+href=\"/", "<a href=\"https://www.boardgamegeek.com/");
		fixedText = fixedText.replaceAll("<img\\s+src=\"//", "<img src=\"https://");
		return fixedText;
	}

	public static void startTimerWithSystemTime(Chronometer timer, long time) {
		timer.setBase(time - System.currentTimeMillis() + SystemClock.elapsedRealtime());
		timer.start();
	}

	public static void finishingEditing(EditText editText) {
		editText.setSelection(0, editText.getText().length());
		editText.requestFocus();
		((InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
			.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
	}

	public static void showMenuItem(Menu menu, int itemId, boolean visible) {
		MenuItem menuItem = menu.findItem(itemId);
		if (menuItem == null) return;
		menuItem.setVisible(visible);
	}

	public static void enableMenuItem(Menu menu, int itemId, boolean enabled) {
		MenuItem menuItem = menu.findItem(itemId);
		if (menuItem == null) return;
		menuItem.setEnabled(enabled);
	}

	public static void checkMenuItem(Menu menu, int itemId) {
		MenuItem menuItem = menu.findItem(itemId);
		if (menuItem == null) return;
		menuItem.setChecked(true);
	}
}
