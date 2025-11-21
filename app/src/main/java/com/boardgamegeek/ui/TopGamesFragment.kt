package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentTopGamesBinding
import com.boardgamegeek.entities.TopGameEntity
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.ui.adapter.TopGamesAdapter
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.jsoup.Jsoup
import timber.log.Timber
import java.io.IOException

class TopGamesFragment : Fragment() {
    private var _binding: FragmentTopGamesBinding? = null
    private val binding get() = _binding!!

    private val adapter: TopGamesAdapter by lazy { TopGamesAdapter() }

    private val disposables = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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
        disposables.clear() // ✅ Dispose subscriptions properly
        _binding = null
    }

    private fun loadTopGames() {
        val disposable = Single
            .fromCallable { findTopGames() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ topGames ->
                if (!isAdded) return@subscribe
                if (topGames.isEmpty()) {
                    binding.emptyView.setText(R.string.empty_top_games)
                    binding.emptyView.fadeIn()
                    binding.recyclerView.fadeOut()
                } else {
                    adapter.results = topGames
                    binding.recyclerView.fadeIn()
                    binding.emptyView.fadeOut()
                }
                binding.progressView.hide()
            }, { error ->
                Timber.w(error, "Error loading top games")
                if (!isAdded) return@subscribe
                binding.emptyView.text = getString(R.string.empty_http_error, error.localizedMessage)
                binding.recyclerView.fadeOut()
                binding.emptyView.fadeIn()
                binding.progressView.hide()
            })

        disposables.add(disposable)
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
            val link = element.getElementsByTag("a").first()!!
            val gameNameElement = element.parent()!!.select(".collection_objectname")[0].child(1)
            val yearPublishedText = gameNameElement.child(1).text()

            val game = TopGameEntity(
                getGameIdFromLink(link.attr("href")),
                gameNameElement.child(0).text(),
                rank,
                Integer.parseInt(yearPublishedText.substring(1, yearPublishedText.length - 1)),
                link.child(0).attr("src") ?: ""
            )

            topGames.add(game)
            rank++
        }
        return topGames
    }

    private fun getGameIdFromLink(href: String): Int {
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
