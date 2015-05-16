/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.boardgamegeek.pref;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.boardgamegeek.R;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link Preference} that displays a list of entries as a dialog.
 * <p/>
 * This preference will store a set of strings into the SharedPreferences. This set will contain one or more values from
 * the {@link #setEntryValues(CharSequence[])} array.
 *
 * @attr ref android.R.styleable#MultiSelectListPreference_entries
 * @attr ref android.R.styleable#MultiSelectListPreference_entryValues
 */
public class MultiSelectListPreference extends DialogPreference {
	private static final String SEPARATOR = "OV=I=XseparatorX=I=VO";
	private CharSequence[] mEntries;
	private CharSequence[] mEntryValues;
	private Set<String> mValues = new HashSet<>();
	private Set<String> mNewValues = new HashSet<>();
	private boolean mPreferenceChanged;

	public MultiSelectListPreference(Context context, AttributeSet attrs) {
		super(context, attrs);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MultiSelectListPreference, 0, 0);
		mEntries = a.getTextArray(R.styleable.MultiSelectListPreference_android_entries);
		mEntryValues = a.getTextArray(R.styleable.MultiSelectListPreference_android_entryValues);
		a.recycle();
	}

	public MultiSelectListPreference(Context context) {
		this(context, null);
	}

	/**
	 * Sets the human-readable entries to be shown in the list. This will be shown in subsequent dialogs.
	 * <p/>
	 * Each entry must have a corresponding index in {@link #setEntryValues(CharSequence[])}.
	 *
	 * @param entries The entries.
	 * @see #setEntryValues(CharSequence[])
	 */
	public void setEntries(CharSequence[] entries) {
		mEntries = entries;
	}

	/**
	 * @param entriesResId The entries array as a resource.
	 * @see #setEntries(CharSequence[])
	 */
	public void setEntries(int entriesResId) {
		setEntries(getContext().getResources().getTextArray(entriesResId));
	}

	/**
	 * The list of entries to be shown in the list in subsequent dialogs.
	 *
	 * @return The list as an array.
	 */
	public CharSequence[] getEntries() {
		return mEntries;
	}

	/**
	 * The array to find the value to save for a preference when an entry from entries is selected. If a user clicks on
	 * the second item in entries, the second item in this array will be saved to the preference.
	 *
	 * @param entryValues The array to be used as values to save for the preference.
	 */
	public void setEntryValues(CharSequence[] entryValues) {
		mEntryValues = entryValues;
	}

	/**
	 * @param entryValuesResId The entry values array as a resource.
	 * @see #setEntryValues(CharSequence[])
	 */
	public void setEntryValues(int entryValuesResId) {
		setEntryValues(getContext().getResources().getTextArray(entryValuesResId));
	}

	/**
	 * Returns the array of values to be saved for the preference.
	 *
	 * @return The array of values.
	 */
	public CharSequence[] getEntryValues() {
		return mEntryValues;
	}

	/**
	 * Sets the value of the key. This should contain entries in {@link #getEntryValues()}.
	 *
	 * @param values The values to set for the key.
	 */
	public void setValues(Set<String> values) {
		mValues.clear();
		mValues.addAll(values);

		persistStringSet(values);
	}

	private boolean persistStringSet(Set<String> values) {
		String value = buildString(values);
		return persistString(value);
	}

	/**
	 * Retrieves the current value of the key.
	 */
	public Set<String> getValues() {
		return mValues;
	}

	/**
	 * Returns the index of the given value (in the entry values array).
	 *
	 * @param value The value whose index should be returned.
	 * @return The index of the value, or -1 if not found.
	 */
	public int findIndexOfValue(String value) {
		if (value != null && mEntryValues != null) {
			for (int i = mEntryValues.length - 1; i >= 0; i--) {
				if (mEntryValues[i].equals(value)) {
					return i;
				}
			}
		}
		return -1;
	}

	@Override
	public CharSequence getSummary() {
		if (mValues == null || mValues.size() == 0) {
			return getContext().getString(R.string.pref_list_empty);
		}

		StringBuilder sb = new StringBuilder();
		for (CharSequence value : mValues) {
			for (int i = 0; i < mEntryValues.length; i++) {
				CharSequence entry = mEntryValues[i];
				if (entry.equals(value)) {
					sb.append(mEntries[i]).append(", ");
					break;
				}
			}
		}
		if (sb.length() > 2) {
			return sb.substring(0, sb.length() - 2);
		} else {
			return sb.toString();
		}
	}

	@Override
	protected void onPrepareDialogBuilder(Builder builder) {
		super.onPrepareDialogBuilder(builder);

		if (mEntries == null || mEntryValues == null) {
			throw new IllegalStateException("MultiSelectListPreference requires an entries array and "
				+ "an entryValues array.");
		}

		boolean[] checkedItems = getSelectedItems();
		builder.setMultiChoiceItems(mEntries, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				if (isChecked) {
					mPreferenceChanged |= mNewValues.add(mEntryValues[which].toString());
				} else {
					mPreferenceChanged |= mNewValues.remove(mEntryValues[which].toString());
				}
			}
		});
		mNewValues.clear();
		mNewValues.addAll(mValues);
	}

	private boolean[] getSelectedItems() {
		final CharSequence[] entries = mEntryValues;
		final int entryCount = entries.length;
		final Set<String> values = mValues;
		boolean[] result = new boolean[entryCount];

		for (int i = 0; i < entryCount; i++) {
			result[i] = values.contains(entries[i].toString());
		}

		return result;
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		if (positiveResult && mPreferenceChanged) {
			notifyChanged();

			final Set<String> values = mNewValues;
			if (callChangeListener(values)) {
				setValues(values);
			}
		}
		mPreferenceChanged = false;
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		final CharSequence[] defaultValues = a.getTextArray(index);
		final Set<String> result = new HashSet<>();

		for (CharSequence defaultValue : defaultValues) {
			result.add(defaultValue.toString());
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		setValues(restoreValue ? getPersistedStringSet(mValues) : (Set<String>) defaultValue);
	}

	private Set<String> getPersistedStringSet(Set<String> defaultReturnValue) {
		if (!shouldPersist()) {
			return defaultReturnValue;
		}
		String string = getPersistedString("");
		String[] values = string.split(SEPARATOR);
		Set<String> set = new HashSet<>();
		Collections.addAll(set, values);
		return set;
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		final Parcelable superState = super.onSaveInstanceState();
		if (isPersistent()) {
			// No need to save instance state
			return superState;
		}

		final SavedState myState = new SavedState(superState);
		myState.values = getValues();
		return myState;
	}

	private static class SavedState extends BaseSavedState {
		Set<String> values;

		public SavedState(Parcel source) {
			super(source);
			values = new HashSet<>();
			String[] strings = source.createStringArray();

			final int stringCount = strings.length;
			values.addAll(Arrays.asList(strings).subList(0, stringCount));
		}

		public SavedState(Parcelable superState) {
			super(superState);
		}

		@Override
		public void writeToParcel(@NonNull Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeStringArray(values.toArray(new String[values.size()]));
		}

		@SuppressWarnings("unused")
		public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}

	public static String[] parseStoredValue(String value) {
		if (TextUtils.isEmpty(value)) {
			return null;
		} else {
			return value.split(SEPARATOR);
		}
	}

	/**
	 * Builds a persistable string from the set of string values
	 * @param values Set of values to convert to a string.
	 * @return A string representation of the values to persist in preferences.
	 */
	public static String buildString(Set<String> values) {
		StringBuilder sb = new StringBuilder();
		for (String value : values) {
			sb.append(value).append(SEPARATOR);
		}
		// remove trailing separator
		String value = sb.toString();
		if (value.length() > 0) {
			value = value.substring(0, value.length() - SEPARATOR.length());
		}
		return value;
	}
}