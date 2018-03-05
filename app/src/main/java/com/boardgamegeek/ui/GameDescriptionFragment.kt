package com.boardgamegeek.ui


import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider.AndroidViewModelFactory
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.setTextMaybeHtml
import com.boardgamegeek.tasks.sync.SyncGameTask
import com.boardgamegeek.ui.viewmodel.GameDescriptionViewModel
import com.boardgamegeek.util.TaskUtils
import kotlinx.android.synthetic.main.fragment_game_description.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class GameDescriptionFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {
    private var gameId: Int = 0
    private var isRefreshing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
        if (arguments != null) {
            gameId = arguments!!.getInt(ARG_GAME_ID, BggContract.INVALID_ID)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_game_description, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipe_refresh.setOnRefreshListener(this)
        swipe_refresh.setColorSchemeResources(R.color.orange, R.color.light_blue, R.color.dark_blue, R.color.light_blue)

        val viewModel = AndroidViewModelFactory.getInstance(activity!!.application).create(GameDescriptionViewModel::class.java)
        viewModel.getGameDescription(gameId).observe(this, Observer { s ->
            game_description.setTextMaybeHtml(s)
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    override fun onRefresh() {
        if (!isRefreshing) {
            updateRefreshStatus(true)
            TaskUtils.executeAsyncTask(SyncGameTask(context, gameId))
        } else {
            updateRefreshStatus(false)
        }
    }

    private fun updateRefreshStatus(refreshing: Boolean) {
        this.isRefreshing = refreshing
        if (swipe_refresh != null) {
            swipe_refresh.post { if (swipe_refresh != null) swipe_refresh.isRefreshing = isRefreshing }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: SyncGameTask.CompletedEvent) {
        if (event.gameId == gameId) {
            updateRefreshStatus(false)
        }
    }

    companion object {
        private const val ARG_GAME_ID = "GAME_ID"

        @JvmStatic
        fun newInstance(gameId: Int): GameDescriptionFragment {
            val fragment = GameDescriptionFragment()
            val args = Bundle()
            args.putInt(ARG_GAME_ID, gameId)
            fragment.arguments = args
            return fragment
        }
    }
}