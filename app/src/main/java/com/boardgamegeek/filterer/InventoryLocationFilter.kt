package com.boardgamegeek.filterer

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity

class InventoryLocationFilter(context: Context) : CollectionFilterer(context) {
    var textOperator = TextOperator.default
    var filterText = ""
    var ignoreCase = true

    override val typeResourceId = R.string.collection_filter_type_inventory_location

    override fun inflate(data: String) {
        val d = data.split(DELIMITER)
        // val version = d.getOrNull(0)?.toIntOrNull() ?: 1
        textOperator = d.getOrNull(1)?.toIntOrNull()?.let {
            TextOperator.getByKey(it)
        } ?: TextOperator.default
        filterText = d.getOrNull(2).orEmpty()
        ignoreCase = d.getOrNull(3) == "1"
    }

    override fun deflate() = "1$DELIMITER${textOperator.key}$DELIMITER$filterText$DELIMITER" + if (ignoreCase) "1" else "0"

    override val iconResourceId: Int
        get() = R.drawable.ic_baseline_location_on_24

    override fun chipText(): String {
        val prefix = context.getString(R.string.at)
        val ignoreCaseText = if (ignoreCase) "~" else ""
        return "$prefix $ignoreCaseText" + when (textOperator) {
            TextOperator.Is -> "'$filterText'"
            TextOperator.IsNot -> "!'$filterText'"
            TextOperator.Contains -> "'*$filterText*'"
            TextOperator.DoesNotContain -> "!'*$filterText*'"
            TextOperator.StartsWith -> "'$filterText*'"
            TextOperator.EndsWith -> "'*$filterText'"
            TextOperator.IsEmpty -> "''"
            TextOperator.IsNotEmpty -> "!''"
        }
    }

    override fun description(): String {
        val prefix = "Inventory Location "
        return prefix + when (textOperator) {
            TextOperator.Is -> "is '$filterText'"
            TextOperator.IsNot -> "is not '$filterText'"
            TextOperator.Contains -> "contains '$filterText'"
            TextOperator.DoesNotContain -> "does not contain '$filterText'"
            TextOperator.StartsWith -> "starts with '$filterText'"
            TextOperator.EndsWith -> "ends with '$filterText'"
            TextOperator.IsEmpty -> "is empty"
            TextOperator.IsNotEmpty -> "is not empty" // context.getString(R.string.is_not_empty)
        }
        // TODO include ignore case
    }

    override fun filter(item: CollectionItemEntity): Boolean {
        val filterField = item.inventoryLocation
        return when (textOperator) {
            TextOperator.Is -> filterField.equals(filterText, ignoreCase)
            TextOperator.IsNot -> !filterField.equals(filterText, ignoreCase)
            TextOperator.Contains -> filterField.contains(filterText, ignoreCase)
            TextOperator.DoesNotContain -> !filterField.contains(filterText, ignoreCase)
            TextOperator.StartsWith -> filterField.startsWith(filterText, ignoreCase)
            TextOperator.EndsWith -> filterField.endsWith(filterText, ignoreCase)
            TextOperator.IsEmpty -> filterField.isBlank()
            TextOperator.IsNotEmpty -> filterField.isNotBlank()
        }
    }
}
