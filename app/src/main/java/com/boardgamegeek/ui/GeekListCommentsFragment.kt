package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import com.boardgamegeek.databinding.FragmentGeeklistCommentsBinding
import com.boardgamegeek.entities.Status
import com.boardgamegeek.ui.adapter.GeekListCommentsRecyclerViewAdapter
import com.boardgamegeek.ui.viewmodel.GeekListViewModel

class GeekListCommentsFragment : Fragment() {
    private var _binding: FragmentGeeklistCommentsBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<GeekListViewModel>()
    private val adapter: GeekListCommentsRecyclerViewAdapter by lazy { GeekListCommentsRecyclerViewAdapter() }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentGeeklistCommentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
        binding.recyclerView.adapter = adapter

        viewModel.geekList.observe(viewLifecycleOwner) {
            it?.let { (status, data, _) ->
                if (status == Status.REFRESHING) binding.progressView.show()
                else binding.progressView.hide()
                if (status == Status.ERROR) {
                    binding.emptyView.isVisible = true
                    binding.recyclerView.isVisible = false
                } else {
                    adapter.comments = data?.comments.orEmpty()
                    binding.emptyView.isVisible = adapter.comments.isEmpty()
                    binding.recyclerView.isVisible = adapter.comments.isNotEmpty()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
