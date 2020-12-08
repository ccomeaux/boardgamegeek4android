package com.boardgamegeek.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;

import com.boardgamegeek.R;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

/**
 * Helper class for creating and modifying dialogs.
 */
public class DialogUtils {
	private DialogUtils() {
	}

	public static Builder createThemedBuilder(Context context) {
		return new AlertDialog.Builder(context, R.style.Theme_bgglight_Dialog_Alert);
	}

	public static void showAndSurvive(Fragment host, DialogFragment dialog) {
		final FragmentManager fragmentManager = host.getParentFragmentManager();
		String tag = "dialog";

		FragmentTransaction ft = fragmentManager.beginTransaction();
		Fragment prev = fragmentManager.findFragmentByTag(tag);
		if (prev != null) ft.remove(prev);
		ft.addToBackStack(null);

		dialog.show(ft, tag);
	}

	public interface OnDiscardListener {
		void onDiscard();
	}

	public static Dialog createDiscardDialog(final Activity activity, @StringRes int objectResId, boolean isNew) {
		return createDiscardDialog(activity, objectResId, isNew, true, null);
	}

	public static Dialog createDiscardDialog(final Activity activity, @StringRes int objectResId, boolean isNew, final boolean finishActivity, final OnDiscardListener listener) {
		if (activity == null) return null;
		String messageFormat = activity.getString(isNew ?
			R.string.discard_new_message :
			R.string.discard_changes_message);
		return createThemedBuilder(activity)
			.setMessage(String.format(messageFormat, activity.getString(objectResId).toLowerCase()))
			.setPositiveButton(R.string.keep_editing, null)
			.setNegativeButton(R.string.discard, (dialog, id) -> {
				if (listener != null) listener.onDiscard();
				if (finishActivity) {
					activity.setResult(Activity.RESULT_CANCELED);
					activity.finish();
				}
			})
			.setCancelable(true)
			.create();
	}

	public static Dialog createConfirmationDialog(Context context, int messageId, OnClickListener okListener) {
		return createConfirmationDialog(context, messageId, okListener, R.string.ok);
	}

	public static Dialog createConfirmationDialog(Context context, int messageId, OnClickListener okListener, @StringRes int positiveButtonTextId) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context)
			.setCancelable(true)
			.setNegativeButton(R.string.cancel, null)
			.setPositiveButton(positiveButtonTextId, okListener);
		if (messageId > 0) builder.setMessage(messageId);
		return builder.create();
	}

	public static void showFragment(FragmentActivity activity, DialogFragment fragment, String tag) {
		FragmentManager manager = activity.getSupportFragmentManager();
		final FragmentTransaction ft = manager.beginTransaction();
		final Fragment prev = manager.findFragmentByTag(tag);
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);
		fragment.show(ft, tag);
	}
}
