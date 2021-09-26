package com.boardgamegeek.util

import java.util.*

/**
 * Static methods for modifying and applying colors to views.
 */
object ColorUtils {
    private val limitedColorNameList = arrayListOf(
            "Red" to 0xFF_FF_00_00.toInt(),
            "Yellow" to 0xFF_FF_FF_00.toInt(),
            "Blue" to 0xFF_00_00_FF.toInt(),
            "Green" to 0xFF_00_80_00.toInt(),
            "Purple" to 0xFF_80_00_80.toInt(),
            "Orange" to 0xFF_E5_94_00.toInt(),
            "White" to 0xFF_FF_FF_FF.toInt(),
            "Black" to 0xFF_00_00_00.toInt(),
            "Natural" to 0xFF_E9_C2_A6.toInt(),
            "Brown" to 0xFF_A5_2A_2A.toInt()
    )

    private val colorNameList = limitedColorNameList.toMutableList() + arrayListOf(
            "Tan" to 0xFF_DB_93_70.toInt(),
            "Gray" to 0xFF_88_88_88.toInt(),
            "Gold" to 0xFF_FF_D7_00.toInt(),
            "Silver" to 0xFF_C0_C0_C0.toInt(),
            "Bronze" to 0xFF_8C_78_53.toInt(),
            "Ivory" to 0xFF_FF_FF_F0.toInt(),
            "Rose" to 0xFF_FF_00_7F.toInt(),
            "Pink" to 0xFF_CD_91_9E.toInt(),
            "Teal" to 0xFF_00_80_80.toInt()
    )
    // "Light Gray" to 0xFF_CC_CC_CC
    // "Dark Gray" to 0xFF_44_44_44
    // "Cyan" = 0xFF_00_FF_FF
    // "Magenta"" = 0xFF_FF_00_FF
    // "Aqua" to 0xFF_66_CC_CC

    private val colorNameMutableMap = colorNameList.map { formatKey(it.first) to it.second } .toMap()

    val limitedColorList: MutableList<Pair<String, Int>>
        get() = limitedColorNameList.toMutableList()

    val colorList: MutableList<Pair<String, Int>>
        get() = colorNameList.toMutableList()

    val colorNameMap
        get() = colorNameMutableMap.toMap()

    fun formatKey(name: String): String {
        return name.toLowerCase(Locale.US)
    }
}
