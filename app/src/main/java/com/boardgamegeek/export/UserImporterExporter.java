package com.boardgamegeek.export;

import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.boardgamegeek.export.model.Color;
import com.boardgamegeek.export.model.User;
import com.boardgamegeek.export.model.PlayerColor;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.GameColors;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.PlayerColors;
import com.boardgamegeek.util.ResolverUtils;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.util.ArrayList;
import java.util.List;

public class UserImporterExporter implements ImporterExporter {
    @Override
    public String getFileName() {
        return "users.json";
    }

    @Override
    public Cursor getCursor(Context context) {
        return context.getContentResolver().query(
                Buddies.CONTENT_URI,
                User.PROJECTION,
                null, null, null);
    }

    @Override
    public void writeJsonRecord(Context context, Cursor cursor, Gson gson, JsonWriter writer) {
        User user = User.fromCursor(cursor);
        user.addColors(context);
        if (user.hasColors()) {
            gson.toJson(user, User.class, writer);
        }
    }

    @Override
    public void initializeImport(Context context) {
    }

    @Override
    public void importRecord(Context context, Gson gson, JsonReader reader) {
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