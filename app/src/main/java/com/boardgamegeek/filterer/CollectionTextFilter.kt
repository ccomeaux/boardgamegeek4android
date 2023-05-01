package com.boardgamegeek.filterer

import android.content.Context
import com.boardgamegeek.R

abstract class CollectionTextFilter(context: Context) : CollectionFilterer(context) {
    var textOperator = TextOperator.default
    var filterText = ""
    var ignoreCase = true

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

    protected fun chipText(prefix: String): String {
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

    protected fun description(prefix: String): String {
        //val ignoreCaseText = if (ignoreCase) "~" else ""
        return "$prefix " + when (textOperator) {
            TextOperator.Is -> context.getString(R.string.is_prefix, filterText)
            TextOperator.IsNot -> context.getString(R.string.is_not_prefix, filterText)
            TextOperator.Contains -> context.getString(R.string.contains_prefix, filterText)
            TextOperator.DoesNotContain -> context.getString(R.string.does_not_contain_prefix, filterText)
            TextOperator.StartsWith -> context.getString(R.string.starts_with_prefix, filterText)
            TextOperator.EndsWith -> context.getString(R.string.ends_with_prefix, filterText)
            TextOperator.IsEmpty -> context.getString(R.string.is_empty)
            TextOperator.IsNotEmpty -> context.getString(R.string.is_not_empty)
        }
    }

    protected fun filterByText(filterField: String): Boolean {
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
