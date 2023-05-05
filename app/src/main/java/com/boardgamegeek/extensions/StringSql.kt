@file:Suppress("NOTHING_TO_INLINE", "SpellCheckingInspection")

package com.boardgamegeek.extensions

inline fun String.ascending() = this.plus(" ASC")

inline fun String.descending() = this.plus(" DESC")

inline fun String.collateNoCase() = this.plus(" COLLATE NOCASE")

inline fun String.isTrue() = this.plus("=1")

inline fun String.greaterThanZero() = this.plus(">0")

inline fun String.notBlank() = "$this<>''"

inline fun String.whereZeroOrNull() = "($this=0 OR $this IS NULL)"

inline fun String.whereNotZeroOrNull() = "($this>0 OR $this IS NOT NULL)"

inline fun String.whereEqualsOrNull() = "($this=? OR $this IS NULL)"

inline fun String.whereNotEqualsOrNull() = "($this!=? OR $this IS NULL)"

inline fun String.whereNullOrBlank() = "($this IS NULL OR $this='')"
