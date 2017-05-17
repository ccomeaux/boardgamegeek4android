package com.boardgamegeek.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;

import com.boardgamegeek.R;

/**
 * Helper class for creating and modifying dialogs.
 */
public class DialogUtils {
	private DialogUtils() {
	}

	public static Builder createThemedBuilder(Context context) {
		return new AlertDialog.Builder(context, R.style.Theme_bgglight_Dialog_Alert);
	}

	public static void show(DialogFragment fragment, FragmentManager manager, String tag) {
		FragmentTransaction ft = manager.beginTransaction();
		ft.add(fragment, tag);
		ft.commitAllowingStateLoss();
	}

	public static void launchDialog(Fragment host, DialogFragment dialog, String tag, Bundle arguments) {
		FragmentTransaction ft = host.getFragmentManager().beginTransaction();
		Fragment prev = host.getFragmentManager().findFragmentByTag(tag);
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);

		dialog.setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_bgglight_Dialog);
		dialog.setArguments(arguments);
		ft.add(dialog, tag);
		ft.commitAllowingStateLoss();
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
			.setNegativeButton(R.string.discard, new OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					if (listener != null) listener.onDiscard();
					if (finishActivity) {
						activity.setResult(Activity.RESULT_CANCELED);
						activity.finish();
					}
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

	public static void requestFocus(@NonNull AlertDialog dialog) {
		requestFocus(dialog, null);
	}

	public static void requestFocus(@NonNull AlertDialog dialog, View view) {
		if (view != null) view.requestFocus();
		Window window = dialog.getWindow();
		if (window != null) window.setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
	}
}
