package com.boardgamegeek.export;

import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.boardgamegeek.export.model.PlayerColor;
import com.boardgamegeek.export.model.User;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.PlayerColors;
import com.boardgamegeek.util.ResolverUtils;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.util.ArrayList;

public class UserImportTask extends JsonImportTask<User> {
	public UserImportTask(Context context, Uri uri) {
		super(context, Constants.TYPE_USERS, uri);
	}

	@Override
	protected void importRecord(Context context, Gson gson, JsonReader reader) {
		User user = gson.fromJson(reader, User.class);

		ContentResolver resolver = context.getContentResolver();

		String name = user.getName();
		if (ResolverUtils.rowExists(resolver, Buddies.buildBuddyUri(name))) {

			ArrayList<ContentProviderOperation> batch = new ArrayList<>();
			for (PlayerColor color : user.getColors()) {
				if (!TextUtils.isEmpty(color.getColor())) {

					Builder builder;
					final int sortOrder = color.getSort();
					if (ResolverUtils.rowExists(resolver, PlayerColors.buildUserUri(name, sortOrder))) {
						builder = ContentProviderOperation.newUpdate(PlayerColors.buildUserUri(name, sortOrder));
					} else {
						builder = ContentProviderOperation.newInsert(PlayerColors.buildUserUri(name))
							.withValue(PlayerColors.PLAYER_COLOR_SORT_ORDER, sortOrder);
					}
					batch.add(builder.withValue(PlayerColors.PLAYER_COLOR, color.getColor()).build());
				}
			}

			ResolverUtils.applyBatch(context, batch);
		}
	}
}
