package com.boardgamegeek.pref;

import android.content.Context;
import android.preference.DialogPreference;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.util.HelpUtils;
import com.boardgamegeek.util.ListTagHandler;

public class VersionPreference extends DialogPreference {

	public VersionPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public VersionPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		setDialogTitle("");
		setDialogLayoutResource(R.layout.dialog_about);
		setPositiveButtonText(R.string.close);
		setNegativeButtonText("");
	}

	@Override
	public CharSequence getSummary() {
		return HelpUtils.getVersionName(getContext());
	}

	@Override
	protected void onBindDialogView(@NonNull View view) {
		super.onBindDialogView(view);
		TextView nameAndVersionView = (TextView) view.findViewById(R.id.app_name_and_version);
		TextView aboutBodyView = (TextView) view.findViewById(R.id.about_body);

		nameAndVersionView.setText(Html.fromHtml(getContext().getString(R.string.pref_about_app_name_and_version,
			HelpUtils.getVersionName(getContext()))));
		aboutBodyView.setMovementMethod(new LinkMovementMethod());
		aboutBodyView.setText(Html.fromHtml(getContext().getString(R.string.pref_about_body), null,
			new ListTagHandler()));
	}
}