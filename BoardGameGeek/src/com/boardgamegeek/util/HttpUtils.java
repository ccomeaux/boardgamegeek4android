package com.boardgamegeek.util;

import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

public class HttpUtils {
	private static final String TAG = makeLogTag(HttpUtils.class);
	private static final int HTTP_REQUEST_TIMEOUT_MS = 30 * 1000;
	private static final String SITE_URL = "http://www.boardgamegeek.com/";

	private static boolean mMockLogin = false;

	public static String encode(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			LOGE(TAG, "What do you mean UTF-8 isn't supported?!", e);
		}
		return s;
	}

	public static CookieStore authenticate(String username, String password) {
		if (mMockLogin) {
			return mockLogin(username);
		}

		String AUTH_URI = HttpUtils.SITE_URL + "login";

		final HttpResponse resp;
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("username", username));
		params.add(new BasicNameValuePair("password", password));

		final HttpEntity entity;
		try {
			entity = new UrlEncodedFormEntity(params);
		} catch (final UnsupportedEncodingException e) {
			// this should never happen.
			throw new IllegalStateException(e);
		}
		LOGI(TAG, "Authenticating to: " + AUTH_URI);
		final HttpPost post = new HttpPost(AUTH_URI);
		post.addHeader(entity.getContentType());
		post.setEntity(entity);
		try {
			final DefaultHttpClient client = (DefaultHttpClient) getHttpClient();
			resp = client.execute(post);
			LOGW(TAG, resp.toString());
			CookieStore cookieStore = null;
			int code = resp.getStatusLine().getStatusCode();
			if (code == HttpStatus.SC_OK) {
				List<Cookie> cookies = client.getCookieStore().getCookies();
				if (cookies == null || cookies.isEmpty()) {
					LOGW(TAG, "missing cookies");
				} else {
					for (Cookie cookie : cookies) {
						if (cookie.getName().equals("bggpassword")) {
							cookieStore = client.getCookieStore();
							break;
						}
					}
				}
			} else {
				LOGW(TAG, "Bad response code - " + code);
			}
			if (cookieStore != null) {
				LOGW(TAG, "Successful authentication");
				return cookieStore;
			} else {
				LOGW(TAG, "Error authenticating - " + resp.getStatusLine());
				return null;
			}
		} catch (final IOException e) {
			LOGW(TAG, "IOException when getting authtoken", e);
			return null;
		} finally {
			LOGW(TAG, "Authenticate complete");
		}
	}

	/**
	 * Configures the httpClient to connect to the URL provided.
	 */
	private static HttpClient getHttpClient() {
		HttpClient httpClient = new DefaultHttpClient();
		final HttpParams params = httpClient.getParams();
		HttpConnectionParams.setConnectionTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
		HttpConnectionParams.setSoTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
		ConnManagerParams.setTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
		return httpClient;
	}

	/**
	 * Mocks a login by setting the cookie store with bogus data.
	 */
	private static CookieStore mockLogin(String username) {
		CookieStore store = new BasicCookieStore();
		store.addCookie(new BasicClientCookie("bggpassword", "password"));
		store.addCookie(new BasicClientCookie("SessionID", "token"));
		return store;
	}

	public static String ensureScheme(String url) {
		if (url.startsWith("//")) {
			return "http:" + url;
		}
		return url;
	}
}
