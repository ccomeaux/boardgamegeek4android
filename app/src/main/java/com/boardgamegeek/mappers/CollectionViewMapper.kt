package com.boardgamegeek.mappers

import android.content.Context
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionViewShortcutEntity
import com.boardgamegeek.extensions.truncate
import com.boardgamegeek.ui.CollectionActivity
import com.boardgamegeek.util.ShortcutUtils

@RequiresApi(Build.VERSION_CODES.N_MR1)
fun CollectionViewShortcutEntity.map(context: Context): ShortcutInfo {
    return ShortcutInfo.Builder(context, createShortcutName(viewId))
            .setShortLabel(name.truncate(ShortcutUtils.SHORT_LABEL_LENGTH))
            .setLongLabel(name.truncate(ShortcutUtils.LONG_LABEL_LENGTH))
            .setIcon(Icon.createWithResource(context, R.drawable.ic_shortcut_ic_collection))
            .setIntent(CollectionActivity.createIntentAsShortcut(context, viewId))
            .build()
}

fun createShortcutName(viewId: Long) = "collection-view-$viewId"
