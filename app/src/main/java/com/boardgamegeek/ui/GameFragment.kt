package com.boardgamegeek.ui

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentGameBinding
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.dialog.GamePollDialogFragment
import com.boardgamegeek.ui.dialog.GameRanksDialogFragment
import com.boardgamegeek.ui.dialog.GameSuggestedPlayerCountPollDialogFragment
import com.boardgamegeek.ui.viewmodel.GameViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.DecimalFormat

@AndroidEntryPoint
class GameFragment : Fragment() {
    private var _binding: FragmentGameBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<GameViewModel>()
    private var gameId = BggContract.INVALID_ID
    private var gameName = ""
    private val scoreFormat = DecimalFormat("#,##0.00")

    @Suppress("DEPRECATION")
    private val rankSeparator = "  ${Html.fromHtml("&#9679;")}  "

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.constraintLayout.layoutTransition.setAnimateParentHierarchy(false)
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.swipeRefresh.setBggColors()

        binding.footer.lastModifiedView.timestamp = 0

        viewModel.gameId.observe(viewLifecycleOwner) { gameId ->
            this.gameId = gameId
            binding.footer.gameIdView.text = gameId.toString()
        }

        viewModel.game.observe(viewLifecycleOwner) { resource ->
            resource?.let { (status, data, message) ->
                binding.swipeRefresh.isRefreshing = status == Status.REFRESHING
                when {
                    status == Status.ERROR && data == null -> showError(message)
                    data == null -> showError(getString(R.string.empty_game))
                    else -> onGameContentChanged(data)
                }
                binding.progress.hide()

                viewModel.ranks.observe(viewLifecycleOwner) { it?.let { onRankQueryComplete(it) } }
                viewModel.languagePoll.observe(viewLifecycleOwner) { gamePollEntity -> onLanguagePollQueryComplete(gamePollEntity) }
                viewModel.agePoll.observe(viewLifecycleOwner) { gameSuggestedAgePollEntity -> onAgePollQueryComplete(gameSuggestedAgePollEntity) }
                viewModel.playerPoll.observe(viewLifecycleOwner) { gamePlayerPollEntities -> onPlayerCountQueryComplete(gamePlayerPollEntities) }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showError(message: String?) {
        if (message?.isNotBlank() == true) {
            binding.emptyMessage.text = message
            binding.emptyMessage.isVisible = true
            binding.ranksInclude.root.isVisible = true
            binding.ratingsInclude.root.isVisible = true
            binding.yearInclude.root.isVisible = true
            binding.playingTimeInclude.root.isVisible = true
            binding.playerRangeInclude.root.isVisible = true
            binding.agesInclude.root.isVisible = true
            binding.weightInclude.root.isVisible = true
            binding.languageInclude.root.isVisible = false
        }
    }

    private fun colorize(@ColorInt iconColor: Int) {
        if (!isAdded) return

        listOf(
            binding.ranksInclude.ranksIcon,
            binding.ratingsInclude.ratingIcon,
            binding.yearInclude.yearIcon,
            binding.playingTimeInclude.playTimeIcon,
            binding.playerRangeInclude.playerCountIcon,
            binding.agesInclude.playerAgeIcon,
            binding.weightInclude.weightIcon,
            binding.languageInclude.languageIcon,
        ).forEach { it.setOrClearColorFilter(iconColor) }
    }

    private fun onGameContentChanged(game: GameEntity) {
        colorize(game.iconColor)

        gameName = game.name

        binding.ranksInclude.rankView.text = game.overallRank.asRank(requireContext(), game.subtype?.code.orEmpty())
        binding.ranksInclude.rankContainer.setOnClickListener { GameRanksDialogFragment.launch(this) }
        binding.ranksInclude.root.isVisible = true

        binding.ratingsInclude.ratingView.text = game.rating.asBoundedRating(context, DecimalFormat("#0.0"), R.string.unrated)
        binding.ratingsInclude.ratingView.setTextViewBackground(game.rating.toColor(BggColors.ratingColors))
        val numberOfRatings = requireContext().getQuantityText(R.plurals.ratings_suffix, game.numberOfRatings, game.numberOfRatings)
        val numberOfComments = requireContext().getQuantityText(R.plurals.comments_suffix, game.numberOfComments, game.numberOfComments)
        binding.ratingsInclude.ratingVotesView.text = getString(R.string.ampersand, numberOfRatings, numberOfComments)
        binding.ratingsInclude.ratingContainer.setOrClearOnClickListener(game.numberOfRatings > 0 || game.numberOfComments > 0) {
            CommentsActivity.startRating(requireContext(), gameId, gameName)
        }
        binding.ratingsInclude.root.isVisible = true

        binding.yearInclude.yearView.text = game.yearPublished.asYear(context)
        binding.yearInclude.root.isVisible = true

        binding.playingTimeInclude.playTimeView.text =
            requireContext().getQuantityText(R.plurals.mins_suffix, game.minPlayingTime, (game.minPlayingTime to game.maxPlayingTime).asRange())
        binding.playingTimeInclude.root.isVisible = true

        binding.playerRangeInclude.playerCountView.text =
            requireContext().getQuantityText(R.plurals.player_range_suffix, game.minPlayers, (game.minPlayers to game.maxPlayers).asRange())
        binding.playerRangeInclude.root.isVisible = true

        binding.agesInclude.playerAgeView.text = game.minimumAge.asAge(context)
        binding.agesInclude.root.isVisible = true

        binding.weightInclude.weightView.text = game.averageWeight.toDescription(requireContext(), R.array.game_weight, R.string.unknown_weight)
        if (game.averageWeight == 0.0) {
            binding.weightInclude.weightScoreView.isVisible = false
        } else {
            binding.weightInclude.weightScoreView.setTextOrHide(game.averageWeight.asScore(context, format = scoreFormat))
        }
        val textColor = binding.weightInclude.weightColorView.setTextViewBackground(game.averageWeight.toColor(BggColors.fiveStageColors))
        binding.weightInclude.weightView.setTextColor(textColor)
        binding.weightInclude.weightScoreView.setTextColor(textColor)
        binding.weightInclude.weightVotesView.setTextOrHide(
            requireContext().getQuantityText(
                R.plurals.votes_suffix,
                game.numberOfUsersWeighting,
                game.numberOfUsersWeighting
            )
        )
        binding.weightInclude.root.isVisible = true

        binding.footer.gameIdView.text = game.id.toString()
        binding.footer.lastModifiedView.timestamp = game.updated

        binding.emptyMessage.isVisible = false
    }

    private fun onRankQueryComplete(gameRanks: List<GameRankEntity>) {
        val descriptions = gameRanks
            .filter { it.isFamilyType }
            .map { it.value.asRank(requireContext(), it.name, it.type) }
        binding.ranksInclude.subtypeView.setTextOrHide(descriptions.joinTo(rankSeparator))
    }

    private fun onLanguagePollQueryComplete(entity: GamePollEntity?) {
        val score = entity?.calculateScore() ?: 0.0
        val totalVotes = entity?.totalVotes ?: 0

        binding.languageInclude.languageView.text = score.toDescription(requireContext(), R.array.language_poll, R.string.unknown_language)
        if (score == 0.0) {
            binding.languageInclude.languageScoreView.isVisible = false
        } else {
            binding.languageInclude.languageScoreView.setTextOrHide(score.asScore(context, format = scoreFormat))
        }
        binding.languageInclude.languageVotesView.setTextOrHide(requireContext().getQuantityText(R.plurals.votes_suffix, totalVotes, totalVotes))

        val textColor = binding.languageInclude.languageColorView.setTextViewBackground(score.toColor(BggColors.fiveStageColors))
        binding.languageInclude.languageView.setTextColor(textColor)
        binding.languageInclude.languageScoreView.setTextColor(textColor)

        binding.languageInclude.languageContainer.setOrClearOnClickListener(totalVotes > 0) {
            GamePollDialogFragment.launchLanguageDependence(this)
        }
        binding.languageInclude.root.isVisible = true
    }

    private fun onAgePollQueryComplete(entity: GamePollEntity?) {
        val voteCount = entity?.totalVotes ?: 0
        val message = if (entity?.modalValue.isNullOrBlank()) ""
        else requireContext().getText(R.string.age_community, entity?.modalValue.orEmpty())
        binding.agesInclude.playerAgePollView.setTextOrHide(message)
        binding.agesInclude.playerAgeVotesView.setTextOrHide(requireContext().getQuantityText(R.plurals.votes_suffix, voteCount, voteCount))
        binding.agesInclude.playerAgeContainer.setOrClearOnClickListener(voteCount > 0) {
            GamePollDialogFragment.launchSuggestedPlayerAge(this)
        }
    }

    private fun onPlayerCountQueryComplete(entity: GamePlayerPollEntity?) {
        val bestCounts = entity?.results
            ?.filter { it.calculatedRecommendation == GamePlayerPollResultsEntity.BEST }
            ?.toSet() ?: emptySet()
        val goodCounts = entity?.results
            ?.filter { it.calculatedRecommendation == GamePlayerPollResultsEntity.BEST || it.calculatedRecommendation == GamePlayerPollResultsEntity.RECOMMENDED }
            ?.toSet() ?: emptySet()
        val voteCount = entity?.totalVotes ?: 0

        val best = requireContext().getText(R.string.best_prefix, bestCounts.toList().asRange())
        val good = requireContext().getText(R.string.recommended_prefix, goodCounts.toList().asRange())
        val communityText = when {
            bestCounts.isNotEmpty() && goodCounts.isNotEmpty() && bestCounts != goodCounts -> getString(R.string.ampersand, best, good)
            bestCounts.isNotEmpty() -> best
            goodCounts.isNotEmpty() -> good
            else -> ""
        }
        binding.playerRangeInclude.playerCountCommunityView.setTextOrHide(communityText)
        binding.playerRangeInclude.playerCountContainer.setOrClearOnClickListener(voteCount > 0) {
            GameSuggestedPlayerCountPollDialogFragment.launch(this)
        }
    }

    private fun List<GamePlayerPollResultsEntity>.asRange(comma: String = ", ", dash: String = " - "): String {
       return this.sortedBy { it.playerNumber }.fold(mutableListOf<MutableList<GamePlayerPollResultsEntity>>()) { accumulator, element ->
            val current = element.playerNumber
            val last = accumulator.lastOrNull()?.lastOrNull()?.playerNumber ?: Int.MAX_VALUE
            if (accumulator.isEmpty() || last != current - 1) {
                accumulator += mutableListOf(element)
            } else accumulator.last() += element
            accumulator
        }.joinToString(comma) {
            if (it.size == 1)
                it.first().playerCount
            else if (it.last().playerCount.endsWith('+'))
                it.first().playerCount + "+"
            else
                it.first().playerCount + dash + it.last().playerCount
        }
    }
}
