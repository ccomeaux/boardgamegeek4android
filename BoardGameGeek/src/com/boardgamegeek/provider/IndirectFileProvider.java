package com.boardgamegeek.provider;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.boardgamegeek.database.ResolverUtils;

public abstract class IndirectFileProvider extends BaseFileProvider {
	protected abstract Uri getFileUri(Uri uri);

	protected abstract String getColumnName();

	@Override
	protected String generateFileName(Context context, Uri uri) {
		String url = ResolverUtils.queryString(context.getContentResolver(), getFileUri(uri), getColumnName());
		if (!TextUtils.isEmpty(url) && !BggContract.INVALID_URL.equals(url)) {
			int i = url.lastIndexOf('/');
			if (i > 0) {
				return url.substring(i + 1);
			}
		}
		return null;
	}
}
