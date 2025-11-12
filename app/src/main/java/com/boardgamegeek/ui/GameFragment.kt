package com.boardgamegeek.ui

import android.os.Bundle
import android.text.Html
import android.text.TextUtils
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.Games
import com.boardgamegeek.ui.dialog.GameRanksFragment
import com.boardgamegeek.ui.viewmodel.GameViewModel
import com.boardgamegeek.ui.widget.GameDetailRow
import com.boardgamegeek.ui.widget.SafeViewTarget
import com.boardgamegeek.util.HelpUtils
import com.boardgamegeek.util.ShowcaseViewWizard
import com.boardgamegeek.databinding.FragmentGameBinding
import com.github.amlcurran.showcaseview.targets.Target

class GameFragment : Fragment(R.layout.fragment_game) {
    private var _binding: FragmentGameBinding? = null
    private val binding get() = _binding!!
    private var gameId: Int = BggContract.INVALID_ID
    private var gameName: String = ""
    private var showcaseViewWizard: ShowcaseViewWizard? = null

    @Suppress("DEPRECATION")
    private val rankSeparator = "  " + Html.fromHtml("&#9679;") + "  "

    private val viewModel: GameViewModel by lazy {
        ViewModelProvider(this).get(GameViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentGameBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.swipeRefresh.setBggColors()

        binding.lastModifiedView.timestamp = 0

        viewModel.gameId.observe(this, Observer { gameId ->
            this.gameId = gameId
            binding.gameIdView.text = gameId.toString()
        })

        viewModel.game.observe(this, Observer {
            binding.swipeRefresh.post { binding.swipeRefresh.isRefreshing = it?.status == Status.REFRESHING }
            when {
                it == null -> showError(getString(R.string.empty_game))
                it.status == Status.ERROR && it.data == null -> showError(it.message)
                it.data == null -> showError(getString(R.string.empty_game))
                else -> onGameContentChanged(it.data)
            }
            binding.progress.hide()

            viewModel.ranks.observe(this, Observer { gameRanks -> onRankQueryComplete(gameRanks) })

            viewModel.languagePoll.observe(this, Observer { gamePollEntity -> onLanguagePollQueryComplete(gamePollEntity) })

            viewModel.agePoll.observe(this, Observer { gameSuggestedAgePollEntity -> onAgePollQueryComplete(gameSuggestedAgePollEntity) })

            viewModel.playerPoll.observe(this, Observer { gamePlayerPollEntities -> onPlayerCountQueryComplete(gamePlayerPollEntities) })

            viewModel.expansions.observe(this, Observer { gameDetails -> onListQueryComplete(gameDetails, binding.gameInfoExpansions) })

            viewModel.baseGames.observe(this, Observer { gameDetails -> onListQueryComplete(gameDetails, binding.gameInfoBaseGames) })
        })

        showcaseViewWizard = setUpShowcaseViewWizard()
        showcaseViewWizard?.maybeShowHelp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.help, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_help) {
            showcaseViewWizard?.showHelp()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setUpShowcaseViewWizard(): ShowcaseViewWizard {
        val wizard = ShowcaseViewWizard(activity, HelpUtils.HELP_GAME_KEY, HELP_VERSION)
        wizard.addTarget(R.string.help_game_menu, Target.NONE)
        wizard.addTarget(R.string.help_game_poll, SafeViewTarget(R.id.playerCountContainer, requireActivity()))
        wizard.addTarget(0, SafeViewTarget(R.id.playerAgeContainer, requireActivity()))
        return wizard
    }

    private fun showError(message: String?) {
        if (message?.isNotBlank() == true) {
            binding.emptyMessage.text = message
            binding.dataContainer.fadeOut()
            binding.emptyMessage.fadeIn()
        }
    }

    private fun colorize(@ColorInt iconColor: Int) {
        if (!isAdded) return

        listOf(binding.ranksIcon, binding.ratingIcon, binding.yearIcon, binding.playTimeIcon, binding.playerCountIcon, binding.playerAgeIcon, binding.weightIcon, binding.languageIcon)
                .forEach { it.setOrClearColorFilter(iconColor) }

        listOf(binding.gameInfoExpansions, binding.gameInfoBaseGames)
                .forEach { it.colorize(iconColor) }
    }

    private fun onGameContentChanged(game: GameEntity) {
        colorize(game.iconColor)

        gameName = game.name

        binding.rankView.text = game.overallRank.asRank(requireContext(), game.subtype)
        binding.rankContainer.setOnClickListener { GameRanksFragment.launch(this) }

        binding.ratingView.text = game.rating.asRating(context)
        binding.ratingView.setTextViewBackground(game.rating.toColor(ratingColors))
        val numberOfRatings = context?.getQuantityText(R.plurals.ratings_suffix, game.numberOfRatings, game.numberOfRatings)
                ?: ""
        val numberOfComments = context?.getQuantityText(R.plurals.comments_suffix, game.numberOfComments, game.numberOfComments)
                ?: ""
        binding.ratingVotesView.text = listOf(numberOfRatings, " & ", numberOfComments).concat()
        binding.ratingContainer.setOrClearOnClickListener(game.numberOfRatings > 0 || game.numberOfComments > 0) {
            CommentsActivity.startRating(context, Games.buildGameUri(gameId), gameName)
        }

        binding.yearView.text = game.yearPublished.asYear(context)

        binding.playTimeView.text = context?.getQuantityText(R.plurals.mins_suffix, game.maxPlayingTime, (game.minPlayingTime to game.maxPlayingTime).asRange())
                ?: ""

        binding.playerCountView.text = context?.getQuantityText(R.plurals.player_range_suffix, game.maxPlayers, (game.minPlayers to game.maxPlayers).asRange())
                ?: ""

        binding.playerAgeView.text = game.minimumAge.asAge(context)

        binding.weightView.text = game.averageWeight.toDescription(requireContext(), R.array.game_weight, R.string.unknown_weight)
        if (game.averageWeight == 0.0) {
            binding.weightScoreView.isVisible = false
        } else {
            binding.weightScoreView.setTextOrHide(game.averageWeight.asScore(context))
        }
        val textColor = binding.weightColorView.setTextViewBackground(game.averageWeight.toColor(fiveStageColors))
        binding.weightView.setTextColor(textColor)
        binding.weightScoreView.setTextColor(textColor)
        binding.weightVotesView.setTextOrHide(requireContext().getQuantityText(R.plurals.votes_suffix, game.numberOfUsersWeighting, game.numberOfUsersWeighting))

        binding.gameIdView.text = game.id.toString()
        binding.lastModifiedView.timestamp = game.updated

        binding.emptyMessage.fadeOut()
        binding.dataContainer.fadeIn()
    }

    private fun onRankQueryComplete(gameRanks: List<GameRankEntity>?) {
        val descriptions = gameRanks?.filter { it.isFamilyType }?.map { it.value.asRank(requireContext(), it.name, it.type) }
                ?: emptyList()
        binding.subtypeView.setTextOrHide(descriptions.joinTo(rankSeparator))
    }

    private fun onLanguagePollQueryComplete(entity: GamePollEntity?) {
        val score = entity?.calculateScore() ?: 0.0
        val totalVotes = entity?.totalVotes ?: 0

        binding.languageView.text = score.toDescription(requireContext(), R.array.language_poll, R.string.unknown_language)
        if (score == 0.0) {
            binding.languageScoreView.isVisible = false
        } else {
            binding.languageScoreView.setTextOrHide(score.asScore(context))
        }
        binding.languageVotesView.setTextOrHide(requireContext().getQuantityText(R.plurals.votes_suffix, totalVotes, totalVotes))

        val textColor = binding.languageColorView.setTextViewBackground(score.toColor(fiveStageColors))
        binding.languageView.setTextColor(textColor)
        binding.languageScoreView.setTextColor(textColor)

        binding.languageContainer.setOrClearOnClickListener(totalVotes > 0) {
            PollFragment.launchLanguageDependence(this)
        }
    }

    private fun onAgePollQueryComplete(entity: GamePollEntity?) {
        val message = if (entity?.modalValue.isNullOrBlank()) ""
        else context?.getText(R.string.age_community, entity?.modalValue ?: "") ?: ""
        binding.playerAgePollView.setTextOrHide(message)
        binding.playerAgeContainer.setOrClearOnClickListener(entity?.totalVotes ?: 0 > 0) {
            PollFragment.launchSuggestedPlayerAge(this)
        }
    }

    private fun onPlayerCountQueryComplete(entity: GamePlayerPollEntity?) {
        val bestCounts = entity?.bestCounts ?: emptyList()
        val goodCounts = entity?.recommendedCounts ?: emptyList()

        val best = context?.getText(R.string.best_prefix, bestCounts.asRange(max = maxPlayerCount)) ?: ""
        val good = context?.getText(R.string.recommended_prefix, goodCounts.asRange(max = maxPlayerCount)) ?: ""
        val communityText = when {
            bestCounts.isNotEmpty() && goodCounts.isNotEmpty() && bestCounts != goodCounts -> TextUtils.concat(best, " & ", good)
            bestCounts.isNotEmpty() -> best
            goodCounts.isNotEmpty() -> good
            else -> ""
        }
        binding.playerCountCommunityView.setTextOrHide(communityText)
        binding.playerCountContainer.setOrClearOnClickListener(entity?.totalVotes ?: 0 > 0) {
            SuggestedPlayerCountPollFragment.launch(this)
        }
    }

    private fun onListQueryComplete(list: List<GameDetailEntity>?, view: GameDetailRow?) {
        view?.bindData(
                viewModel.gameId.value ?: BggContract.INVALID_ID,
                viewModel.game.value?.data?.name ?: "",
                list)
    }

    companion object {
        private const val HELP_VERSION = 2

        fun newInstance(): GameFragment {
            return GameFragment()
        }
    }
}