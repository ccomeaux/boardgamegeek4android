package com.boardgamegeek.ui

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.entities.GeekListItemEntity
import com.boardgamegeek.extensions.setWebViewText
import com.boardgamegeek.util.XmlApiMarkupConverter
import kotlinx.android.synthetic.main.fragment_geeklist_item.*
import org.jetbrains.anko.support.v4.withArguments

class GeekListItemFragment : Fragment(R.layout.fragment_geeklist_item) {
    private var order = 0
    private var geekListTitle = ""
    private var glItem = GeekListItemEntity()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            order = it.getInt(KEY_ORDER, 0)
            geekListTitle = it.getString(KEY_TITLE).orEmpty()
            glItem = it.getParcelable(KEY_ITEM) ?: GeekListItemEntity()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val markupConverter = XmlApiMarkupConverter(requireContext())
        orderView.text = order.toString()
        geekListTitleView.text = geekListTitle
        typeView.text = glItem.objectTypeDescription(requireContext())
        usernameView.text = glItem.username
        thumbsView.text = glItem.numberOfThumbs.toString()
        bodyView.setWebViewText(markupConverter.toHtml(glItem.body))
        postedDateView.timestamp = glItem.postDateTime
        editedDateView.timestamp = glItem.editDateTime
        datetimeDividerView.isVisible = glItem.editDateTime != glItem.postDateTime
        editedDateView.isVisible = glItem.editDateTime != glItem.postDateTime
    }

    companion object {
        private const val KEY_ORDER = "GEEK_LIST_ORDER"
        private const val KEY_TITLE = "GEEK_LIST_TITLE"
        private const val KEY_ITEM = "ITEM"

        fun newInstance(order: Int, title: String, item: GeekListItemEntity): GeekListItemFragment {
            return GeekListItemFragment().withArguments(
                    KEY_ORDER to order,
                    KEY_TITLE to title,
                    KEY_ITEM to item
            )
        }
    }
}