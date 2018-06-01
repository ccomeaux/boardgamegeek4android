package com.boardgamegeek.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.graphics.Color
import android.os.Bundle
import android.support.annotation.ColorInt
import android.support.v4.app.Fragment
import android.text.TextUtils
import android.view.*
import butterknife.ButterKnife
import com.boardgamegeek.*
import com.boardgamegeek.entities.GamePlayerPollEntity
import com.boardgamegeek.entities.GamePollEntity
import com.boardgamegeek.entities.GameRankEntity
import com.boardgamegeek.io.BggService
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.Games
import com.boardgamegeek.tasks.FavoriteGameTask
import com.boardgamegeek.ui.dialog.GameUsersDialogFragment
import com.boardgamegeek.ui.dialog.RanksFragment
import com.boardgamegeek.ui.model.Game
import com.boardgamegeek.ui.model.Status
import com.boardgamegeek.ui.viewmodel.GameViewModel
import com.boardgamegeek.ui.widget.GameDetailRow
import com.boardgamegeek.ui.widget.SafeViewTarget
import com.boardgamegeek.util.*
import com.github.amlcurran.showcaseview.targets.Target
import kotlinx.android.synthetic.main.fragment_game.*
import kotlinx.android.synthetic.main.include_game_ages.*
import kotlinx.android.synthetic.main.include_game_footer.*
import kotlinx.android.synthetic.main.include_game_language_dependence.*
import kotlinx.android.synthetic.main.include_game_player_range.*
import kotlinx.android.synthetic.main.include_game_playing_time.*
import kotlinx.android.synthetic.main.include_game_ranks.*
import kotlinx.android.synthetic.main.include_game_ratings.*
import kotlinx.android.synthetic.main.include_game_users.*
import kotlinx.android.synthetic.main.include_game_weight.*
import kotlinx.android.synthetic.main.include_game_year_published.*
import org.jetbrains.anko.support.v4.act
import org.jetbrains.anko.support.v4.ctx
import java.util.*

class GameFragment : Fragment() {
    private var gameId: Int = BggContract.INVALID_ID
    private var gameName: String = ""
    private var showcaseViewWizard: ShowcaseViewWizard? = null

    private val viewModel: GameViewModel by lazy {
        ViewModelProviders.of(act).get(GameViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_game, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gameId = arguments?.getInt(KEY_GAME_ID, BggContract.INVALID_ID) ?: BggContract.INVALID_ID
        if (gameId == BggContract.INVALID_ID) throw IllegalArgumentException("Invalid game ID")
        gameName = arguments?.getString(KEY_GAME_NAME) ?: ""


        swipeRefresh?.setOnRefreshListener { viewModel.refresh() }
        swipeRefresh?.setColorSchemeResources(*PresentationUtils.getColorSchemeResources())

        game_info_id?.text = gameId.toString()
        game_info_last_updated?.timestamp = 0

        viewModel.game.observe(this, Observer {
            swipeRefresh?.post { swipeRefresh?.isRefreshing = it?.status == Status.REFRESHING }
            when {
                it == null -> showError(getString(R.string.empty_game))
                it.status == Status.ERROR && it.data == null -> showError(it.message)
                it.data == null -> showError(getString(R.string.empty_game))
                else -> onGameContentChanged(it.data)
            }
            progress.hide()
        })

        viewModel.ranks.observe(this, Observer { gameRanks -> onRankQueryComplete(gameRanks) })

        viewModel.languagePoll.observe(this, Observer { gamePollEntity -> onLanguagePollQueryComplete(gamePollEntity) })

        viewModel.agePoll.observe(this, Observer { gameSuggestedAgePollEntity -> onAgePollQueryComplete(gameSuggestedAgePollEntity) })

        viewModel.playerPoll.observe(this, Observer { gamePlayerPollEntities -> onPlayerCountQueryComplete(gamePlayerPollEntities) })

        viewModel.designers.observe(this, Observer { gameDetails -> onListQueryComplete(gameDetails, game_info_designers) })

        viewModel.artists.observe(this, Observer { gameDetails -> onListQueryComplete(gameDetails, game_info_artists) })

        viewModel.publishers.observe(this, Observer { gameDetails -> onListQueryComplete(gameDetails, game_info_publishers) })

        viewModel.categories.observe(this, Observer { gameDetails -> onListQueryComplete(gameDetails, game_info_categories) })

        viewModel.mechanics.observe(this, Observer { gameDetails -> onListQueryComplete(gameDetails, game_info_mechanics) })

        viewModel.expansions.observe(this, Observer { gameDetails -> onListQueryComplete(gameDetails, game_info_expansions) })

        viewModel.baseGames.observe(this, Observer { gameDetails -> onListQueryComplete(gameDetails, game_info_base_games) })

        showcaseViewWizard = setUpShowcaseViewWizard()
        showcaseViewWizard?.maybeShowHelp()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.help, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.menu_help) {
            showcaseViewWizard?.showHelp()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setUpShowcaseViewWizard(): ShowcaseViewWizard {
        val wizard = ShowcaseViewWizard(act, HelpUtils.HELP_GAME_KEY, HELP_VERSION)
        wizard.addTarget(R.string.help_game_menu, Target.NONE)
        wizard.addTarget(R.string.help_game_poll, SafeViewTarget(R.id.number_of_players, act))
        wizard.addTarget(-1, SafeViewTarget(R.id.playerAgeContainer, act))
        return wizard
    }

    private fun showError(message: String?) {
        if (message?.isNotBlank() == true) {
            emptyMessage.text = message
            dataContainer.fadeOut()
            emptyMessage.fadeIn()
        }
    }

    private fun colorize(@ColorInt iconColor: Int) {
        if (!isAdded) return

        val colorizedIcons = listOf(favoriteIcon, ratingIcon, yearIcon, playTimeIcon, playerCountIcon, playerAgeIcon, weightIcon, languageIcon, usersIcon)
        if (iconColor != Color.TRANSPARENT) {
            colorizedIcons.forEach { it?.setColorFilter(iconColor) }

            val colorizedRows = listOf(game_info_designers, game_info_artists, game_info_publishers, game_info_categories, game_info_mechanics, game_info_expansions, game_info_base_games)
            ButterKnife.apply(colorizedRows, GameDetailRow.rgbIconSetter, iconColor)
        } else {
            colorizedIcons.forEach { it?.clearColorFilter() }
        }
    }

    private fun onGameContentChanged(game: Game) {
        colorize(game.iconColor)

        gameName = game.name

        favoriteIcon?.setImageResource(if (game.isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
        favoriteIcon?.setTag(R.id.favorite, game.isFavorite)
        favoriteIcon?.setOnClickListener {
            val isFavorite = favoriteIcon?.getTag(R.id.favorite) as? Boolean? ?: false
            TaskUtils.executeAsyncTask(FavoriteGameTask(ctx, gameId, !isFavorite))
        }

        rankView?.text = PresentationUtils.describeRank(ctx, game.rank, BggService.RANK_TYPE_SUBTYPE, game.subtype)
        rankContainer?.setOnClickListener { RanksFragment.launch(this, gameId) }

        ratingView?.text = PresentationUtils.describeRating(ctx, game.rating)
        ratingView.setTextViewBackground(ColorUtils.getRatingColor(game.rating))
        val numberOfRatings = PresentationUtils.getQuantityText(ctx, R.plurals.ratings_suffix, game.usersRated, game.usersRated)
        val numberOfComments = PresentationUtils.getQuantityText(ctx, R.plurals.comments_suffix, game.usersCommented, game.usersCommented)
        ratingVotesView?.text = TextUtils.concat(numberOfRatings, " & ", numberOfComments)
        if (game.usersRated > 0 || game.usersCommented > 0) {
            ratingContainer?.setOnClickListener { CommentsActivity.startRating(ctx, Games.buildGameUri(gameId), gameName) }
        } else {
            ratingContainer?.setOnClickListener { }
            ratingContainer?.isClickable = false
        }

        yearView?.text = PresentationUtils.describeYear(ctx, game.yearPublished)

        playTimeView?.text = PresentationUtils.describeMinuteRange(ctx, game.minPlayingTime, game.maxPlayingTime, game.playingTime)

        playerCountView?.text = PresentationUtils.describePlayerRange(ctx, game.minPlayers, game.maxPlayers)

        playerAgeView?.text = PresentationUtils.describePlayerAge(ctx, game.minimumAge)

        weightView?.text = PresentationUtils.describeWeight(ctx, game.averageWeight)
        weightView.setTextViewBackground(ColorUtils.getFiveStageColor(game.averageWeight))
        weightScoreView?.setTextOrHide(PresentationUtils.describeScore(ctx, game.averageWeight))
        weightScoreView.setTextOrHide(PresentationUtils.getQuantityText(ctx, R.plurals.votes_suffix, game.numberWeights, game.numberWeights))

        usersView?.text = PresentationUtils.getQuantityText(ctx, R.plurals.users_suffix, game.maxUsers, game.maxUsers)
        if (game.maxUsers > 0) {
            usersContainer?.setOnClickListener { GameUsersDialogFragment.launch(this, gameId) }
        } else {
            usersContainer?.setOnClickListener { }
            usersContainer?.isClickable = false
        }

        game_info_id?.text = game.id.toString()
        game_info_last_updated?.timestamp = game.updated

        emptyMessage?.fadeOut()
        dataContainer?.fadeIn()
    }

    private fun onListQueryComplete(list: List<Pair<Int, String>>?, view: GameDetailRow?) {
        view?.bindData(gameId, gameName, list)
    }

    private fun onRankQueryComplete(gameRanks: List<GameRankEntity>?) {
        if (gameRanks == null || gameRanks.isEmpty()) {
            subtypeView?.visibility = View.GONE
        } else {
            var cs: CharSequence? = null
            for (rank in gameRanks) {
                if (rank.isFamilyType) {
                    val rankDescription = PresentationUtils.describeRank(ctx, rank.value, rank.type, rank.name)
                    cs = if (cs != null) {
                        PresentationUtils.getText(ctx, R.string.rank_div, cs, rankDescription)
                    } else {
                        rankDescription
                    }
                }
            }
            subtypeView.setTextOrHide(cs)
        }
    }

    private fun onLanguagePollQueryComplete(entity: GamePollEntity?) {
        val score = entity?.calculateScore() ?: 0.0
        languageView?.text = PresentationUtils.describeLanguageDependence(ctx, score)
        languageView?.setTextViewBackground(ColorUtils.getFiveStageColor(score))
        languageScoreView?.setTextOrHide(PresentationUtils.describeScore(ctx, score))
        val totalVotes = entity?.totalVotes ?: 0
        languageVotesView?.setTextOrHide(PresentationUtils.getQuantityText(ctx, R.plurals.votes_suffix, totalVotes, totalVotes))
        if (entity?.totalVotes ?: 0 > 0) {
            languageContainer?.setOnClickListener { PollFragment.launchLanguageDependence(this, gameId) }
        } else {
            languageContainer?.setOnClickListener { }
            languageContainer?.isClickable = false
        }
    }

    private fun onAgePollQueryComplete(entity: GamePollEntity?) {
        if (entity != null && entity.modalValue.isNotBlank()) {
            playerAgePollView?.setTextOrHide(PresentationUtils.describePlayerAge(ctx, entity.modalValue))
        } else {
            playerAgePollView.visibility = View.GONE
        }
        if (entity?.totalVotes ?: 0 > 0) {
            playerAgeContainer?.setOnClickListener { PollFragment.launchSuggestedPlayerAge(this, gameId) }
        } else {
            playerAgeContainer?.setOnClickListener { }
            playerAgeContainer?.isClickable = false
        }
    }

    private fun onPlayerCountQueryComplete(list: List<GamePlayerPollEntity>?) {
        var totalVotes = 0
        if (list != null) {
            val bestCounts = ArrayList<Int>()
            val recommendedCounts = ArrayList<Int>()
            for ((totalVotes1, playerCount, recommendation) in list) {
                totalVotes = Math.max(totalVotes, totalVotes1)
                if (recommendation == PlayerCountRecommendation.BEST) {
                    bestCounts.add(playerCount)
                    recommendedCounts.add(playerCount)
                } else if (recommendation == PlayerCountRecommendation.RECOMMENDED) {
                    recommendedCounts.add(playerCount)
                }
            }

            var communityText: CharSequence = ""
            if (bestCounts.size > 0) {
                communityText = PresentationUtils.getText(ctx, R.string.best_prefix, StringUtils.formatRange(bestCounts))
                if (recommendedCounts.size > 0 && bestCounts != recommendedCounts) {
                    val good = PresentationUtils.getText(ctx, R.string.recommended_prefix, StringUtils.formatRange(recommendedCounts))
                    communityText = TextUtils.concat(communityText, " & ", good)
                }
            } else if (recommendedCounts.size > 0) {
                communityText = PresentationUtils.getText(ctx, R.string.recommended_prefix, StringUtils.formatRange(recommendedCounts))
            }
            playerCountCommunityView?.setTextOrHide(communityText)
        } else {
            playerCountCommunityView?.visibility = View.GONE
        }
        playerCountVotesView.setTextOrHide(PresentationUtils.getQuantityText(ctx, R.plurals.votes_suffix, totalVotes, totalVotes))
        if (totalVotes > 0) {
            playerCountContainer?.setOnClickListener { SuggestedPlayerCountPollFragment.launch(this, gameId) }
        } else {
            playerCountContainer?.setOnClickListener { }
            playerCountContainer?.isClickable = false
        }
    }

    companion object {
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_GAME_NAME = "GAME_NAME"

        private const val HELP_VERSION = 2

        fun newInstance(gameId: Int, gameName: String): GameFragment {
            val args = Bundle()
            args.putInt(KEY_GAME_ID, gameId)
            args.putString(KEY_GAME_NAME, gameName)
            val fragment = GameFragment()
            fragment.arguments = args
            return fragment
        }
    }
}