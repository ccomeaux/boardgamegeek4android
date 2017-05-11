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
		return createDiscardDialog(activity, objectResId, isNew, null);
	}

	public static Dialog createDiscardDialog(final Activity activity, @StringRes int objectResId, boolean isNew, final OnDiscardListener listener) {
		if (activity == null) return null;
		String messageFormat = activity.getString(isNew ?
			R.string.discard_new_message :
			R.string.discard_changes_message);
		AlertDialog.Builder builder = new AlertDialog.Builder(activity)
			.setCancelable(true)
			.setPositiveButton(R.string.keep_editing, null)
			.setNegativeButton(R.string.discard, new OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					if (listener != null) listener.onDiscard();
					activity.setResult(Activity.RESULT_CANCELED);
					activity.finish();
				}
			})
			.setMessage(String.format(messageFormat, activity.getString(objectResId).toLowerCase()));
		return builder.create();
	}

	public static Dialog createConfirmationDialog(Context context, String message, OnClickListener okListener) {
		return createConfirmationDialog(context, -1, message, null, okListener, null);
	}

	public static Dialog createConfirmationDialog(Context context, int messageId, OnClickListener okListener) {
		return createConfirmationDialog(context, messageId, null, null, okListener, null);
	}

	private static Dialog createConfirmationDialog(Context context, int messageId, String message, View view, OnClickListener okListener, OnClickListener cancelListener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context).setCancelable(true)
			.setNegativeButton(android.R.string.cancel, cancelListener)
			.setPositiveButton(android.R.string.ok, okListener)
			.setTitle(R.string.are_you_sure_title);
		builder = addAlertIcon(builder);
		if (messageId != -1) {
			builder.setMessage(messageId);
		} else {
			builder.setMessage(message);
		}
		if (view != null) {
			builder.setView(view);
		}

		return builder.create();
	}

	public static AlertDialog.Builder addAlertIcon(AlertDialog.Builder builder) {
		return builder.setIconAttribute(android.R.attr.alertDialogIcon);
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
