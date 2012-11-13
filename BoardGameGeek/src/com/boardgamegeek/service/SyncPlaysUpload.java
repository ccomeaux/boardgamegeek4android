package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.R;
import com.boardgamegeek.io.PlaySender;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.LogInHelper;

public class SyncPlaysUpload extends SyncTask {
	private static final String TAG = makeLogTag(SyncPlaysUpload.class);
	private Context mContext;

	@Override
	public void execute(RemoteExecutor executor, Context context) throws HandlerException {
		mContext = context;

		updatePendingPlays();
		deletePendingPlays();
	}

	@Override
	public int getNotification() {
		return R.string.notification_text_plays_upload;
	}

	private void updatePendingPlays() {
		LogInHelper helper = new LogInHelper(mContext, null);
		if (helper.checkCookies()) {
			PlaySender playSender = new PlaySender(mContext, helper.getCookieStore());
			Cursor cursor = null;
			try {
				cursor = mContext.getContentResolver().query(Plays.CONTENT_URI, null, Plays.SYNC_STATUS + "=?",
					new String[] { String.valueOf(Play.SYNC_STATUS_PENDING_UPDATE) }, null);
				LOGI(TAG, String.format("Updating %s plays", cursor.getCount()));
				while (cursor.moveToNext()) {
					Play play = new Play().populate(cursor);
					playSender.sendPlay(play);
				}
			} finally {
				if (cursor != null && !cursor.isClosed()) {
					cursor.close();
				}
			}
		}
	}

	private void deletePendingPlays() {
		LogInHelper helper = new LogInHelper(mContext, null);
		if (helper.checkCookies()) {
			Cursor cursor = null;
			try {
				cursor = mContext.getContentResolver().query(Plays.CONTENT_URI, new String[] { Plays.PLAY_ID },
					Plays.SYNC_STATUS + "=?", new String[] { String.valueOf(Play.SYNC_STATUS_PENDING_DELETE) }, null);
				LOGI(TAG, String.format("Deleting %s plays", cursor.getCount()));
				while (cursor.moveToNext()) {
					ActivityUtils.deletePlay(mContext, helper.getCookieStore(), cursor.getInt(0));
				}
			} finally {
				if (cursor != null && !cursor.isClosed()) {
					cursor.close();
				}
			}
		}
	}
}
