package com.boardgamegeek.filterer;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;

public class ExpansionStatusFilterer extends CollectionFilterer {
	private int selectedSubtype;

	public ExpansionStatusFilterer() {
		setType(CollectionFilterDataFactory.TYPE_EXPANSION_STATUS);
	}

	public ExpansionStatusFilterer(@NonNull Context context, String data) {
		super(context);
		setData(data);
		init(context);
	}

	public ExpansionStatusFilterer(@NonNull Context context, int selectedSubtype) {
		super(context);
		this.selectedSubtype = selectedSubtype;
		init(context);
	}

	@Override
	public void setData(@NonNull String data) {
		selectedSubtype = Integer.valueOf(data);
	}

	private void init(@NonNull Context context) {
		setType(CollectionFilterDataFactory.TYPE_EXPANSION_STATUS);
		createDisplayText(context.getResources());
		setSelection(context.getResources());
	}

	private void createDisplayText(@NonNull Resources resources) {
		String text = "";
		String[] subtypes = resources.getStringArray(R.array.expansion_status_filter);
		if (subtypes != null && selectedSubtype != 0 && selectedSubtype < subtypes.length) {
			text = subtypes[selectedSubtype];
		}
		displayText(text);
	}

	public int getSelectedSubtype() {
		return selectedSubtype;
	}

	@NonNull
	@Override
	public String flatten() {
		return String.valueOf(selectedSubtype);
	}

	private void setSelection(@NonNull Resources resources) {
		String value = "";
		String[] values = resources.getStringArray(R.array.expansion_status_filter_values);
		if (values != null && selectedSubtype != 0 && selectedSubtype < values.length) {
			value = values[selectedSubtype];
		}

		if (!TextUtils.isEmpty(value)) {
			selection(Games.SUBTYPE + "=?");
			selectionArgs(value);
		} else {
			selection("");
			selectionArgs("");
		}
	}
}
