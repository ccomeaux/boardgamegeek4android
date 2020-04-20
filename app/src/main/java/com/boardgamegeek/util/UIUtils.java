package com.boardgamegeek.util;

import android.content.Context;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.Chronometer;
import android.widget.EditText;

import javax.annotation.Nonnull;

/**
 * Various static methods for use on views and fragments.
 */
public class UIUtils {
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

	public static void finishingEditing(@Nonnull EditText editText) {
		editText.setSelection(0, editText.getText().length());
		focusWithKeyboard(editText);
	}

	public static void focusWithKeyboard(@Nonnull EditText editText) {
		editText.requestFocus();
		InputMethodManager inputMethodManager = (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		if (inputMethodManager != null)
			inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
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
