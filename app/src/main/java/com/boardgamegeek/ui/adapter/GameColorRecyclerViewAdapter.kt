package com.boardgamegeek.ui.adapter

import android.annotation.SuppressLint
import android.util.SparseBooleanArray
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.util.size
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.compose.ListItemPrimaryText
import com.boardgamegeek.ui.compose.ListItemDefaults
import com.boardgamegeek.ui.theme.BggAppTheme
import kotlin.properties.Delegates

class GameColorRecyclerViewAdapter(private val callback: Callback?) :
    RecyclerView.Adapter<GameColorRecyclerViewAdapter.ViewHolder>(), AutoUpdatableAdapter {
    private val selectedItems = SparseBooleanArray()

    var colors: List<String> by Delegates.observable(emptyList()) { _, old, new ->
        autoNotify(old, new) { o, n -> o == n }
    }

    interface Callback {
        fun onItemClick(position: Int)
        fun onItemLongPress(position: Int): Boolean
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ComposeView(parent.context))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return colors.size
    }

    inner class ViewHolder(private val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
        fun bind(position: Int) {
            getColorName(position)?.let { colorName ->
                composeView.setContent {
                    ColorListItem(
                        colorName = colorName,
                        isSelected = selectedItems[position, false],
                        onClick = { callback?.onItemClick(position) },
                        onLongClick = { callback?.onItemLongPress(position) ?: false }
                    )
                }
            }
        }
    }

    fun getColorName(position: Int): String? {
        return colors.getOrNull(position)
    }

    fun toggleSelection(position: Int) {
        selectedItems.toggle(position)
        notifyItemChanged(position)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearSelections() {
        selectedItems.clear()
        notifyDataSetChanged()
    }

    val selectedItemCount
        get() = selectedItems.size

    fun getSelectedColors(): List<String> {
        return selectedItems.filterTrue().mapNotNull { getColorName(it) }
    }
}

@Composable
private fun ColorListItem(
    colorName: String,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface
            )
            .combinedClickable(
                onLongClick = onLongClick,
            ) {
                onClick()
            }
            .padding(ListItemDefaults.paddingValues),
    ) {
        val colorRgb = colorName.asColorRgb()
        Box(
            modifier = Modifier
                .padding(end = 16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(colorRgb))
                .border(1.dp, Color(colorRgb.darkenColor(0.75)), CircleShape)
        )
        ListItemPrimaryText(colorName, isSelected = true)
    }
}

@Preview
@Composable
private fun ColorListItemPreview() {
    BggAppTheme {
        Column {
            BggColors.colorList.forEachIndexed { index, color ->
                ColorListItem(colorName = color.first, isSelected = ((index % 4) == 3))
            }
        }
    }
}
