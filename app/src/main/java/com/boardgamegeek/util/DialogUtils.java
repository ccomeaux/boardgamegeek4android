package com.boardgamegeek.util;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.view.View;

import com.boardgamegeek.R;

/**
 * Helper class for creating and modifying dialogs.
 */
public class DialogUtils {
	private DialogUtils() {
	}

	@SuppressLint("CommitTransaction")
	public static void launchDialog(Fragment host, DialogFragment dialog, String tag, Bundle arguments) {
		FragmentTransaction ft = host.getFragmentManager().beginTransaction();
		Fragment prev = host.getFragmentManager().findFragmentByTag(tag);
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);

		dialog.setArguments(arguments);
		dialog.show(ft, tag);
	}

	public static Dialog createCancelDialog(final Activity activity) {
		return createConfirmationDialog(activity, R.string.are_you_sure_cancel,
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					activity.setResult(Activity.RESULT_CANCELED);
					activity.finish();
				}
			});
	}

	public static Dialog createConfirmationDialog(Context context, String message,
												  DialogInterface.OnClickListener okListener) {
		return createConfirmationDialog(context, -1, message, null, okListener, null);
	}

	public static Dialog createConfirmationDialog(Context context, int messageId,
												  DialogInterface.OnClickListener okListener) {
		return createConfirmationDialog(context, messageId, null, null, okListener, null);
	}

	private static Dialog createConfirmationDialog(Context context, int messageId, String message, View view,
												   DialogInterface.OnClickListener okListener,
												   DialogInterface.OnClickListener cancelListener) {
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

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static AlertDialog.Builder addAlertIcon(AlertDialog.Builder builder) {
		if (VersionUtils.hasHoneycomb()) {
			return builder.setIconAttribute(android.R.attr.alertDialogIcon);
		} else {
			return builder.setIcon(android.R.drawable.ic_dialog_alert);
		}
	}
}
