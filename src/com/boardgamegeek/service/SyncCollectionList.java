package com.boardgamegeek.service;

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

		String[] filters = new String[] { "own", "prevowned", "trade", "want", "wanttoplay", "wanttobuy", "wishlist",
				"preordered" };
		String filterOff = "";
		for (int i = 0; i < filters.length; i++) {
			executor.executeGet(HttpUtils.constructCollectionUrl(mUsername, filters[i]), new RemoteCollectionHandler(
					startTime));
			filterOff = filterOff + "," + filters[i] + "=0";
		}
		executor.executeGet(HttpUtils.constructCollectionUrl(mUsername, null) + "?" + filterOff.substring(1),
				new RemoteCollectionHandler(startTime));
		resolver.delete(Games.CONTENT_URI, Games.UPDATED_LIST + "<?", new String[] { "" + startTime });
	}

	@Override
	public int getNotification() {
		return R.string.notification_text_collection_list;
	}
}
