package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.boardgamegeek.databinding.FragmentGeeklistItemBinding
import com.boardgamegeek.entities.GeekListItemEntity
import com.boardgamegeek.extensions.setWebViewText
import com.boardgamegeek.util.XmlApiMarkupConverter

class GeekListItemFragment : Fragment() {
    private var _binding: FragmentGeeklistItemBinding? = null
    private val binding get() = _binding!!
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
        binding.typeView.text = glItem.objectTypeDescription(requireContext())
        binding.usernameView.text = glItem.username
        binding.thumbsView.text = glItem.numberOfThumbs.toString()
        binding.bodyView.setWebViewText(markupConverter.toHtml(glItem.body))
        binding.postedDateView.timestamp = glItem.postDateTime
        binding.editedDateView.timestamp = glItem.editDateTime
        binding.datetimeDividerView.isVisible = glItem.editDateTime != glItem.postDateTime
        binding.editedDateView.isVisible = glItem.editDateTime != glItem.postDateTime
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val KEY_ORDER = "GEEK_LIST_ORDER"
        private const val KEY_TITLE = "GEEK_LIST_TITLE"
        private const val KEY_ITEM = "ITEM"

        fun newInstance(order: Int, title: String, item: GeekListItemEntity): GeekListItemFragment {
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
