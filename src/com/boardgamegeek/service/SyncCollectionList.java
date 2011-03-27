package com.boardgamegeek.service;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteCollectionHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.HttpUtils;

public class SyncCollectionList extends SyncTask {

	private String mUsername;

	@Override
	public void execute(RemoteExecutor executor, Context context) throws HandlerException {

		final long startTime = System.currentTimeMillis();
		ContentResolver resolver = context.getContentResolver();

		mUsername = BggApplication.getInstance().getUserName();
		String[] statuses = BggApplication.getInstance().getSyncStatuses();

		List<String> filterOff = new ArrayList<String>(statuses.length);
		for (int i = 0; i < statuses.length; i++) {
			executor.executeGet(HttpUtils.constructCollectionUrl(mUsername, statuses[i], filterOff),
					new RemoteCollectionHandler(startTime));
			filterOff.add(statuses[i]);
		}
		resolver.delete(Games.CONTENT_URI, Games.UPDATED_LIST + "<?", new String[] { "" + startTime });
	}

	@Override
	public int getNotification() {
		return R.string.notification_text_collection_list;
	}
}
