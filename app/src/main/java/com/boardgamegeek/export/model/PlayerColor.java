package com.boardgamegeek.export.model;

import android.database.Cursor;

import com.boardgamegeek.provider.BggContract.PlayerColors;
import com.google.gson.annotations.Expose;

public class PlayerColor {
    public static String[] PROJECTION = new String[]{
            PlayerColors._ID,
            PlayerColors.PLAYER_COLOR_SORT_ORDER,
            PlayerColors.PLAYER_COLOR
    };

    private static final int SORT = 1;
    private static final int COLOR = 2;

    private int id;
    @Expose private int sort;
    @Expose private String color;

    public static PlayerColor fromCursor(Cursor cursor) {
        PlayerColor pc = new PlayerColor();
        pc.sort = cursor.getInt(SORT);
        pc.color = cursor.getString(COLOR);
        return pc;
    }

    public int getSort() {
        return sort;
    }

    public String getColor() {
        return color;
    }
}
