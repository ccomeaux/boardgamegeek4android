package com.boardgamegeek.ui

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.annotation.IdRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.dialog.EditCollectionTextDialogFragment
import com.boardgamegeek.ui.dialog.PrivateInfoDialogFragment
import com.boardgamegeek.ui.viewmodel.GameCollectionItemViewModel
import com.boardgamegeek.ui.widget.TextEditorView
import kotlinx.android.synthetic.main.fragment_game_collection_item.*
import org.jetbrains.anko.support.v4.withArguments

class GameCollectionItemFragment : Fragment(R.layout.fragment_game_collection_item) {
    private var gameId = BggContract.INVALID_ID
    private var collectionId = BggContract.INVALID_ID
    private var internalId = BggContract.INVALID_ID.toLong()
    private var isItemEditable = false
    private var isInEditMode = false
    private var isDirty = false
    private val viewModel by activityViewModels<GameCollectionItemViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        gameId = arguments?.getInt(KEY_GAME_ID, BggContract.INVALID_ID) ?: BggContract.INVALID_ID
        collectionId = arguments?.getInt(KEY_COLLECTION_ID, BggContract.INVALID_ID)
                ?: BggContract.INVALID_ID
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listOf(wantToBuyView, preorderedView, ownView, wantToPlayView, previouslyOwnedView, wantInTradeView, forTradeView, wishlistView).forEach {
            it.setOnCheckedChangeListener { view, _ ->
                if (view.isVisible && isInEditMode) {
                    if (view === wishlistView) {
                        wishlistPriorityView.isEnabled = wishlistView.isChecked
                    }
                    updateStatuses()
                }
            }
        }
        wishlistPriorityView.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (wishlistPriorityView.isVisible && wishlistPriorityView.isEnabled && wishlistView.isChecked && isInEditMode) {
                    updateStatuses()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
        wishlistPriorityView.adapter = WishlistPriorityAdapter(requireContext())
        commentView.setOnClickListener {
            onTextEditorClick(commentView, BggContract.Collection.COMMENT, BggContract.Collection.COMMENT_DIRTY_TIMESTAMP)
        }
        privateInfoCommentView.setOnClickListener {
            onTextEditorClick(privateInfoCommentView, BggContract.Collection.PRIVATE_INFO_COMMENT, BggContract.Collection.PRIVATE_INFO_DIRTY_TIMESTAMP)
        }
        wishlistCommentView.setOnClickListener {
            onTextEditorClick(wishlistCommentView, BggContract.Collection.WISHLIST_COMMENT, BggContract.Collection.WISHLIST_COMMENT_DIRTY_TIMESTAMP)
        }
        conditionView.setOnClickListener {
            onTextEditorClick(conditionView, BggContract.Collection.CONDITION, BggContract.Collection.TRADE_CONDITION_DIRTY_TIMESTAMP)
        }
        wantPartsView.setOnClickListener {
            onTextEditorClick(wantPartsView, BggContract.Collection.WANTPARTS_LIST, BggContract.Collection.WANT_PARTS_DIRTY_TIMESTAMP)
        }
        hasPartsView.setOnClickListener {
            onTextEditorClick(hasPartsView, BggContract.Collection.HASPARTS_LIST, BggContract.Collection.HAS_PARTS_DIRTY_TIMESTAMP)
        }
        privateInfoEditContainer.setOnClickListener {
            val privateInfoDialogFragment = PrivateInfoDialogFragment.newInstance(
                    editPrivateInfoView.getTag(R.id.priceCurrencyView).toString(),
                    getDoubleFromTag(editPrivateInfoView, R.id.priceView),
                    editPrivateInfoView.getTag(R.id.currentValueCurrencyView).toString(),
                    getDoubleFromTag(editPrivateInfoView, R.id.currentValueView),
                    getIntFromTag(editPrivateInfoView, R.id.quantityView),
                    editPrivateInfoView.getTag(R.id.acquisitionDateView).toString(),
                    editPrivateInfoView.getTag(R.id.acquiredFromView).toString(),
                    editPrivateInfoView.getTag(R.id.inventoryLocationView).toString()
            )
            this.showAndSurvive(privateInfoDialogFragment)
        }

        viewModel.swatch.observe(viewLifecycleOwner, Observer {
            it?.let { swatch ->
                listOf(privateInfoHeader, wishlistHeader, tradeHeader, privateInfoHintView).forEach { view ->
                    view.setTextColor(swatch.rgb)
                }
                listOf(commentView, privateInfoCommentView, wishlistCommentView, conditionView, wantPartsView, hasPartsView).forEach { view ->
                    view.setHeaderColor(swatch)
                }
            }
        })
        viewModel.item.observe(viewLifecycleOwner, Observer { (status, item, message) ->
            if (status == Status.ERROR) {
                showError(message)
                isItemEditable = false
            } else {
                if (item != null) {
                    internalId = item.internalId
                    isDirty = item.isDirty
                    updateUi(item)
                    isItemEditable = true
                } else {
                    internalId = BggContract.INVALID_ID.toLong()
                    isDirty = false
                    showError(getString(R.string.invalid_collection_status))
                    isItemEditable = false
                }
            }
        })
    }

    private fun showError(message: String) {
        invalidStatusView.text = message
        invalidStatusView.fadeIn()
        mainContainer.fadeOut()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.game_collection_fragment, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.menu_discard).isVisible = !isInEditMode && isDirty
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_discard -> {
                createDiscardDialog(requireActivity(), R.string.collection_item, R.string.keep, isNew = false, finishActivity = false) {
                    viewModel.reset()
                }.show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun enableEditMode(enable: Boolean) {
        if (!isItemEditable && enable) return
        isInEditMode = enable
        bindVisibility()
    }

    private fun bindVisibility() {
        listOf(statusEditContainer, privateInfoEditContainer, wishlistEditContainer, tradeEditContainer).forEach { view ->
            view.isVisible = isInEditMode
        }
        personalRatingView.enableEditMode(isInEditMode)
        commentView.enableEditMode(isInEditMode)
        privateInfoCommentView.enableEditMode(isInEditMode)
        wishlistCommentView.enableEditMode(isInEditMode)
        conditionView.enableEditMode(isInEditMode)
        wantPartsView.enableEditMode(isInEditMode)
        hasPartsView.enableEditMode(isInEditMode)
        listOf(statusView, viewPrivateInfoView, wishlistStatusView, tradeStatusView).forEach { view ->
            if (isInEditMode) {
                view.visibility = View.GONE
            } else {
                view.isVisible = getVisibleTag(view)
            }
        }
        val readonlyContainers = listOf(mainContainer, privateInfoContainer, wishlistContainer, tradeContainer)
        readonlyContainers.forEach { view -> setVisibilityByChildren(view) }
        invalidStatusView.isVisible = readonlyContainers.none { it.isVisible }
    }

    private fun updateStatuses() {
        val statuses = mutableListOf<String>()
        listOf(wantToBuyView, preorderedView, ownView, wantToPlayView, previouslyOwnedView, wantInTradeView, forTradeView, wishlistView).forEach { checkBox ->
            if (checkBox.isChecked) {
                (checkBox.tag as? String)?.let {
                    if (it.isNotBlank()) statuses.add(it)
                }
            }
        }
        val wishlistPriority = if (wishlistView.isChecked) wishlistPriorityView.selectedItemPosition + 1 else 0
        viewModel.updateStatuses(statuses, wishlistPriority)
    }

    private fun onTextEditorClick(view: TextEditorView, textColumn: String, timestampColumn: String) {
        showAndSurvive(EditCollectionTextDialogFragment.newInstance(
                view.headerText,
                view.contentText,
                textColumn,
                timestampColumn))
    }

    private fun getDoubleFromTag(textView: View?, @IdRes key: Int): Double? {
        return textView?.getTag(key) as? Double
    }

    private fun getIntFromTag(textView: View?, @IdRes key: Int): Int? {
        return textView?.getTag(key) as? Int
    }

    private fun updateUi(item: CollectionItemEntity) {
        bindMainContainer(item)
        bindWishlist(item)
        bindTrade(item)
        bindPrivateInfo(item)
        bindFooter(item)
        bindVisibility()
    }

    private fun bindMainContainer(item: CollectionItemEntity) {
        // view
        val statusDescription = getStatusDescription(item)
        statusView.setTextOrHide(statusDescription)
        setVisibleTag(statusView, statusDescription.isNotEmpty())
        // edit
        wantToBuyView.isChecked = item.wantToBuy
        preorderedView.isChecked = item.preOrdered
        ownView.isChecked = item.own
        wantToPlayView.isChecked = item.wantToPlay
        previouslyOwnedView.isChecked = item.previouslyOwned
        // both
        personalRatingView.setContent(item.rating, item.ratingDirtyTimestamp, gameId, collectionId, item.internalId)
        commentView.setContent(item.comment, item.commentDirtyTimestamp)
    }

    private fun bindWishlist(item: CollectionItemEntity) {
        // view
        if (item.wishList) {
            wishlistStatusView.setTextOrHide(item.wishListPriority.asWishListPriority(context))
        } else {
            wishlistStatusView.visibility = View.GONE
        }
        setVisibleTag(wishlistStatusView, item.wishList)

        // edit
        if (item.wishList) {
            wishlistPriorityView.setSelection(item.wishListPriority.coerceIn(1..5) - 1)
        }
        wishlistPriorityView.isEnabled = item.wishList
        wishlistView.isChecked = item.wishList
        wishlistCommentView.setContent(item.wishListComment, item.wishListDirtyTimestamp)
    }

    private fun bindTrade(item: CollectionItemEntity) {
        // view
        val statusDescriptions = mutableListOf<String>()
        if (item.forTrade) statusDescriptions.add(getString(R.string.collection_status_for_trade))
        if (item.wantInTrade) statusDescriptions.add(getString(R.string.collection_status_want_in_trade))
        tradeStatusView.setTextOrHide(statusDescriptions.formatList())
        setVisibleTag(tradeStatusView, item.forTrade || item.wantInTrade)

        // edit
        wantInTradeView.isChecked = item.wantInTrade
        forTradeView.isChecked = item.forTrade

        // both
        conditionView.setContent(item.conditionText, item.tradeConditionDirtyTimestamp)
        wantPartsView.setContent(item.wantPartsList, item.wantPartsDirtyTimestamp)
        hasPartsView.setContent(item.hasPartsList, item.hasPartsDirtyTimestamp)
    }

    private fun bindPrivateInfo(item: CollectionItemEntity) {
        // view
        viewPrivateInfoView.setTextOrHide(item.getPrivateInfo(requireContext()))
        setVisibleTag(viewPrivateInfoView, hasPrivateInfo(item))

        // edit
        privateInfoHintView.isVisible = !hasPrivateInfo(item)
        editPrivateInfoView.isVisible = hasPrivateInfo(item)
        editPrivateInfoView.text = item.getPrivateInfo(requireContext())
        editPrivateInfoView.setTag(R.id.priceCurrencyView, item.pricePaidCurrency)
        editPrivateInfoView.setTag(R.id.priceView, item.pricePaid)
        editPrivateInfoView.setTag(R.id.currentValueCurrencyView, item.currentValueCurrency)
        editPrivateInfoView.setTag(R.id.currentValueView, item.currentValue)
        editPrivateInfoView.setTag(R.id.quantityView, item.quantity)
        editPrivateInfoView.setTag(R.id.acquisitionDateView, item.acquisitionDate)
        editPrivateInfoView.setTag(R.id.acquiredFromView, item.acquiredFrom)
        editPrivateInfoView.setTag(R.id.inventoryLocationView, item.inventoryLocation)

        // both
        privateInfoCommentView.setContent(item.privateComment, item.privateInfoDirtyTimestamp)
    }

    private fun bindFooter(item: CollectionItemEntity) {
        lastModifiedView.timestamp = when {
            item.dirtyTimestamp > 0 -> item.dirtyTimestamp
            item.statusDirtyTimestamp > 0 -> item.statusDirtyTimestamp
            else -> item.lastModifiedDate
        }
        updatedView.timestamp = item.syncTimestamp
        idView.text = item.collectionId.toString()
    }

    private fun getStatusDescription(item: CollectionItemEntity): String {
        val statusDescriptions = mutableListOf<String>()
        if (item.own) statusDescriptions.add(getString(R.string.collection_status_own))
        if (item.previouslyOwned) statusDescriptions.add(getString(R.string.collection_status_prev_owned))
        if (item.wantToBuy) statusDescriptions.add(getString(R.string.collection_status_want_to_buy))
        if (item.wantToPlay) statusDescriptions.add(getString(R.string.collection_status_want_to_play))
        if (item.preOrdered) statusDescriptions.add(getString(R.string.collection_status_preordered))
        val status = statusDescriptions.formatList()
        return if (status.isEmpty() && item.numberOfPlays > 0) {
            getString(R.string.played)
        } else status
    }

    private class WishlistPriorityAdapter(context: Context) : ArrayAdapter<String?>(context,
            android.R.layout.simple_spinner_item,
            context.resources.getStringArray(R.array.wishlist_priority_finite)) {
        init {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun setVisibilityByChildren(view: ViewGroup) {
        for (i in 0 until view.childCount) {
            val child = view.getChildAt(i)
            if (setVisibilityByChild(view, child)) return
        }
        view.visibility = View.GONE
    }

    companion object {
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_COLLECTION_ID = "COLLECTION_ID"

        fun newInstance(gameId: Int, collectionId: Int): GameCollectionItemFragment {
            return GameCollectionItemFragment().withArguments(
                    KEY_GAME_ID to gameId,
                    KEY_COLLECTION_ID to collectionId
            )
        }

        private fun setVisibleTag(view: View, isVisible: Boolean) {
            view.setTag(R.id.visibility, isVisible)
        }

        private fun getVisibleTag(view: View): Boolean {
            return view.getTag(R.id.visibility) as? Boolean ?: false
        }

        fun hasPrivateInfo(item: CollectionItemEntity): Boolean {
            return item.quantity > 1 ||
                    item.acquisitionDate.isNotEmpty() ||
                    item.acquiredFrom.isNotEmpty() ||
                    item.pricePaid > 0.0 ||
                    item.currentValue > 0.0 ||
                    item.inventoryLocation.isNotEmpty()
        }

        private fun setVisibilityByChild(view: ViewGroup, child: View): Boolean {
            if (child is ViewGroup) {
                val tag = child.getTag() as? String?
                if (tag != null && tag == "container") {
                    for (j in 0 until child.childCount) {
                        val grandchild = child.getChildAt(j)
                        if (setVisibilityByChild(view, grandchild)) return true
                    }
                } else {
                    if (setVisibilityByChildView(view, child)) return true
                }
            } else {
                if (setVisibilityByChildView(view, child)) return true
            }
            return false
        }

        private fun setVisibilityByChildView(view: View, child: View): Boolean {
            val tag = child.tag as? String?
            if (tag != null && tag == "header") return false
            if (child.visibility == View.VISIBLE) {
                view.visibility = View.VISIBLE
                return true
            }
            return false
        }
    }
}