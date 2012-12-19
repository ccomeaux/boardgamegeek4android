package com.boardgamegeek.io;

import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;

import com.boardgamegeek.io.XmlHandler.HandlerException;

public class RemoteExecutor {
	private static final String TAG = makeLogTag(RemoteExecutor.class);

	private static XmlPullParserFactory sFactory;
	private final HttpClient mHttpClient;
	private final Context mContext;

	public HttpClient getHttpClient() {
		return mHttpClient;
	}

	public RemoteExecutor(HttpClient httpClient, Context context) {
		mHttpClient = httpClient;
		mContext = context;
	}

	public void executePagedGet(String url, XmlHandler handler) throws HandlerException {
		int page = 1;
		while (executeGet(url + "&page=" + page, handler)) {
			page++;
		}
	}

	public boolean safelyExecuteGet(String url, XmlHandler handler) {
		final HttpUriRequest request = new HttpGet(url);
		try {
			return execute(request, handler);
		} catch (HandlerException e) {
			LOGE(TAG, "Getting " + url, e);
			((RemoteBggHandler) handler).setErrorMessage(e.toString());
		}
		return false;
	}

	public boolean executeGet(String url, XmlHandler handler) throws HandlerException {
		final HttpUriRequest request = new HttpGet(url);
		return execute(request, handler);
	}

	public boolean execute(HttpUriRequest request, XmlHandler handler) throws HandlerException {
		LOGI(TAG, request.getURI().toString());

		HttpResponse response;
		try {
			response = mHttpClient.execute(request);
			final int status = response.getStatusLine().getStatusCode();

			if (status != HttpStatus.SC_OK) {
				throw new HandlerException("Unexpected server response " + response.getStatusLine() + " for "
					+ request.getRequestLine());
			}

			final InputStream input = response.getEntity().getContent();
			try {
				XmlPullParser parser = createPullParser(input);
				return handler.parseAndHandle(parser, mContext);
			} catch (XmlPullParserException e) {
				throw new HandlerException("Malformed response for " + request.getRequestLine(), e);
			} finally {
				if (input != null) {
					input.close();
				}
			}
		} catch (IOException e) {
			throw new HandlerException("Problem reading remote response for " + request.getRequestLine(), e);
		}
	}

	private static XmlPullParser createPullParser(InputStream input) throws XmlPullParserException {
		if (sFactory == null) {
			sFactory = XmlPullParserFactory.newInstance();
		}
		final XmlPullParser parser = sFactory.newPullParser();
		parser.setInput(input, null);
		return parser;
	}
}
