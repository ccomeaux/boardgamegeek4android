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
import com.boardgamegeek.ui.widget.GameDetailRow

class GameFragment : Fragment() {
    private var _binding: FragmentGameBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<GameViewModel>()
    private var gameId = BggContract.INVALID_ID
    private var gameName = ""

    @Suppress("DEPRECATION")
    private val rankSeparator = "  ${Html.fromHtml("&#9679;")}  "

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.dataContainer.layoutTransition.setAnimateParentHierarchy(false)
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
                viewModel.expansions.observe(viewLifecycleOwner) { it?.let { entity -> onListQueryComplete(entity, binding.expansionsRow) } }
                viewModel.baseGames.observe(viewLifecycleOwner) { it?.let { entity -> onListQueryComplete(entity, binding.baseGamesRow) } }
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
            binding.dataContainer.isVisible = false
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

        listOf(binding.expansionsRow, binding.baseGamesRow).forEach { it.colorize(iconColor) }
    }

    private fun onGameContentChanged(game: GameEntity) {
        colorize(game.iconColor)

        gameName = game.name

        binding.ranksInclude.rankView.text = game.overallRank.asRank(requireContext(), game.subtype)
        binding.ranksInclude.rankContainer.setOnClickListener { GameRanksDialogFragment.launch(this) }

        binding.ratingsInclude.ratingView.text = game.rating.asRating(context)
        binding.ratingsInclude.ratingView.setTextViewBackground(game.rating.toColor(ratingColors))
        val numberOfRatings = requireContext().getQuantityText(R.plurals.ratings_suffix, game.numberOfRatings, game.numberOfRatings)
        val numberOfComments = requireContext().getQuantityText(R.plurals.comments_suffix, game.numberOfComments, game.numberOfComments)
        binding.ratingsInclude.ratingVotesView.text = getString(R.string.ampersand, numberOfRatings, numberOfComments)
        binding.ratingsInclude.ratingContainer.setOrClearOnClickListener(game.numberOfRatings > 0 || game.numberOfComments > 0) {
            CommentsActivity.startRating(requireContext(), gameId, gameName)
        }

        binding.yearInclude.yearView.text = game.yearPublished.asYear(context)

        binding.playingTimeInclude.playTimeView.text =
            requireContext().getQuantityText(R.plurals.mins_suffix, game.maxPlayingTime, (game.minPlayingTime to game.maxPlayingTime).asRange())

        binding.playerRangeInclude.playerCountView.text =
            requireContext().getQuantityText(R.plurals.player_range_suffix, game.maxPlayers, (game.minPlayers to game.maxPlayers).asRange())

        binding.agesInclude.playerAgeView.text = game.minimumAge.asAge(context)

        binding.weightInclude.weightView.text = game.averageWeight.toDescription(requireContext(), R.array.game_weight, R.string.unknown_weight)
        if (game.averageWeight == 0.0) {
            binding.weightInclude.weightScoreView.isVisible = false
        } else {
            binding.weightInclude.weightScoreView.setTextOrHide(game.averageWeight.asScore(context))
        }
        val textColor = binding.weightInclude.weightColorView.setTextViewBackground(game.averageWeight.toColor(fiveStageColors))
        binding.weightInclude.weightView.setTextColor(textColor)
        binding.weightInclude.weightScoreView.setTextColor(textColor)
        binding.weightInclude.weightVotesView.setTextOrHide(
            requireContext().getQuantityText(
                R.plurals.votes_suffix,
                game.numberOfUsersWeighting,
                game.numberOfUsersWeighting
            )
        )

        binding.footer.gameIdView.text = game.id.toString()
        binding.footer.lastModifiedView.timestamp = game.updated

        binding.emptyMessage.isVisible = false
        binding.dataContainer.isVisible = true
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
            binding.languageInclude.languageScoreView.setTextOrHide(score.asScore(context))
        }
        binding.languageInclude.languageVotesView.setTextOrHide(requireContext().getQuantityText(R.plurals.votes_suffix, totalVotes, totalVotes))

        val textColor = binding.languageInclude.languageColorView.setTextViewBackground(score.toColor(fiveStageColors))
        binding.languageInclude.languageView.setTextColor(textColor)
        binding.languageInclude.languageScoreView.setTextColor(textColor)

        binding.languageInclude.languageContainer.setOrClearOnClickListener(totalVotes > 0) {
            GamePollDialogFragment.launchLanguageDependence(this)
        }
    }

    private fun onAgePollQueryComplete(entity: GamePollEntity?) {
        val message = if (entity?.modalValue.isNullOrBlank()) ""
        else requireContext().getText(R.string.age_community, entity?.modalValue.orEmpty())
        binding.agesInclude.playerAgePollView.setTextOrHide(message)
        binding.agesInclude.playerAgeContainer.setOrClearOnClickListener(entity?.totalVotes ?: 0 > 0) {
            GamePollDialogFragment.launchSuggestedPlayerAge(this)
        }
    }

    private fun onPlayerCountQueryComplete(entity: GamePlayerPollEntity?) {
        val bestCounts = entity?.bestCounts ?: emptySet()
        val goodCounts = entity?.recommendedAndBestCounts ?: emptySet()

        val best = requireContext().getText(R.string.best_prefix, bestCounts.asRange(max = GamePlayerPollEntity.maxPlayerCount))
        val good = requireContext().getText(R.string.recommended_prefix, goodCounts.asRange(max = GamePlayerPollEntity.maxPlayerCount))
        val communityText = when {
            bestCounts.isNotEmpty() && goodCounts.isNotEmpty() && bestCounts != goodCounts -> getString(R.string.ampersand, best, good)
            bestCounts.isNotEmpty() -> best
            goodCounts.isNotEmpty() -> good
            else -> ""
        }
        binding.playerRangeInclude.playerCountCommunityView.setTextOrHide(communityText)
        binding.playerRangeInclude.playerCountContainer.setOrClearOnClickListener(entity?.totalVotes ?: 0 > 0) {
            GameSuggestedPlayerCountPollDialogFragment.launch(this)
        }
    }

    private fun onListQueryComplete(list: List<GameDetailEntity>, view: GameDetailRow) {
        view.bindData(viewModel.gameId.value ?: BggContract.INVALID_ID, gameName, list)
    }
}
