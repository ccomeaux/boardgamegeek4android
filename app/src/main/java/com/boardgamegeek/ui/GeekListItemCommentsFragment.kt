package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import com.boardgamegeek.databinding.FragmentGeeklistCommentsBinding
import com.boardgamegeek.model.GeekListComment
import com.boardgamegeek.extensions.getParcelableArrayListCompat
import com.boardgamegeek.ui.adapter.GeekListCommentsRecyclerViewAdapter

class GeekListItemCommentsFragment : Fragment() {
    private var _binding: FragmentGeeklistCommentsBinding? = null
    private val binding get() = _binding!!
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

        val comments = arguments?.getParcelableArrayListCompat<GeekListComment>(KEY_COMMENTS).orEmpty()

        adapter.comments = comments
        binding.emptyView.isVisible = comments.isEmpty()
        binding.recyclerView.isVisible = comments.isNotEmpty()
        binding.progressView.hide()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
        _binding = null
    }

    companion object {
        private const val KEY_COMMENTS = "GEEK_LIST_COMMENTS"

        fun newInstance(comments: List<GeekListComment>?): GeekListItemCommentsFragment {
            return GeekListItemCommentsFragment().apply {
                arguments = bundleOf(KEY_COMMENTS to comments)
            }
        }
    }
}
