package com.boardgamegeek.pref;

// TODO: show the statuses in the preference summary

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.ListPreference;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;

/**
 * A {@link Preference} that displays a list of entries as a dialog and allows
 * multiple selections
 * <p>
 * This preference will store a string into the SharedPreferences. This string
 * will be the values selected from the {@link #setEntryValues(CharSequence[])}
 * array.
 * </p>
 */
public class ListPreferenceMultiSelect extends ListPreference {
	// Need to make sure the SEPARATOR is unique and weird enough that it
	// doesn't match one of the entries.
	// Not using any fancy symbols because this is interpreted as a regex for
	// splitting strings.
	private static final String SEPARATOR = "OV=I=XseparatorX=I=VO";

	private boolean[] mClickedDialogEntryIndices;

	public ListPreferenceMultiSelect(Context context, AttributeSet attrs) {
		super(context, attrs);
		mClickedDialogEntryIndices = new boolean[getEntries().length];
	}

	@Override
	public void setEntries(CharSequence[] entries) {
		super.setEntries(entries);
		mClickedDialogEntryIndices = new boolean[entries.length];
	}

	public ListPreferenceMultiSelect(Context context) {
		this(context, null);
	}

	@Override
	protected void onPrepareDialogBuilder(Builder builder) {
		CharSequence[] entries = getEntries();
		CharSequence[] entryValues = getEntryValues();

		if (entries == null || entryValues == null || entries.length != entryValues.length) {
			throw new IllegalStateException(
					"ListPreferenceMultiSelect requires an entries array and an entryValues array which are both the same length");
		}

		restoreCheckedEntries();
		builder.setMultiChoiceItems(entries, mClickedDialogEntryIndices,
				new DialogInterface.OnMultiChoiceClickListener() {
					public void onClick(DialogInterface dialog, int which, boolean val) {
						mClickedDialogEntryIndices[which] = val;
					}
				});
	}

	public static String[] parseStoredValue(String val) {
		if (TextUtils.isEmpty(val)) {
			return null;
		} else {
			return val.split(SEPARATOR);
		}
	}

	private void restoreCheckedEntries() {
		CharSequence[] entryValues = getEntryValues();

		String[] checkedValues = parseStoredValue(getValue());
		if (checkedValues != null) {
			for (int j = 0; j < checkedValues.length; j++) {
				String val = checkedValues[j].trim();
				for (int i = 0; i < entryValues.length; i++) {
					CharSequence entry = entryValues[i];
					if (entry.equals(val)) {
						mClickedDialogEntryIndices[i] = true;
						break;
					}
				}
			}
		}
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		// super.onDialogClosed(positiveResult);

		CharSequence[] entryValues = getEntryValues();
		if (positiveResult && entryValues != null) {
			StringBuffer value = new StringBuffer();
			for (int i = 0; i < entryValues.length; i++) {
				if (mClickedDialogEntryIndices[i]) {
					value.append(entryValues[i]).append(SEPARATOR);
				}
			}

			if (callChangeListener(value)) {
				String val = value.toString();
				if (val.length() > 0)
					val = val.substring(0, val.length() - SEPARATOR.length());
				setValue(val);
			}
		}
	}
}