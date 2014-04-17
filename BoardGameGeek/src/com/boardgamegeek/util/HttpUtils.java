package com.boardgamegeek.util;

import static com.boardgamegeek.util.LogUtils.LOGD;
import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HttpContext;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.boardgamegeek.auth.Authenticator;

public class HttpUtils {
	private static final String TAG = makeLogTag(HttpUtils.class);

	public static final String SITE_URL = "http://www.boardgamegeek.com/";
	public static final String BASE_URL = SITE_URL + "xmlapi/";
	public static final String BASE_URL_2 = SITE_URL + "xmlapi2/";

	private static final int TIMEOUT_SECS = 60;
	private static final int BUFFER_SIZE = 8192;
	private static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
	private static final String ENCODING_GZIP = "gzip";
	private static boolean mMockLogin = false;

	public static String constructSearchUrl(String searchTerm, boolean useExact) {
		// http://boardgamegeek.com/xmlapi/search?search=puerto+rico
		// http://boardgamegeek.com/xmlapi2/search?type=boardgame&query=puerto+rico
		String queryUrl = BASE_URL + "search?search=" + encode(searchTerm);
		if (useExact) {
			queryUrl += "&exact=1";
		}
		LOGD(TAG, "Query: " + queryUrl);
		return queryUrl;
	}

	public static String constructHotnessUrl() {
		// http://www.boardgamegeek.com/xmlapi2/hot
		return BASE_URL_2 + "hot";
	}

	public static String constructDesignerUrl(int designerId) {
		// http://www.boardgamegeek.com/xmlapi/designer/13
		return BASE_URL + "designer/" + designerId;
	}

	public static String constructArtistUrl(int artistId) {
		// http://www.boardgamegeek.com/xmlapi/boardgameartist/9798
		return BASE_URL + "boardgameartist/" + artistId;
	}

	public static String constructPublisherUrl(int publisherId) {
		// http://www.boardgamegeek.com/xmlapi/publisher/10
		return BASE_URL + "publisher/" + publisherId;
	}

	public static String constructForumUrl(int forumId, int page) {
		return BASE_URL_2 + "forum?id=" + forumId + "&page=" + page;
	}

	public static String constructThreadUrl(int mThreadId) {
		return BASE_URL_2 + "thread?id=" + mThreadId;
	}

	public static HttpClient createHttpClient(Context context, String username, String authToken,
		long authTokenExpiration, boolean useGzip) {
		final HttpParams params = createHttpParams(context, useGzip);
		final DefaultHttpClient client = new DefaultHttpClient(params);
		client.setCookieStore(createCookieStore(username, authToken, authTokenExpiration));
		if (useGzip) {
			addGzipInterceptors(client);
		}
		return client;
	}

	private static CookieStore createCookieStore(String username, String password, long expiration) {
		BasicCookieStore cookieStore = new BasicCookieStore();

		BasicClientCookie nameCookie = new BasicClientCookie("bggusername", username);
		nameCookie.setDomain(".boardgamegeek.com");
		nameCookie.setPath("/");
		nameCookie.setExpiryDate(new Date(expiration));
		cookieStore.addCookie(nameCookie);

		BasicClientCookie pwCookie = new BasicClientCookie("bggpassword", password);
		pwCookie.setDomain(".boardgamegeek.com");
		pwCookie.setPath("/");
		pwCookie.setExpiryDate(new Date(expiration));
		cookieStore.addCookie(pwCookie);

		return cookieStore;
	}

	public static HttpClient createHttpClient(Context context, boolean useGzip) {
		final HttpParams params = createHttpParams(context, useGzip);
		final DefaultHttpClient client = new DefaultHttpClient(params);
		if (useGzip) {
			addGzipInterceptors(client);
		}
		return client;
	}

	public static HttpClient createHttpClientWithAuth(Context context, boolean useGzip, boolean requireAuth)
		throws OperationCanceledException, AuthenticatorException, IOException {
		AccountManager accountManager = AccountManager.get(context);
		Account account = Authenticator.getAccount(accountManager);
		String authToken = accountManager.blockingGetAuthToken(account, Authenticator.AUTHTOKEN_TYPE, true);

		HttpClient httpClient = null;
		if (account == null || (TextUtils.isEmpty(authToken))) {
			if (!requireAuth) {
				httpClient = HttpUtils.createHttpClient(context, useGzip);
			}
		} else {
			long expiry = 0;
			final String userData = accountManager.getUserData(account, Authenticator.KEY_AUTHTOKEN_EXPIRY);
			if (!TextUtils.isEmpty(userData)) {
				expiry = Long.parseLong(userData);
			}
			httpClient = HttpUtils.createHttpClient(context, account.name, authToken, expiry, useGzip);
		}
		return httpClient;
	}

	public static HttpClient createHttpClientWithAuthSafely(Context context, boolean useGzip, boolean requireAuth) {
		try {
			return createHttpClientWithAuth(context, useGzip, requireAuth);
		} catch (OperationCanceledException e) {
			LOGE(TAG, "Couldn't create an HTTP client", e);
		} catch (AuthenticatorException e) {
			LOGE(TAG, "Couldn't create an HTTP client", e);
		} catch (IOException e) {
			LOGE(TAG, "Couldn't create an HTTP client", e);
		}
		return null;
	}

	private static HttpParams createHttpParams(Context context, boolean useGzip) {
		final HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(params, TIMEOUT_SECS * (int) DateUtils.SECOND_IN_MILLIS);
		HttpConnectionParams.setSoTimeout(params, TIMEOUT_SECS * (int) DateUtils.SECOND_IN_MILLIS);
		HttpConnectionParams.setSocketBufferSize(params, BUFFER_SIZE);
		if (useGzip) {
			HttpProtocolParams.setUserAgent(params, buildUserAgent(context));
		}
		return params;
	}

	private static String buildUserAgent(Context context) {
		try {
			final PackageManager manager = context.getPackageManager();
			final PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);

			// Some APIs require "(gzip)" in the user-agent string.
			return info.packageName + "/" + info.versionName + " (" + info.versionCode + ") (gzip)";
		} catch (NameNotFoundException e) {
			return null;
		}
	}

	private static void addGzipInterceptors(DefaultHttpClient client) {
		client.addRequestInterceptor(new HttpRequestInterceptor() {
			public void process(HttpRequest request, HttpContext context) {
				// Add header to accept gzip content
				if (!request.containsHeader(HEADER_ACCEPT_ENCODING)) {
					request.addHeader(HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
				}
			}
		});

		client.addResponseInterceptor(new HttpResponseInterceptor() {
			public void process(HttpResponse response, HttpContext context) {
				// Inflate any responses compressed with gzip
				final HttpEntity entity = response.getEntity();
				final Header encoding = entity.getContentEncoding();
				if (encoding != null) {
					for (HeaderElement element : encoding.getElements()) {
						if (element.getName().equalsIgnoreCase(ENCODING_GZIP)) {
							response.setEntity(new InflatingEntity(response.getEntity()));
							break;
						}
					}
				}
			}
		});
	}

	private static class InflatingEntity extends HttpEntityWrapper {
		public InflatingEntity(HttpEntity wrapped) {
			super(wrapped);
		}

		@Override
		public InputStream getContent() throws IOException {
			return new GZIPInputStream(wrappedEntity.getContent());
		}

		@Override
		public long getContentLength() {
			return -1;
		}
	}

	/**
	 * Parses an HttpResponse to a string.
	 */
	public static String parseResponse(HttpResponse response) throws IOException {
		if (response == null) {
			return "";
		}

		final HttpEntity entity = response.getEntity();
		if (entity == null) {
			return "";
		}

		final InputStream stream = entity.getContent();
		if (stream == null) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		try {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				LOGE(TAG, "Parsing response", e);
				return "";
			}
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line).append('\n');
			}
		} finally {
			stream.close();
		}
		return sb.toString().trim();
	}

	public static String encode(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			LOGE(TAG, "What do you mean UTF-8 isn't supported?!", e);
		}
		return s;
	}

	/**
	 * Workaround for bug pre-Froyo, see here for more info:
	 * http://android-developers.blogspot.com/2011/09/androids-http-clients.html
	 */
	public static void disableConnectionReuseIfNecessary() {
		// HTTP connection reuse which was buggy pre-froyo
		if (hasHttpConnectionBug()) {
			System.setProperty("http.keepAlive", "false");
		}
	}

	/**
	 * Check if OS version has a http URLConnection bug. See here for more information:
	 * http://android-developers.blogspot.com/2011/09/androids-http-clients.html
	 * 
	 * @return true if this OS version is affected, false otherwise
	 */
	public static boolean hasHttpConnectionBug() {
		return !VersionUtils.hasFroyo();
	}

	public static final int HTTP_REQUEST_TIMEOUT_MS = 30 * 1000;

	/**
	 * Configures the httpClient to connect to the URL provided.
	 */
	public static HttpClient getHttpClient() {
		HttpClient httpClient = new DefaultHttpClient();
		final HttpParams params = httpClient.getParams();
		HttpConnectionParams.setConnectionTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
		HttpConnectionParams.setSoTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
		ConnManagerParams.setTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
		return httpClient;
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
	 * Mocks a login by setting the cookie store with bogus data.
	 */
	private static CookieStore mockLogin(String username) {
		CookieStore store = new BasicCookieStore();
		store.addCookie(new BasicClientCookie("bggpassword", "password"));
		store.addCookie(new BasicClientCookie("SessionID", "token"));
		return store;
	}
}
