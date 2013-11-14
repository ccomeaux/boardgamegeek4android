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

import com.boardgamegeek.util.HttpUtils;

public class RemoteExecutor {
	private static final String TAG = makeLogTag(RemoteExecutor.class);

	private static XmlPullParserFactory sFactory;
	private final HttpClient mHttpClient;
	private final Context mContext;

	public HttpClient getHttpClient() {
		return mHttpClient;
	}

	public Context getContext() {
		return mContext;
	}
	
	public RemoteExecutor(Context context){
		mContext = context;
		mHttpClient = HttpUtils.createHttpClient(context, true);
	}

	public RemoteExecutor(HttpClient httpClient, Context context) {
		mContext = context;
		mHttpClient = httpClient;
	}

	public void executePagedGet(String url, RemoteBggHandler handler) throws IOException, XmlPullParserException {
		int page = 1;
		while (executeGet(url + "&page=" + page, handler)) {
			page++;
		}
	}

	public boolean safelyExecuteGet(String url, RemoteBggHandler handler) {
		try {
			return executeGet(url, handler);
		} catch (IOException e) {
			LOGE(TAG, "Getting " + url, e);
			handler.setErrorMessage(e.getLocalizedMessage());
		} catch (XmlPullParserException e) {
			LOGE(TAG, "Getting " + url, e);
			handler.setErrorMessage(e.getLocalizedMessage());
		}
		return false;
	}

	public boolean safelyExecuteGet(RemoteBggParser parser) {
		return safelyExecuteGet(parser.getUrl(), parser);
	}
	
	public boolean safelyExecuteGet(String url, RemoteBggParser parser) {
		try {
			return executeGet(url, parser);
		} catch (IOException e) {
			LOGE(TAG, "Getting " + url, e);
			parser.setErrorMessage(e.getLocalizedMessage());
		} catch (XmlPullParserException e) {
			LOGE(TAG, "Getting " + url, e);
			parser.setErrorMessage(e.getLocalizedMessage());
		}
		return false;
	}

	public boolean executeGet(String url, RemoteBggHandler handler) throws IOException, XmlPullParserException {
		final HttpUriRequest request = new HttpGet(url);
		return execute(request, handler);
	}

	public boolean executeGet(String url, RemoteBggParser parser) throws IOException, XmlPullParserException {
		final HttpUriRequest request = new HttpGet(url);
		return execute(request, parser);
	}

	/**
	 * Executes the given request on the current HTTP client, passing the entity content of the response to the given
	 * parser.
	 * 
	 * @return true if there are more pages to execute in a paged get.
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	private boolean execute(HttpUriRequest request, RemoteBggParser parser) throws IOException,
		XmlPullParserException {
		LOGI(TAG, request.getURI().toString());

		HttpResponse response;
		response = mHttpClient.execute(request);
		final int status = response.getStatusLine().getStatusCode();

		if (status != HttpStatus.SC_OK) {
			throw new IOException("Unexpected server response " + response.getStatusLine() + " for "
				+ request.getRequestLine());
		}

		final InputStream input = response.getEntity().getContent();
		try {
			XmlPullParser pullParser = createPullParser(input);
			return parser.parse(pullParser, mContext);
		} finally {
			if (input != null) {
				input.close();
			}
		}
	}

	/**
	 * Executes the given request on the current HTTP client, passing the entity content of the response to the given
	 * handler.
	 * 
	 * @return true if there are more pages to execute in a paged get.
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	private boolean execute(HttpUriRequest request, RemoteBggHandler handler) throws IOException,
		XmlPullParserException {
		LOGI(TAG, request.getURI().toString());

		HttpResponse response;
		response = mHttpClient.execute(request);
		final int status = response.getStatusLine().getStatusCode();

		if (status != HttpStatus.SC_OK) {
			throw new IOException("Unexpected server response " + response.getStatusLine() + " for "
				+ request.getRequestLine());
		}

		final InputStream input = response.getEntity().getContent();
		try {
			XmlPullParser pullParser = createPullParser(input);
			return handler.parseAndHandle(pullParser, mContext);
		} finally {
			if (input != null) {
				input.close();
			}
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
