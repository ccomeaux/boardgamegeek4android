package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.PlayerColors;
import com.boardgamegeek.provider.BggDatabase.Tables;

public class PlayerColorsProvider extends BasicProvider {
    @Override
    protected String getDefaultSortOrder() {
        return PlayerColors.DEFAULT_SORT;
    }

    @Override
    protected String getPath() {
        return BggContract.PATH_PLAYER_COLORS;
    }

    @Override
    public String getTable() {
        return Tables.PLAYER_COLORS;
    }

    @Override
    protected String getType(Uri uri) {
        return PlayerColors.CONTENT_TYPE;
    }
}
