package com.boardgamegeek.filterer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.StringUtils;

public class ExpansionStatusFilterer extends CollectionFilterer {
	private int selectedSubtype;

	public ExpansionStatusFilterer(Context context) {
		super(context);
	}

	public ExpansionStatusFilterer(@NonNull Context context, int selectedSubtype) {
		super(context);
		this.selectedSubtype = selectedSubtype;
	}

	@Override
	public void setData(@NonNull String data) {
		selectedSubtype = StringUtils.parseInt(data);
	}

	@Override
	public int getTypeResourceId() {
		return R.string.collection_filter_type_subtype;
	}

	@Override
	public String getDisplayText() {
		return getSelectedFromStringArray(R.array.expansion_status_filter);
	}

	public int getSelectedSubtype() {
		return selectedSubtype;
	}

	@NonNull
	@Override
	public String flatten() {
		return String.valueOf(selectedSubtype);
	}

	@Override
	public String getSelection() {
		String value = getSelectedFromStringArray(R.array.expansion_status_filter_values);
		if (!TextUtils.isEmpty(value)) {
			return Games.SUBTYPE + "=?";
		} else {
			return "";
		}
	}

	@Override
	public String[] getSelectionArgs() {
		return new String[] { getSelectedFromStringArray(R.array.expansion_status_filter_values) };
	}

	private String getSelectedFromStringArray(int resId) {
		String[] values = context.getResources().getStringArray(resId);
		if (selectedSubtype != 0 && selectedSubtype < values.length) {
			return values[selectedSubtype];
		}
		return "";
	}
}
