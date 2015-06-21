package com.boardgamegeek.export.model;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.PlayerColors;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

public class User {
    public static String[] PROJECTION = new String[]{
            Buddies.BUDDY_NAME,
    };

    private static final int BUDDY_NAME = 0;

    @Expose private String name;
    @Expose private List<PlayerColor> colors;

    public String getName() {
        return name;
    }

    public boolean hasColors() {
        return colors != null && colors.size() > 0;
    }

    public List<PlayerColor> getColors() {
        return colors;
    }

    public static User fromCursor(Cursor cursor) {
        User user = new User();
        user.name = cursor.getString(BUDDY_NAME);
        return user;
    }

    public void addColors(Context context) {
        colors = new ArrayList<>();

        final Cursor cursor = context.getContentResolver().query(
                PlayerColors.buildUserUri(name),
                PlayerColor.PROJECTION,
                null, null, null);

        if (cursor == null) {
            return;
        }

        try {
            while (cursor.moveToNext()) {
                PlayerColor color = PlayerColor.fromCursor(cursor);
                colors.add(color);
            }
        } finally {
            cursor.close();
        }
    }
}
