package com.boardgamegeek.pref;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;

public class SignOutPreference extends DialogPreference {
	private Context mContext;

	public SignOutPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
	}

	public SignOutPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
	}

	@Override
	protected View onCreateDialogView() {
		TextView tw = new TextView(getContext());
		tw.setText(R.string.pref_sync_sign_out_are_you_sure);
		int padding = (int) getContext().getResources().getDimension(R.dimen.padding_extra);
		tw.setPadding(padding, padding, padding, padding);
		return tw;
	}
	
	@Override
	public boolean isEnabled() {
		return Authenticator.isSignedIn(mContext);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
			Authenticator.signOut(mContext);
			notifyChanged();
		}
	}
}
