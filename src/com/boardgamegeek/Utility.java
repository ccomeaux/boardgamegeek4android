package com.boardgamegeek;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;

public final class Utility {

	public final static String siteUrl = "http://www.boardgamegeek.com/";

	private final static String LOG_TAG = "BoardGameGeek";
	private static final int IO_BUFFER_SIZE = 4 * 1024;

	// prevent instantiation
	private Utility() {}

	// prepares text to be part of a where clause in a query
	public static String querifyText(String query) {
		return query.replace("'", "''");
	}


	public static String EncodeAsUrl(String s) {
		// converts any accented characters into standard equivalents
		// and replaces spaces with +

		if (TextUtils.isEmpty(s)) {
			return null;
		}

		final String PLAIN_ASCII = "AaEeIiOoUu" // grave
			+ "AaEeIiOoUuYy" // acute
			+ "AaEeIiOoUuYy" // circumflex
			+ "AaOoNn" // tilde
			+ "AaEeIiOoUuYy" // umlaut
			+ "Aa" // ring
			+ "Cc" // cedilla
			+ "OoUu" // double acute
			+ "+" // space
		;

		final String UNICODE = "\u00C0\u00E0\u00C8\u00E8\u00CC\u00EC\u00D2\u00F2\u00D9\u00F9"
			+ "\u00C1\u00E1\u00C9\u00E9\u00CD\u00ED\u00D3\u00F3\u00DA\u00FA\u00DD\u00FD"
			+ "\u00C2\u00E2\u00CA\u00EA\u00CE\u00EE\u00D4\u00F4\u00DB\u00FB\u0176\u0177"
			+ "\u00C3\u00E3\u00D5\u00F5\u00D1\u00F1"
			+ "\u00C4\u00E4\u00CB\u00EB\u00CF\u00EF\u00D6\u00F6\u00DC\u00FC\u0178\u00FF" + "\u00C5\u00E5"
			+ "\u00C7\u00E7" + "\u0150\u0151\u0170\u0171" + " ";

		StringBuilder sb = new StringBuilder();
		int n = s.length();
		for (int i = 0; i < n; i++) {
			char c = s.charAt(i);
			int pos = UNICODE.indexOf(c);
			if (pos > -1) {
				sb.append(PLAIN_ASCII.charAt(pos));
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	public static int parseInt(String text) {
		return parseInt(text, 0);
	}

	public static int parseInt(String text, int defaultValue) {
		try {
			return Integer.parseInt(text);
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
	}

	public static double parseDouble(String text) {
		return parseDouble(text, 0);
	}

	public static double parseDouble(String text, double defaultValue) {
		try {
			return Double.parseDouble(text);
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
	}

	public static Drawable getImage(String url) {
		if (TextUtils.isEmpty(url)) {
			return null;
		}

		try {
			// connect to URL and open input stream
			URL imageURL = new URL(url);
			InputStream inputStream = (InputStream) imageURL.getContent();
			BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream, IO_BUFFER_SIZE);

			// open output stream and copy from input stream
			// this is to workaround a persistent "jpeg error 91" bug
			// solution per
			// http://groups.google.com/group/android-developers/browse_thread/thread/4ed17d7e48899b26/a15129024bb845bf?show_docid=a15129024bb845bf
			final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream, IO_BUFFER_SIZE);
			copy(bufferedInputStream, bufferedOutputStream);
			bufferedOutputStream.flush();

			// get bitmap and convert to drawable
			// if we didn't have to deal with "jpeg error 91" I think we'd only
			// need one line:
			// Drawable thumbnail = Drawable.createFromStream(inputstream,
			// "src");
			final byte[] data = outputStream.toByteArray();
			Bitmap thumbnailBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
			BitmapDrawable thumbnailDrawable = new BitmapDrawable(thumbnailBitmap);

			// close input stream
			bufferedInputStream.close();
			inputStream.close();

			// return drawable
			return thumbnailDrawable;
		} catch (MalformedURLException e) {
			Log.w(LOG_TAG, "Malformed URL: " + url, e);
			return null;
		} catch (IOException e) {
			Log.w(LOG_TAG, "IOException", e);
			return null;
		}
	}

	private static void copy(InputStream in, OutputStream out) throws IOException {
		byte[] b = new byte[IO_BUFFER_SIZE];
		int read;
		while ((read = in.read(b)) != -1) {
			out.write(b, 0, read);
		}
	}

	public static String getVersionDescription(Context context) {
		try {
			PackageManager pm = context.getPackageManager();
			PackageInfo pInfo = pm.getPackageInfo(context.getPackageName(), 0);
			return "Version " + pInfo.versionName;
		} catch (NameNotFoundException e) {
			Log.e(LOG_TAG, "NameNotFoundException in getVersion", e);
		}
		return "";
	}

	public static byte[] ConvertToByteArry(Drawable drawable) {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		BitmapDrawable bm = (BitmapDrawable) drawable;
		bm.getBitmap().compress(Bitmap.CompressFormat.JPEG, 100, stream);
		byte[] byteArray = stream.toByteArray();
		try {
			stream.close();
		} catch (IOException e) {
			Log.e(LOG_TAG, e.getMessage());
		}
		return byteArray;
	}

	public static String parseResponse(HttpResponse response) throws IOException {
		if (response == null) {
			return null;
		}

		final HttpEntity entity = response.getEntity();
		if (entity == null) {
			return null;
		}

		final InputStream stream = entity.getContent();
		if (stream == null) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		try {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				Log.w(LOG_TAG, e.toString());
				return null;
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

	/**
	 * Gets the ordinal (1st) for the given cardinal (1)
	 * @param cardinal
	 * @return
	 */
	public static String getOrdinal(int cardinal) {

		if (cardinal < 0) {
			return "";
		}

		String c = "" + cardinal;
		String n = "0";
		if (c.length() > 1) {
			n = c.substring(c.length() - 2, c.length() - 1);
		}
		String l = c.substring(c.length() - 1);
		if (!n.equals("1")) {
			if (l.equals("1")) {
				return c + "st";
			} else if (l.equals("2")) {
				return c + "nd";
			} else if (l.equals("3")) {
				return c + "rd";
			}
		}
		return c + "th";
	}
}
