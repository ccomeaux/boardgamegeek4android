package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import android.accounts.Account;
import android.content.Context;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteBggHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemotePlaysHandler;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.HttpUtils;

public class SyncPlays extends SyncTask {
	private static final String TAG = makeLogTag(SyncPlays.class);

	private RemoteExecutor mExecutor;
	private Context mContext;
	private long mStartTime;

	@Override
	public void execute(RemoteExecutor executor, Account account) throws IOException, XmlPullParserException {
		mExecutor = executor;
		mContext = executor.getContext();
		mStartTime = System.currentTimeMillis();

		executePagedGet(HttpUtils.constructPlaysUrlNew(account.name));
		deleteMissingPlays(BggApplication.getInstance().getMinPlayDate(), true);

		executePagedGet(HttpUtils.constructPlaysUrlOld(account.name));
		deleteMissingPlays(BggApplication.getInstance().getMaxPlayDate(), false);

		BggApplication.getInstance().putMaxPlayDate("0000-00-00");
	}

	private void executePagedGet(String url) throws IOException, XmlPullParserException {
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
		LOGI(TAG, "Deleted " + count + " plays");
	}

	@Override
	public int getNotification() {
		return R.string.notification_text_plays;
	}
}
