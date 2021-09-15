package com.boardgamegeek.ui

import android.graphics.*
import android.os.Bundle
import android.view.*
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.extensions.colorize
import com.boardgamegeek.extensions.fade
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.extensions.showAndSurvive
import com.boardgamegeek.ui.adapter.GameColorRecyclerViewAdapter
import com.boardgamegeek.ui.dialog.AddColorToGameDialogFragment
import com.boardgamegeek.ui.viewmodel.GameColorsViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_colors.*
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.support.v4.withArguments
import kotlin.math.max
import kotlin.math.min

class GameColorsFragment : Fragment(R.layout.fragment_colors) {
    @ColorInt
    private var iconColor = 0
    private var actionMode: ActionMode? = null
    private val swipePaint = Paint()
    private var deleteIcon: Bitmap? = null

    private val viewModel by activityViewModels<GameColorsViewModel>()

    private val adapter: GameColorRecyclerViewAdapter by lazy {
        createAdapter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fab.colorize(iconColor)
        setUpRecyclerView()
    }

    private fun setUpRecyclerView() {
        recyclerView.adapter = adapter
        swipePaint.color = ContextCompat.getColor(requireContext(), R.color.delete)
        deleteIcon = BitmapFactory.decodeResource(resources, R.drawable.ic_delete_white)
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
                adapter.getColorName(viewHolder.adapterPosition)?.let { color ->
                    viewModel.removeColor(color)
                    Snackbar.make(containerView, getString(R.string.msg_color_deleted, color), Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.undo) { viewModel.addColor(color) }
                            .show()
                }
            }

            override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                return if (actionMode != null) 0 else super.getSwipeDirs(recyclerView, viewHolder)
            }

            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                val horizontalPadding = requireContext().resources.getDimension(R.dimen.material_margin_horizontal)

                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView
                    deleteIcon?.let {
                        val verticalPadding = (itemView.height - it.height) / 2.toFloat()
                        val background: RectF
                        val iconSrc: Rect
                        val iconDst: RectF
                        if (dX > 0) {
                            background = RectF(itemView.left.toFloat(), itemView.top.toFloat(), dX, itemView.bottom.toFloat())
                            iconSrc = Rect(0, 0, min((dX - itemView.left - horizontalPadding).toInt(), it.width), it.height)
                            iconDst = RectF(itemView.left.toFloat() + horizontalPadding, itemView.top.toFloat() + verticalPadding, min(itemView.left + horizontalPadding + it.width, dX), itemView.bottom.toFloat() - verticalPadding)
                        } else {
                            background = RectF(itemView.right.toFloat() + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
                            iconSrc = Rect(max(it.width + horizontalPadding.toInt() + dX.toInt(), 0), 0, it.width, it.height)
                            iconDst = RectF(max(itemView.right.toFloat() + dX, itemView.right.toFloat() - horizontalPadding - it.width), itemView.top.toFloat() + verticalPadding, itemView.right.toFloat() - horizontalPadding, itemView.bottom.toFloat() - verticalPadding)
                        }
                        c.drawRect(background, swipePaint)
                        c.drawBitmap(it, iconSrc, iconDst, swipePaint)
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        arguments?.let {
            iconColor = it.getInt(KEY_ICON_COLOR, Color.TRANSPARENT)
        }

        fab.colorize(iconColor)
        fab.setOnClickListener {
            requireActivity().showAndSurvive(AddColorToGameDialogFragment())
        }

        viewModel.colors.observe(viewLifecycleOwner, Observer {
            adapter.colors = it

            emptyView.fade(it.isEmpty())
            recyclerView.fade(it.isNotEmpty(), isResumed)
            fab.show()
            progressView.fadeOut()

        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.game_colors, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.menu_colors_generate) {
            viewModel.computeColors()
            containerView.snackbar(R.string.msg_colors_generated)
            true
        } else super.onOptionsItemSelected(item)
    }

    private fun createAdapter(): GameColorRecyclerViewAdapter {
        return GameColorRecyclerViewAdapter(object : GameColorRecyclerViewAdapter.Callback {
            override fun onItemClick(position: Int) {
                if (actionMode != null) {
                    toggleSelection(position)
                }
            }

            override fun onItemLongPress(position: Int): Boolean {
                if (actionMode != null) return false
                actionMode = requireActivity().startActionMode(object : ActionMode.Callback {
                    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                        mode.menuInflater.inflate(R.menu.colors_context, menu)
                        adapter.clearSelections()
                        fab.hide()
                        return true
                    }

                    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                        return false
                    }

                    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                        when (item.itemId) {
                            R.id.menu_delete -> {
                                val colors = adapter.getSelectedColors()
                                val count = colors.size
                                colors.forEach { viewModel.removeColor(it) }
                                containerView.snackbar(resources.getQuantityString(R.plurals.msg_colors_deleted, count, count))
                                mode.finish()
                                return true
                            }
                        }
                        mode.finish()
                        return false
                    }

                    override fun onDestroyActionMode(mode: ActionMode) {
                        actionMode = null
                        adapter.clearSelections()
                        fab.show()
                    }
                })
                if (actionMode == null) return false
                toggleSelection(position)
                return true
            }

            private fun toggleSelection(position: Int) {
                adapter.toggleSelection(position)
                val count = adapter.selectedItemCount
                if (count == 0) {
                    actionMode?.finish()
                } else {
                    actionMode?.title = resources.getQuantityString(R.plurals.msg_colors_selected, count, count)
                    actionMode?.invalidate()
                }
            }
        })
    }

    companion object {
        private const val KEY_ICON_COLOR = "ICON_COLOR"

        fun newInstance(@ColorInt iconColor: Int): GameColorsFragment {
            return GameColorsFragment().withArguments(
                    KEY_ICON_COLOR to iconColor
            )
        }
    }
}
