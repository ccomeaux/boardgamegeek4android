package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.content.Context;

import com.boardgamegeek.io.RemoteDesignerHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.util.HttpUtils;

public class SyncDesigner extends SyncTask {
	private static final String TAG = makeLogTag(SyncDesigner.class);
	private int mDesignerId;

	public SyncDesigner(int designerId) {
		mDesignerId = designerId;
	}

	@Override
	public void execute(RemoteExecutor executor, Context context) throws HandlerException {
		RemoteDesignerHandler handler = new RemoteDesignerHandler(mDesignerId);
		executor.executeGet(HttpUtils.constructDesignerUrl(mDesignerId), handler);
		setIsBggDown(handler.isBggDown());
		LOGI(TAG, "Synched Designer " + mDesignerId);
	}
}
