package com.boardgamegeek.ui

import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentThreadBinding
import com.boardgamegeek.model.Forum
import com.boardgamegeek.model.Status
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.extensions.getSerializableCompat
import com.boardgamegeek.extensions.set
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.adapter.ThreadRecyclerViewAdapter
import com.boardgamegeek.ui.viewmodel.ThreadViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs

@AndroidEntryPoint
class ThreadFragment : Fragment() {
    private var _binding: FragmentThreadBinding? = null
    private val binding get() = _binding!!
    private var threadId = BggContract.INVALID_ID
    private var forumId = BggContract.INVALID_ID
    private var forumTitle = ""
    private var objectId = BggContract.INVALID_ID
    private var objectName = ""
    private var objectType = Forum.Type.REGION

    private var currentAdapterPosition = 0
    private var latestArticleId: Int = INVALID_ARTICLE_ID

    private val viewModel by activityViewModels<ThreadViewModel>()

    private val adapter: ThreadRecyclerViewAdapter by lazy {
        ThreadRecyclerViewAdapter(forumId, forumTitle, objectId, objectName, objectType)
    }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentThreadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            threadId = it.getInt(KEY_THREAD_ID, BggContract.INVALID_ID)
            forumId = it.getInt(KEY_FORUM_ID, BggContract.INVALID_ID)
            forumTitle = it.getString(KEY_FORUM_TITLE).orEmpty()
            objectId = it.getInt(KEY_OBJECT_ID, BggContract.INVALID_ID)
            objectName = it.getString(KEY_OBJECT_NAME).orEmpty()
            objectType = it.getSerializableCompat(KEY_OBJECT_TYPE) ?: Forum.Type.REGION
        }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.thread, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                menu.findItem(R.id.menu_scroll_last)?.isVisible = latestArticleId != INVALID_ARTICLE_ID && adapter.itemCount > 0
                menu.findItem(R.id.menu_scroll_bottom)?.isVisible = adapter.itemCount > 0
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.menu_scroll_last -> scrollToLatestArticle()
                    R.id.menu_scroll_bottom -> scrollToBottom()
                    else -> return false

                }
                return true
            }
        })

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                currentAdapterPosition = (recyclerView.layoutManager as? LinearLayoutManager)?.findLastCompletelyVisibleItemPosition()
                    ?: RecyclerView.NO_POSITION
                if (currentAdapterPosition != RecyclerView.NO_POSITION) {
                    val currentArticleId = adapter.getItemId(currentAdapterPosition)
                    if (currentArticleId > latestArticleId) {
                        latestArticleId = currentArticleId.toInt()
                    }
                }
            }
        })

        viewModel.setThreadId(threadId)
        viewModel.articles.observe(viewLifecycleOwner) {
            it?.let { (status, data, message) ->
                when (status) {
                    Status.REFRESHING -> binding.progressView.show()
                    Status.ERROR -> {
                        binding.emptyView.text = message.ifEmpty { getString(R.string.empty_thread) }
                        binding.emptyView.isVisible = true
                        binding.recyclerView.isVisible = false
                        binding.progressView.hide()
                    }
                    Status.SUCCESS -> {
                        binding.emptyView.setText(R.string.empty_thread)
                        adapter.articles = data?.articles.orEmpty()
                        adapter.threadId = data?.threadId ?: BggContract.INVALID_ID
                        adapter.threadSubject = data?.subject.orEmpty()
                        binding.emptyView.isVisible = data?.articles.orEmpty().isEmpty()
                        binding.recyclerView.isVisible = data?.articles.orEmpty().isNotEmpty()
                        binding.progressView.hide()
                    }
                }
            }
            activity?.invalidateOptionsMenu()
        }
    }

    override fun onResume() {
        super.onResume()
        latestArticleId = requireContext().preferences()[getThreadKey(threadId), INVALID_ARTICLE_ID] ?: INVALID_ARTICLE_ID
    }

    override fun onPause() {
        super.onPause()
        if (latestArticleId != INVALID_ARTICLE_ID) {
            requireContext().preferences()[getThreadKey(threadId)] = latestArticleId
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getThreadKey(threadId: Int): String {
        return "THREAD-$threadId"
    }

    private fun scrollToLatestArticle() {
        if (latestArticleId != INVALID_ARTICLE_ID) {
            scrollToPosition(adapter.getPosition(latestArticleId))
        }
    }

    private fun scrollToBottom() {
        scrollToPosition(adapter.itemCount - 1)
    }

    private fun scrollToPosition(position: Int) {
        if (position != RecyclerView.NO_POSITION) {
            val difference = abs(currentAdapterPosition - position)
            if (difference <= SMOOTH_SCROLL_THRESHOLD) {
                binding.recyclerView.smoothScrollToPosition(position)
            } else {
                binding.recyclerView.scrollToPosition(position)
            }
        }
    }

    companion object {
        private const val KEY_FORUM_ID = "FORUM_ID"
        private const val KEY_FORUM_TITLE = "FORUM_TITLE"
        private const val KEY_OBJECT_ID = "OBJECT_ID"
        private const val KEY_OBJECT_NAME = "OBJECT_NAME"
        private const val KEY_OBJECT_TYPE = "OBJECT_TYPE"
        private const val KEY_THREAD_ID = "THREAD_ID"
        private const val SMOOTH_SCROLL_THRESHOLD = 10
        private const val INVALID_ARTICLE_ID = -1

        fun newInstance(
            threadId: Int,
            forumId: Int,
            forumTitle: String?,
            objectId: Int,
            objectName: String?,
            objectType: Forum.Type?,
        ): ThreadFragment {
            return ThreadFragment().apply {
                arguments = bundleOf(
                    KEY_THREAD_ID to threadId,
                    KEY_FORUM_ID to forumId,
                    KEY_FORUM_TITLE to forumTitle,
                    KEY_OBJECT_ID to objectId,
                    KEY_OBJECT_NAME to objectName,
                    KEY_OBJECT_TYPE to objectType,
                )
            }
        }
    }
}
