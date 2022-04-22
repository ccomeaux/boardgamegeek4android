package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentTopGamesBinding
import com.boardgamegeek.entities.TopGameEntity
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.adapter.TopGamesAdapter
import org.jsoup.Jsoup
import rx.Single
import rx.SingleSubscriber
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber
import java.io.IOException

class TopGamesFragment : Fragment() {
    private var _binding: FragmentTopGamesBinding? = null
    private val binding get() = _binding!!
    private val adapter: TopGamesAdapter by lazy { TopGamesAdapter() }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentTopGamesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter
        loadTopGames()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
                        binding.emptyView.setText(R.string.empty_top_games)
                        binding.emptyView.isVisible = true
                        binding.recyclerView.isVisible = false
                    } else {
                        adapter.results = topGames
                        binding.recyclerView.isVisible = true
                        binding.emptyView.isVisible = false
                    }
                    binding.progressView.hide()
                }

                override fun onError(error: Throwable) {
                    Timber.w(error, "Error loading top games")
                    if (!isAdded) return
                    binding.emptyView.text = getString(R.string.empty_http_error, error.localizedMessage)
                    binding.recyclerView.isVisible = false
                    binding.emptyView.isVisible = true
                    binding.progressView.hide()
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
            @Suppress("SpellCheckingInspection")
            val gameNameElement = element.parent()?.select(".collection_objectname")?.getOrNull(0)?.child(1)
            val yearPublishedText = gameNameElement?.child(1)?.text().orEmpty()

            topGames += TopGameEntity(
                getGameIdFromLink(link?.attr("href")),
                gameNameElement?.child(0)?.text().orEmpty(),
                rank,
                yearPublishedText.substring(1, yearPublishedText.length - 1).toInt(),
                link?.child(0)?.attr("src").orEmpty(),
            )

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
