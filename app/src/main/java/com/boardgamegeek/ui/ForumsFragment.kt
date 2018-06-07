package com.boardgamegeek.ui

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.boardgamegeek.R
import com.boardgamegeek.entities.ForumEntity
import com.boardgamegeek.fadeIn
import com.boardgamegeek.fadeOut
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.model.ForumListResponse
import com.boardgamegeek.mappers.ForumMapper
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.adapter.ForumsRecyclerViewAdapter
import com.boardgamegeek.ui.loader.BggLoader
import com.boardgamegeek.ui.loader.SafeResponse
import kotlinx.android.synthetic.main.fragment_forums.*
import org.jetbrains.anko.support.v4.ctx
import retrofit2.Call
import java.util.*

class ForumsFragment : Fragment(), LoaderManager.LoaderCallbacks<SafeResponse<ForumListResponse>> {
    private var gameId = BggContract.INVALID_ID
    private var gameName: String? = null

    private val adapter: ForumsRecyclerViewAdapter by lazy {
        ForumsRecyclerViewAdapter(gameId, gameName)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_forums, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        gameId = arguments?.getInt(KEY_GAME_ID, BggContract.INVALID_ID) ?: BggContract.INVALID_ID
        gameName = arguments?.getString(KEY_GAME_NAME)

        recyclerView?.layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.VERTICAL, false)
        recyclerView?.setHasFixedSize(true)
        recyclerView?.addItemDecoration(DividerItemDecoration(ctx, DividerItemDecoration.VERTICAL))
        recyclerView?.adapter = adapter
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        loaderManager.initLoader(0, null, this)
    }

    override fun onCreateLoader(id: Int, data: Bundle?): Loader<SafeResponse<ForumListResponse>> {
        return ForumsLoader(ctx, gameId)
    }

    override fun onLoadFinished(loader: Loader<SafeResponse<ForumListResponse>>, data: SafeResponse<ForumListResponse>) {
        if (!isAdded) return

        val forums = ArrayList<ForumEntity>()
        val mapper = ForumMapper()
        data.body?.forums?.forEach {
            forums.add(mapper.map(it))
        }
        adapter.forums = forums

        if (data.hasError()) {
            emptyView?.text = data.errorMessage
            emptyView.fadeIn()
            recyclerView.fadeOut()
        } else {
            if (adapter.itemCount == 0) {
                emptyView.fadeIn()
                recyclerView.fadeOut()
            } else {
                recyclerView.fadeIn()
                emptyView.fadeOut()
            }
        }
        progressView?.hide()
    }

    override fun onLoaderReset(loader: Loader<SafeResponse<ForumListResponse>>) {}

    private class ForumsLoader(context: Context, private val gameId: Int) : BggLoader<SafeResponse<ForumListResponse>>(context) {
        private val bggService: BggService = Adapter.createForXml()

        override fun loadInBackground(): SafeResponse<ForumListResponse>? {
            val call: Call<ForumListResponse> = if (gameId == BggContract.INVALID_ID) {
                bggService.forumList(BggService.FORUM_TYPE_REGION, BggService.FORUM_REGION_BOARDGAME)
            } else {
                bggService.forumList(BggService.FORUM_TYPE_THING, gameId)
            }
            return SafeResponse(call)
        }
    }

    companion object {
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_GAME_NAME = "GAME_NAME"

        fun newInstance(): ForumsFragment {
            return ForumsFragment()
        }

        fun newInstance(gameId: Int, gameName: String): ForumsFragment {
            val args = Bundle()
            args.putInt(KEY_GAME_ID, gameId)
            args.putString(KEY_GAME_NAME, gameName)

            val fragment = ForumsFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
