package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.entities.TopGameEntity
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.adapter.TopGamesAdapter
import kotlinx.android.synthetic.main.fragment_top_games.*
import org.jsoup.Jsoup
import rx.Single
import rx.SingleSubscriber
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber
import java.io.IOException

class TopGamesFragment : Fragment() {
    private val adapter: TopGamesAdapter by lazy {
        TopGamesAdapter()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_top_games, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter
        loadTopGames()
    }

    private fun loadTopGames() {
        Single
            .create<List<TopGameEntity>> { singleSubscriber ->
                try {
                    val topGames = findTopGames()
                    singleSubscriber.onSuccess(topGames)
                } catch (t: Throwable) {
                    singleSubscriber.onError(t)
                }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleSubscriber<List<TopGameEntity>>() {
                override fun onSuccess(topGames: List<TopGameEntity>) {
                    if (!isAdded) return
                    if (topGames.isEmpty()) {
                        emptyView.setText(R.string.empty_top_games)
                        emptyView.fadeIn()
                        recyclerView.fadeOut()
                    } else {
                        adapter.results = topGames
                        recyclerView.fadeIn()
                        emptyView.fadeOut()
                    }
                    progressView.hide()
                }

                override fun onError(error: Throwable) {
                    Timber.w(error, "Error loading top games")
                    if (!isAdded) return
                    emptyView.text = getString(R.string.empty_http_error, error.localizedMessage)
                    recyclerView.fadeOut()
                    emptyView.fadeIn()
                    progressView.hide()
                }
            })
    }

    @Throws(IOException::class)
    private fun findTopGames(): List<TopGameEntity> {
        val topGames = mutableListOf<TopGameEntity>()

        var rank = 1
        val doc = Jsoup
            .connect("https://www.boardgamegeek.com/browse/boardgame")
            .timeout(10_000)
            .get()
        val gameElements = doc.select("td.collection_thumbnail")
        for (element in gameElements) {
            val link = element.getElementsByTag("a").first()
            val gameNameElement = element.parent()?.select(".collection_objectname")?.getOrNull(0)?.child(1)
            val yearPublishedText = gameNameElement?.child(1)?.text().orEmpty()

            val game = TopGameEntity(
                getGameIdFromLink(link?.attr("href")),
                gameNameElement?.child(0)?.text().orEmpty(),
                rank,
                Integer.parseInt(yearPublishedText.substring(1, yearPublishedText.length - 1)),
                link?.child(0)?.attr("src").orEmpty()
            )

            topGames.add(game)
            rank++
        }
        return topGames
    }

    private fun getGameIdFromLink(href: String?): Int {
        if (href == null) return BggContract.INVALID_ID
        val boardGameIndex = href.indexOf("/boardgame/")
        val afterBoardGameString = if (boardGameIndex != -1) {
            href.substring(boardGameIndex + 11)
        } else {
            val boardGameExpansionIndex = href.indexOf("/boardgameexpansion/")
            href.substring(boardGameExpansionIndex + 20)
        }
        val slashIndex = afterBoardGameString.indexOf("/")
        return Integer.parseInt(afterBoardGameString.substring(0, slashIndex))
    }
}
