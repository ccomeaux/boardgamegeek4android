package com.boardgamegeek.util;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.boardgamegeek.R;

public class HelpUtils {
	private static final String FRAGMENT_TAG = "dialog_about";

	@SuppressLint("CommitTransaction")
	public static void showAboutDialog(FragmentActivity activity) {
		FragmentManager fm = activity.getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		Fragment prev = fm.findFragmentByTag(FRAGMENT_TAG);
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);

		new AboutDialog().show(ft, FRAGMENT_TAG);
	}

	public static class AboutDialog extends DialogFragment {
		public AboutDialog() {
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			LayoutInflater layoutInflater = getActivity().getLayoutInflater();
			View rootView = layoutInflater.inflate(R.layout.dialog_about, null);
			TextView nameAndVersionView = (TextView) rootView.findViewById(R.id.app_name_and_version);
			TextView aboutBodyView = (TextView) rootView.findViewById(R.id.about_body);

			nameAndVersionView.setText(Html.fromHtml(getString(R.string.app_name_and_version,
				getVersionName(getActivity()))));
			aboutBodyView.setMovementMethod(new LinkMovementMethod());
			aboutBodyView.setText(Html.fromHtml(getString(R.string.about_body)));

			return new AlertDialog.Builder(getActivity()).setView(rootView)
				.setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.dismiss();
					}
				}).create();
		}
	}

	private static String getVersionName(Context context) {
		try {
			PackageManager pm = context.getPackageManager();
			PackageInfo pInfo = pm.getPackageInfo(context.getPackageName(), 0);
			return pInfo.versionName;
		} catch (NameNotFoundException e) {
			return "?.?";
		}
	}
}
