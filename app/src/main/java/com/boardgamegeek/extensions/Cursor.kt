@file:Suppress("NOTHING_TO_INLINE")

package com.boardgamegeek.extensions

import android.database.Cursor

inline fun Cursor.getBoolean(index: Int) = if (isNull(index)) false else getInt(index) == 1

inline fun Cursor.getBooleanOrNull(index: Int) = if (isNull(index)) null else getInt(index) == 1
