package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentGeeklistItemBinding
import com.boardgamegeek.extensions.getParcelableCompat
import com.boardgamegeek.extensions.setWebViewText
import com.boardgamegeek.model.GeekListItem
import com.boardgamegeek.util.XmlApiMarkupConverter

class GeekListItemFragment : Fragment() {
    private var _binding: FragmentGeeklistItemBinding? = null
    private val binding get() = _binding!!
    private var order = 0
    private var geekListTitle = ""
    private var geekListItem = GeekListItem()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            order = it.getInt(KEY_ORDER, 0)
            geekListTitle = it.getString(KEY_TITLE).orEmpty()
            geekListItem = it.getParcelableCompat(KEY_ITEM) ?: GeekListItem()
        }
    }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentGeeklistItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val markupConverter = XmlApiMarkupConverter(requireContext())
        binding.orderView.text = order.toString()
        binding.geekListTitleView.text = geekListTitle
        @StringRes val titleResId = geekListItem.titleResId()
        if (titleResId == ResourcesCompat.ID_NULL)
            binding.typeView.text = ""
        else
            binding.typeView.setText(titleResId)
        binding.usernameView.text = geekListItem.username
        binding.thumbsView.text = geekListItem.numberOfThumbs.toString()
        binding.bodyView.setWebViewText(markupConverter.toHtml(geekListItem.body))
        binding.postedDateView.timestamp = geekListItem.postDateTime
        binding.editedDateView.timestamp = geekListItem.editDateTime
        binding.datetimeDividerView.isVisible = geekListItem.editDateTime != geekListItem.postDateTime
        binding.editedDateView.isVisible = geekListItem.editDateTime != geekListItem.postDateTime
    }

    @StringRes
    private fun GeekListItem.titleResId(): Int {
        return when (this.objectType) {
            GeekListItem.ObjectType.BoardGame -> R.string.title_board_game
            GeekListItem.ObjectType.BoardGameAccessory -> R.string.title_board_game_accessory
            GeekListItem.ObjectType.Thing -> R.string.title_thing
            GeekListItem.ObjectType.Publisher -> R.string.title_board_game_publisher
            GeekListItem.ObjectType.Company -> R.string.title_company
            GeekListItem.ObjectType.Designer -> R.string.title_board_game_designer
            GeekListItem.ObjectType.Person -> R.string.title_person
            GeekListItem.ObjectType.BoardGameFamily -> R.string.title_board_game_family
            GeekListItem.ObjectType.Family -> R.string.title_family
            GeekListItem.ObjectType.File -> R.string.title_file
            GeekListItem.ObjectType.GeekList -> R.string.title_geeklist
            GeekListItem.ObjectType.Unknown -> ResourcesCompat.ID_NULL
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val KEY_ORDER = "GEEK_LIST_ORDER"
        private const val KEY_TITLE = "GEEK_LIST_TITLE"
        private const val KEY_ITEM = "ITEM"

        fun newInstance(order: Int, title: String, item: GeekListItem): GeekListItemFragment {
            return GeekListItemFragment().apply {
                arguments = bundleOf(
                    KEY_ORDER to order,
                    KEY_TITLE to title,
                    KEY_ITEM to item,
                )
            }
        }
    }
}
