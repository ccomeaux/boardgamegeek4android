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

import butterknife.BindView;
import butterknife.ButterKnife;

public class SyncTimestampsDialogPreference extends DialogPreference {
	@BindView(R.id.sync_timestamp_collection_full) TextView collectionFull;
	@BindView(R.id.sync_timestamp_collection_partial) TextView collectionPartial;
	@BindView(R.id.sync_timestamp_buddy) TextView buddies;
	@BindView(R.id.sync_timestamp_plays) TextView playsView;

	@SuppressWarnings("unused")
	public SyncTimestampsDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	@SuppressWarnings("unused")
	public SyncTimestampsDialogPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		setDialogTitle(R.string.pref_sync_timestamps_title);
		setDialogLayoutResource(R.layout.dialog_sync_stats);
		setPositiveButtonText(R.string.close);
		setNegativeButtonText("");
	}

	@Override
	protected void onBindDialogView(@NonNull View view) {
		super.onBindDialogView(view);
		ButterKnife.bind(this, view);

		setDateTime(collectionFull, Authenticator.getLong(getContext(), SyncService.TIMESTAMP_COLLECTION_COMPLETE), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME);
		setDateTime(collectionPartial, Authenticator.getLong(getContext(), SyncService.TIMESTAMP_COLLECTION_PARTIAL), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME);

		setDateTime(buddies, Authenticator.getLong(getContext(), SyncService.TIMESTAMP_BUDDIES), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME);

		long oldestDate = Authenticator.getLong(getContext(), SyncService.TIMESTAMP_PLAYS_OLDEST_DATE);
		long newestDate = Authenticator.getLong(getContext(), SyncService.TIMESTAMP_PLAYS_NEWEST_DATE);
		if (oldestDate == 0 && newestDate == 0) {
			playsView.setText(R.string.plays_sync_status_none);
		} else if (oldestDate == 0) {
			playsView.setText(String.format(getContext().getString(R.string.plays_sync_status_new),
				DateUtils.formatDateTime(getContext(), newestDate, DateUtils.FORMAT_SHOW_DATE)));
		} else if (newestDate == 0) {
			playsView.setText(String.format(getContext().getString(R.string.plays_sync_status_old),
				DateUtils.formatDateTime(getContext(), oldestDate, DateUtils.FORMAT_SHOW_DATE)));
		} else {
			playsView.setText(String.format(getContext().getString(R.string.plays_sync_status_range),
				DateUtils.formatDateTime(getContext(), oldestDate, DateUtils.FORMAT_SHOW_DATE),
				DateUtils.formatDateTime(getContext(), newestDate, DateUtils.FORMAT_SHOW_DATE)));
		}
	}

	private void setDateTime(TextView view, long timeStamp, int flags) {
		CharSequence text;
		if (timeStamp == 0) {
			text = getContext().getString(R.string.never);
		} else {
			text = DateUtils.formatDateTime(getContext(), timeStamp, flags);
		}
		view.setText(text);
	}
}
