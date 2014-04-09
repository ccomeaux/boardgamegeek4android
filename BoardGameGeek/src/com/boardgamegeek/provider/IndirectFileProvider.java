package com.boardgamegeek.provider;

import android.content.Context;
import android.net.Uri;

import com.boardgamegeek.util.FileUtils;
import com.boardgamegeek.util.ResolverUtils;

public abstract class IndirectFileProvider extends BaseFileProvider {
	protected abstract Uri getFileUri(Uri uri);

	protected abstract String getColumnName();

	@Override
	protected String generateFileName(Context context, Uri uri) {
		String url = ResolverUtils.queryString(context.getContentResolver(), getFileUri(uri), getColumnName());
		return FileUtils.getFileNameFromUrl(url);
	}
}
