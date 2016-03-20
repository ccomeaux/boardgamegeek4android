package com.boardgamegeek.pref;

import android.content.Context;
import android.preference.DialogPreference;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.service.SyncService;

public class SyncTimestampsDialogPreference extends DialogPreference {
	public SyncTimestampsDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public SyncTimestampsDialogPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		setDialogTitle(R.string.pref_sync_timestamps);
		setDialogLayoutResource(R.layout.dialog_sync_stats);
		setPositiveButtonText(R.string.close);
		setNegativeButtonText("");
	}

	@Override
	protected void onBindDialogView(@NonNull View view) {
		super.onBindDialogView(view);
		TextView collectionFull = (TextView) view.findViewById(R.id.sync_timestamp_collection_full);
		TextView collectionPartial = (TextView) view.findViewById(R.id.sync_timestamp_collection_partial);
		TextView buddies = (TextView) view.findViewById(R.id.sync_timestamp_buddy);
		TextView playsNewest = (TextView) view.findViewById(R.id.sync_timestamp_plays_newest_date);
		TextView playsOldest = (TextView) view.findViewById(R.id.sync_timestamp_plays_oldest_date);

		setDateTime(collectionFull, Authenticator.getLong(getContext(), SyncService.TIMESTAMP_COLLECTION_COMPLETE));
		setDateTime(collectionPartial, Authenticator.getLong(getContext(), SyncService.TIMESTAMP_COLLECTION_PARTIAL));
		setDateTime(buddies, Authenticator.getLong(getContext(), SyncService.TIMESTAMP_BUDDIES));
		setDate(playsNewest, Authenticator.getLong(getContext(), SyncService.TIMESTAMP_PLAYS_NEWEST_DATE));
		setDate(playsOldest, Authenticator.getLong(getContext(), SyncService.TIMESTAMP_PLAYS_OLDEST_DATE));
	}

	private void setDateTime(TextView view, long timeStamp) {
		CharSequence text;
		if (timeStamp == 0) {
			text = getContext().getString(R.string.never);
		} else {
			int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME;
			text = DateUtils.formatDateTime(getContext(), timeStamp, flags);
		}
		view.setText(text);
	}

	private void setDate(TextView view, long timeStamp) {
		CharSequence text;
		if (timeStamp == 0) {
			text = "-";
		} else {
			int flags = DateUtils.FORMAT_SHOW_DATE;
			text = DateUtils.formatDateTime(getContext(), timeStamp, flags);
		}
		view.setText(text);
	}
}
