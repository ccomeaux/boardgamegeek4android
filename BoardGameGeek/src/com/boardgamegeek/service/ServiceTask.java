package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.ArrayList;
import java.util.Map;

import com.boardgamegeek.io.BggService;
import com.boardgamegeek.io.RetryableException;
import com.boardgamegeek.model.CollectionItem;
import com.boardgamegeek.model.CollectionResponse;

public class ServiceTask {
	public static final int NO_NOTIFICATION = 0;
	private static final String TAG = makeLogTag(ServiceTask.class);
	private static final int MAX_RETRIES = 5;
	private static final int RETRY_BACKOFF = 100;

	public ServiceTask() {
		super();
	}

	protected int getNotification() {
		return NO_NOTIFICATION;
	}

	protected CollectionResponse getCollectionResponse(BggService service, String username, Map<String, String> options) {
		int retries = 0;
		while (true) {
			try {
				return service.collection(username, options);
			} catch (Exception e) {
				if (e instanceof RetryableException || e.getCause() instanceof RetryableException) {
					retries++;
					if (retries > MAX_RETRIES) {
						break;
					}
					try {
						LOGI(TAG, "...retrying #" + retries);
						Thread.sleep(retries * retries * RETRY_BACKOFF);
					} catch (InterruptedException e1) {
						LOGI(TAG, "Interrupted while sleeping before retry " + retries);
						break;
					}
				} else {
					throw e;
				}
			}
		}
		CollectionResponse response = new CollectionResponse();
		response.items = new ArrayList<CollectionItem>();
		return response;
	}
}