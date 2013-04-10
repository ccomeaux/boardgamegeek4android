package com.boardgamegeek.pref;

import com.boardgamegeek.R;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.ListPreference;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;

/**
 * A {@link Preference} that displays a list of entries as a dialog and allows multiple selections
 * <p>
 * This preference will store a string into the SharedPreferences. This string will be the values selected from the
 * {@link #setEntryValues(CharSequence[])} array.
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
	public CharSequence getSummary() {

		String[] checkedValues = parseStoredValue(getValue());
		if (checkedValues == null || checkedValues.length == 0) {
			return getContext().getString(R.string.pref_list_empty);
		}

		CharSequence[] entryValues = getEntryValues();
		CharSequence[] entries = getEntries();
		StringBuffer buffer = new StringBuffer();
		for (int j = 0; j < checkedValues.length; j++) {
			String val = checkedValues[j].trim();
			for (int i = 0; i < entryValues.length; i++) {
				CharSequence entry = entryValues[i];
				if (entry.equals(val)) {
					buffer.append(entries[i]);
					buffer.append(", ");
					break;
				}
			}
		}
		return buffer.substring(0, buffer.length() - 2);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {

		if (!positiveResult) {
			return;
		}

		CharSequence[] entryValues = getEntryValues();
		if (shouldPersist() && entryValues != null) {
			String value = calculateValue(entryValues);

			if (callChangeListener(value)) {
				setValue(value);
			}
		}

		notifyChanged();
	}

	private String calculateValue(CharSequence[] entryValues) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < entryValues.length; i++) {
			if (mClickedDialogEntryIndices[i]) {
				buffer.append(entryValues[i]).append(SEPARATOR);
			}
		}
		// remove trailing separator
		String value = buffer.toString();
		if (value.length() > 0) {
			value = value.substring(0, value.length() - SEPARATOR.length());
		}
		return value;
	}
}