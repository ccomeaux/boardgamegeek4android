package com.boardgamegeek.filterer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;

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
		selectedSubtype = Integer.valueOf(data);
	}

	@Override
	public int getType() {
		return CollectionFiltererFactory.TYPE_EXPANSION_STATUS;
	}

	@Override
	public String getDisplayText() {
		String text = "";
		String[] subtypes = context.getResources().getStringArray(R.array.expansion_status_filter);
		if (subtypes != null && selectedSubtype != 0 && selectedSubtype < subtypes.length) {
			text = subtypes[selectedSubtype];
		}
		return text;
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
		String value = getSubType(R.array.expansion_status_filter_values);
		if (!TextUtils.isEmpty(value)) {
			return Games.SUBTYPE + "=?";
		} else {
			return "";
		}
	}

	@Override
	public String[] getSelectionArgs() {
		return new String[] { getSubType(R.array.expansion_status_filter_values) };
	}

	private String getSubType(int expansion_status_filter_values) {
		String[] values = context.getResources().getStringArray(expansion_status_filter_values);
		if (values != null && selectedSubtype != 0 && selectedSubtype < values.length) {
			return values[selectedSubtype];
		}
		return "";
	}
}
