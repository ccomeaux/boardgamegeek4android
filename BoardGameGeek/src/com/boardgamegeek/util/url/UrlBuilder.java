package com.boardgamegeek.util.url;

import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class UrlBuilder {
	private static final String TAG = makeLogTag(UrlBuilder.class);
	protected static final String BASE_URL = "http://boardgamegeek.com/xmlapi/";
	protected static final String BASE_URL_2 = "http://boardgamegeek.com/xmlapi2/";

	protected static String encode(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			LOGE(TAG, "What do you mean UTF-8 isn't supported?!", e);
		}
		return s;
	}
}