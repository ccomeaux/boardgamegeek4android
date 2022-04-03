package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.databinding.FragmentGeeklistDescriptionBinding
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.setWebViewText
import com.boardgamegeek.ui.viewmodel.GeekListViewModel
import com.boardgamegeek.util.XmlApiMarkupConverter

class GeekListDescriptionFragment : Fragment() {
    private var _binding: FragmentGeeklistDescriptionBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<GeekListViewModel>()

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentGeeklistDescriptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val markupConverter = XmlApiMarkupConverter(requireContext())
        viewModel.geekList.observe(viewLifecycleOwner) {
            it?.let { (status, data, _) ->
                if (status == Status.SUCCESS) {
                    data?.let { entity ->
                        binding.header.usernameView.text = entity.username
                        binding.header.itemCountView.text = entity.numberOfItems.toString()
                        binding.header.thumbCountView.text = entity.numberOfThumbs.toString()
                        binding.bodyView.setWebViewText(markupConverter.toHtml(entity.description))
                        binding.header.postedDateView.timestamp = entity.postTicks
                        binding.header.editedDateView.timestamp = entity.editTicks
                    }
                    binding.container.isVisible = data != null
                    binding.progressBar.hide()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
