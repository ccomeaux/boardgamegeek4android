package com.boardgamegeek.service;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.io.PlaySender;
import com.boardgamegeek.io.RemoteBggHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemotePlaysHandler;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.LogInHelper;

public class SyncPlays extends SyncTask {
	private final static String TAG = "SyncPlays";

	private RemoteExecutor mExecutor;
	private Context mContext;
	private long mStartTime;

	@Override
	public void execute(RemoteExecutor executor, Context context) throws HandlerException {
		mExecutor = executor;
		mContext = context;

		updatePendingPlays();

		mStartTime = System.currentTimeMillis();

		executePagedGet(HttpUtils.constructPlaysUrlNew(BggApplication.getInstance().getUserName()));
		deleteMissingPlays(BggApplication.getInstance().getMinPlayDate(), true);

		executePagedGet(HttpUtils.constructPlaysUrlOld(BggApplication.getInstance().getUserName()));
		deleteMissingPlays(BggApplication.getInstance().getMaxPlayDate(), false);

		BggApplication.getInstance().putMaxPlayDate("0000-00-00");
	}

	private void updatePendingPlays() {
		LogInHelper helper = new LogInHelper(mContext, null);
		if (helper.checkCookies()) {
			PlaySender playSender = new PlaySender(mContext, helper.getCookieStore());
			Cursor cursor = null;
			try {
				cursor = mContext.getContentResolver().query(Plays.CONTENT_URI, null, Plays.SYNC_STATUS + "=?",
						new String[] { String.valueOf(Play.SYNC_STATUS_PENDING) }, null);
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

	private void executePagedGet(String url) throws HandlerException {
		RemoteBggHandler handler = new RemotePlaysHandler();
		int page = 1;
		while (mExecutor.executeGet(url + "&page=" + page, handler)) {
			if (handler.isBggDown()) {
				setIsBggDown(true);
				break;
			}
			page++;
		}
	}

	private void deleteMissingPlays(String date, boolean isMinDate) {
		String selection = Plays.UPDATED_LIST + "<? AND " + Plays.DATE + (isMinDate ? ">" : "<") + "=? AND "
				+ Plays.SYNC_STATUS + "=" + Play.SYNC_STATUS_SYNCED;
		String[] selectionArgs = new String[] { String.valueOf(mStartTime), date };
		int count = mContext.getContentResolver().delete(Plays.CONTENT_URI, selection, selectionArgs);
		Log.i(TAG, "Deleted " + count + " plays");
	}

	@Override
	public int getNotification() {
		return R.string.notification_text_plays;
	}
}
