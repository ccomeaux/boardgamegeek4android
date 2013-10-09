package com.boardgamegeek.pref;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.TypedValue;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.util.VersionUtils;

public class SignOutPreference extends DialogPreference {

	public SignOutPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public SignOutPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void init() {
		int dialogResourceId = android.R.drawable.ic_dialog_alert;
		if (VersionUtils.hasHoneycomb()) {
			TypedValue typedValue = new TypedValue();
			getContext().getTheme().resolveAttribute(android.R.attr.alertDialogIcon, typedValue, true);
			dialogResourceId = typedValue.resourceId;
		}
		setDialogIcon(dialogResourceId);
		setDialogLayoutResource(R.layout.textview_dialogpreference);
	}

	@Override
	public CharSequence getDialogMessage() {
		return getContext().getString(R.string.pref_sync_sign_out_are_you_sure);
	}

	@Override
	public boolean isEnabled() {
		return Authenticator.isSignedIn(getContext());
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
			Authenticator.signOut(getContext());
			notifyChanged();
		}
	}
}
