package com.boardgamegeek.ui

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentGameCollectionItemBinding
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.ui.dialog.*
import com.boardgamegeek.ui.viewmodel.GameCollectionItemViewModel

class GameCollectionItemFragment : Fragment() {
    private var _binding: FragmentGameCollectionItemBinding? = null
    private val binding get() = _binding!!
    private var gameId = INVALID_ID
    private var collectionId = INVALID_ID
    private var internalId = INVALID_ID.toLong()
    private var isInEditMode = false
    private var isDirty = false
    private val viewModel by activityViewModels<GameCollectionItemViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        gameId = arguments?.getInt(KEY_GAME_ID, INVALID_ID) ?: INVALID_ID
        collectionId = arguments?.getInt(KEY_COLLECTION_ID, INVALID_ID) ?: INVALID_ID
    }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentGameCollectionItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listOf(
            binding.wantToBuyView,
            binding.preorderedView,
            binding.ownView,
            binding.wantToPlayView,
            binding.previouslyOwnedView,
            binding.wantInTradeView,
            binding.forTradeView,
            binding.wishlistView
        ).forEach {
            it.setOnCheckedChangeListener { view, _ ->
                if (view.isVisible && isInEditMode) {
                    if (view === binding.wishlistView) {
                        binding.wishlistPriorityView.isEnabled = binding.wishlistView.isChecked
                    }
                    updateStatuses()
                }
            }
        }
        binding.wishlistPriorityView.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (binding.wishlistPriorityView.isVisible && binding.wishlistPriorityView.isEnabled && binding.wishlistView.isChecked && isInEditMode) {
                    updateStatuses()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
        binding.wishlistPriorityView.adapter = WishlistPriorityAdapter(requireContext())
        binding.commentView.setOnClickListener {
            EditCollectionCommentDialogFragment.show(parentFragmentManager, binding.commentView.contentText)
        }
        binding.privateInfoCommentView.setOnClickListener {
            EditCollectionPrivateCommentDialogFragment.show(parentFragmentManager, binding.privateInfoCommentView.contentText)
        }
        binding.wishlistCommentView.setOnClickListener {
            EditCollectionWishlistCommentDialogFragment.show(parentFragmentManager, binding.wishlistCommentView.contentText)
        }
        binding.conditionView.setOnClickListener {
            EditCollectionConditionDialogFragment.show(parentFragmentManager, binding.conditionView.contentText)
        }
        binding.wantPartsView.setOnClickListener {
            EditCollectionWantPartsDialogFragment.show(parentFragmentManager, binding.wantPartsView.contentText)
        }
        binding.hasPartsView.setOnClickListener {
            EditCollectionHasPartsDialogFragment.show(parentFragmentManager, binding.hasPartsView.contentText)
        }
        binding.privateInfoEditContainer.setOnClickListener {
            val privateInfoDialogFragment = PrivateInfoDialogFragment.newInstance(
                binding.editPrivateInfoView.getTag(R.id.priceCurrencyView).toString(),
                binding.editPrivateInfoView.getTag(R.id.priceView) as? Double,
                binding.editPrivateInfoView.getTag(R.id.currentValueCurrencyView).toString(),
                binding.editPrivateInfoView.getTag(R.id.currentValueView) as? Double,
                binding.editPrivateInfoView.getTag(R.id.quantityView) as? Int,
                binding.editPrivateInfoView.getTag(R.id.acquisitionDateView) as? Long,
                binding.editPrivateInfoView.getTag(R.id.acquiredFromView).toString(),
                binding.editPrivateInfoView.getTag(R.id.inventoryLocationView).toString()
            )
            this.showAndSurvive(privateInfoDialogFragment)
        }

        viewModel.isEditMode.observe(viewLifecycleOwner) {
            isInEditMode = it
            bindVisibility()
        }
        viewModel.swatch.observe(viewLifecycleOwner) {
            it?.let { swatch ->
                listOf(binding.privateInfoHeader, binding.wishlistHeader, binding.tradeHeader, binding.privateInfoHintView).forEach { view ->
                    view.setTextColor(swatch.rgb)
                }
                listOf(
                    binding.commentView,
                    binding.privateInfoCommentView,
                    binding.wishlistCommentView,
                    binding.conditionView,
                    binding.wantPartsView,
                    binding.hasPartsView
                ).forEach { view ->
                    view.setHeaderColor(swatch)
                }
            }
        }
        viewModel.item.observe(viewLifecycleOwner) {
            it?.let { (status, data, message) ->
                when (status) {
                    Status.REFRESHING -> binding.progressView.show()
                    Status.ERROR -> {
                        showError(message)
                        binding.progressView.hide()
                    }
                    Status.SUCCESS -> {
                        internalId = data?.internalId ?: INVALID_ID.toLong()
                        isDirty = data?.isDirty ?: false
                        if (data != null) {
                            updateUi(data)
                        } else {
                            showError(getString(R.string.invalid_collection_status))
                        }
                        binding.progressView.hide()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showError(message: String) {
        binding.invalidStatusView.text = message
        binding.invalidStatusView.isVisible = true
        binding.mainContainer.isVisible = false
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
                requireActivity().createDiscardDialog(R.string.collection_item, R.string.keep, isNew = false, finishActivity = false) {
                    viewModel.reset()
                }.show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun bindVisibility() {
        // show edit containers only when in edit mode
        listOf(
            binding.statusEditContainer,
            binding.privateInfoEditContainer,
            binding.wishlistEditContainer,
            binding.tradeEditContainer
        ).forEach { view ->
            view.isVisible = isInEditMode
        }

        binding.personalRatingView.enableEditMode(isInEditMode)
        binding.commentView.enableEditMode(isInEditMode)
        binding.privateInfoCommentView.enableEditMode(isInEditMode)
        binding.wishlistCommentView.enableEditMode(isInEditMode)
        binding.conditionView.enableEditMode(isInEditMode)
        binding.wantPartsView.enableEditMode(isInEditMode)
        binding.hasPartsView.enableEditMode(isInEditMode)

        listOf(binding.statusView, binding.viewPrivateInfoView, binding.wishlistStatusView, binding.tradeStatusView).forEach { view ->
            view.isVisible = if (isInEditMode) false else getVisibleTag(view)
        }
        val readonlyContainers = listOf(binding.mainContainer, binding.privateInfoContainer, binding.wishlistContainer, binding.tradeContainer)
        readonlyContainers.forEach { view -> setVisibilityByChildren(view) }
        //binding.invalidStatusView.isVisible = readonlyContainers.none { it.isVisible }
    }

    private fun updateStatuses() {
        val statuses = listOf(
            binding.wantToBuyView,
            binding.preorderedView,
            binding.ownView,
            binding.wantToPlayView,
            binding.previouslyOwnedView,
            binding.wantInTradeView,
            binding.forTradeView,
            binding.wishlistView
        ).filter {
            it.isChecked
        }.filterNot {
            (it.tag as? String).isNullOrBlank()
        }.map {
            it.tag as String
        }
        val wishlistPriority = if (binding.wishlistView.isChecked) binding.wishlistPriorityView.selectedItemPosition + 1 else 0
        viewModel.updateStatuses(statuses, wishlistPriority)
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
        binding.statusView.setTextOrHide(statusDescription)
        setVisibleTag(binding.statusView, statusDescription.isNotEmpty())
        // edit
        binding.wantToBuyView.isChecked = item.wantToBuy
        binding.preorderedView.isChecked = item.preOrdered
        binding.ownView.isChecked = item.own
        binding.wantToPlayView.isChecked = item.wantToPlay
        binding.previouslyOwnedView.isChecked = item.previouslyOwned
        // both
        binding.personalRatingView.setContent(item.rating, item.ratingDirtyTimestamp, gameId, collectionId, item.internalId)
        binding.commentView.setContent(item.comment, item.commentDirtyTimestamp)
    }

    private fun bindWishlist(item: CollectionItemEntity) {
        // view
        if (item.wishList) {
            binding.wishlistStatusView.setTextOrHide(item.wishListPriority.asWishListPriority(context))
        } else {
            binding.wishlistStatusView.visibility = View.GONE
        }
        setVisibleTag(binding.wishlistStatusView, item.wishList)

        // edit
        if (item.wishList) {
            binding.wishlistPriorityView.setSelection(item.wishListPriority.coerceIn(1..5) - 1)
        }
        binding.wishlistPriorityView.isEnabled = item.wishList
        binding.wishlistView.isChecked = item.wishList
        binding.wishlistCommentView.setContent(item.wishListComment, item.wishListDirtyTimestamp)
    }

    private fun bindTrade(item: CollectionItemEntity) {
        // view
        val statusDescriptions = mutableListOf<String>()
        if (item.forTrade) statusDescriptions += getString(R.string.collection_status_for_trade)
        if (item.wantInTrade) statusDescriptions += getString(R.string.collection_status_want_in_trade)
        binding.tradeStatusView.setTextOrHide(statusDescriptions.formatList())
        setVisibleTag(binding.tradeStatusView, item.forTrade || item.wantInTrade)

        // edit
        binding.wantInTradeView.isChecked = item.wantInTrade
        binding.forTradeView.isChecked = item.forTrade

        // both
        binding.conditionView.setContent(item.conditionText, item.tradeConditionDirtyTimestamp)
        binding.wantPartsView.setContent(item.wantPartsList, item.wantPartsDirtyTimestamp)
        binding.hasPartsView.setContent(item.hasPartsList, item.hasPartsDirtyTimestamp)
    }

    private fun bindPrivateInfo(item: CollectionItemEntity) {
        // view
        binding.viewPrivateInfoView.setTextOrHide(item.getPrivateInfo(requireContext()))
        setVisibleTag(binding.viewPrivateInfoView, hasPrivateInfo(item))

        // edit
        binding.privateInfoHintView.isVisible = !hasPrivateInfo(item)
        binding.editPrivateInfoView.isVisible = hasPrivateInfo(item)
        binding.editPrivateInfoView.text = item.getPrivateInfo(requireContext())
        binding.editPrivateInfoView.setTag(R.id.priceCurrencyView, item.pricePaidCurrency)
        binding.editPrivateInfoView.setTag(R.id.priceView, item.pricePaid)
        binding.editPrivateInfoView.setTag(R.id.currentValueCurrencyView, item.currentValueCurrency)
        binding.editPrivateInfoView.setTag(R.id.currentValueView, item.currentValue)
        binding.editPrivateInfoView.setTag(R.id.quantityView, item.quantity)
        binding.editPrivateInfoView.setTag(R.id.acquisitionDateView, item.acquisitionDate)
        binding.editPrivateInfoView.setTag(R.id.acquiredFromView, item.acquiredFrom)
        binding.editPrivateInfoView.setTag(R.id.inventoryLocationView, item.inventoryLocation)

        // both
        binding.privateInfoCommentView.setContent(item.privateComment, item.privateInfoDirtyTimestamp)
    }

    private fun bindFooter(item: CollectionItemEntity) {
        binding.lastModifiedView.timestamp = when {
            item.dirtyTimestamp > 0 -> item.dirtyTimestamp
            item.statusDirtyTimestamp > 0 -> item.statusDirtyTimestamp
            else -> item.lastModifiedDate
        }
        binding.updatedView.timestamp = item.syncTimestamp
        binding.idView.text = item.collectionId.toString()
    }

    private fun getStatusDescription(item: CollectionItemEntity): String {
        val statusDescriptions = mutableListOf<String>()
        if (item.own) statusDescriptions += getString(R.string.collection_status_own)
        if (item.previouslyOwned) statusDescriptions += getString(R.string.collection_status_prev_owned)
        if (item.wantToBuy) statusDescriptions += getString(R.string.collection_status_want_to_buy)
        if (item.wantToPlay) statusDescriptions += getString(R.string.collection_status_want_to_play)
        if (item.preOrdered) statusDescriptions += getString(R.string.collection_status_preordered)
        return if (statusDescriptions.isEmpty() && item.numberOfPlays > 0) {
            getString(R.string.played)
        } else statusDescriptions.formatList()
    }

    private class WishlistPriorityAdapter(context: Context) : ArrayAdapter<String?>(
        context,
        android.R.layout.simple_spinner_item,
        context.resources.getStringArray(R.array.wishlist_priority_finite)
    ) {
        init {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun setVisibilityByChildren(view: ViewGroup) {
        view.children.forEach { child ->
            if (setVisibilityByChild(view, child)) return
        }
        view.visibility = View.GONE
    }

    companion object {
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_COLLECTION_ID = "COLLECTION_ID"

        fun newInstance(gameId: Int, collectionId: Int): GameCollectionItemFragment {
            return GameCollectionItemFragment().apply {
                arguments = bundleOf(
                    KEY_GAME_ID to gameId,
                    KEY_COLLECTION_ID to collectionId,
                )
            }
        }

        private fun setVisibleTag(view: View, isVisible: Boolean) {
            view.setTag(R.id.visibility, isVisible)
        }

        private fun getVisibleTag(view: View): Boolean {
            return view.getTag(R.id.visibility) as? Boolean ?: false
        }

        fun hasPrivateInfo(item: CollectionItemEntity): Boolean {
            return item.quantity > 1 ||
                    item.acquisitionDate > 0L ||
                    item.acquiredFrom.isNotEmpty() ||
                    item.pricePaid > 0.0 ||
                    item.currentValue > 0.0 ||
                    item.inventoryLocation.isNotEmpty()
        }

        private fun setVisibilityByChild(view: ViewGroup, child: View): Boolean {
            if (child is ViewGroup) {
                val tag = child.getTag() as? String?
                if (tag != null && tag == "container") {
                    child.children.forEach { grandchild ->
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

        /** Show the view if the child is visible. **/
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
