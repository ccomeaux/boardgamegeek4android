package com.boardgamegeek.ui.widget

import android.animation.AnimatorSet
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.boardgamegeek.R
import timber.log.Timber

class FastScroller @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private var bubble: View? = null
    //private var handle: View? = null

    private var currentAnimator: AnimatorSet? = null

    private var scrollerHeight: Int = 0

    internal var recyclerView: RecyclerView? = null

    private val scrollListener = ScrollListener()

    private val handleHider = HandleHider()


    init {
        LayoutInflater.from(context).inflate(R.layout.fastscroller, this)
        orientation = LinearLayout.HORIZONTAL
        clipChildren = false
        bubble = findViewById(R.id.fastscroller_bubble)
        //handle = findViewById(R.id.fastscroller_handle)
    }

//    private fun showHandle() {
//        val animatorSet = AnimatorSet()
//        handle!!.pivotX = handle!!.width.toFloat()
//        handle!!.pivotY = handle!!.height.toFloat()
//        handle!!.visibility = View.VISIBLE
//        val growerX = ObjectAnimator.ofFloat(handle, SCALE_X, 0f, 1f).setDuration(HANDLE_ANIMATION_DURATION.toLong())
//        val growerY = ObjectAnimator.ofFloat(handle, SCALE_Y, 0f, 1f).setDuration(HANDLE_ANIMATION_DURATION.toLong())
//        val alpha = ObjectAnimator.ofFloat(handle, ALPHA, 0f, 1f).setDuration(HANDLE_ANIMATION_DURATION.toLong())
//        animatorSet.playTogether(growerX, growerY, alpha)
//        animatorSet.start()
//    }
//
//    private fun hideHandle() {
//        currentAnimator = AnimatorSet()
//        handle!!.pivotX = handle!!.width.toFloat()
//        handle!!.pivotY = handle!!.height.toFloat()
//        val shrinkerX = ObjectAnimator.ofFloat(handle, SCALE_X, 1f, 0f).setDuration(HANDLE_ANIMATION_DURATION.toLong())
//        val shrinkerY = ObjectAnimator.ofFloat(handle, SCALE_Y, 1f, 0f).setDuration(HANDLE_ANIMATION_DURATION.toLong())
//        val alpha = ObjectAnimator.ofFloat(handle, ALPHA, 1f, 0f).setDuration(HANDLE_ANIMATION_DURATION.toLong())
//        currentAnimator!!.playTogether(shrinkerX, shrinkerY, alpha)
//        currentAnimator!!.addListener(object : AnimatorListenerAdapter() {
//            override fun onAnimationEnd(animation: Animator) {
//                super.onAnimationEnd(animation)
//                handle!!.visibility = View.INVISIBLE
//                currentAnimator = null
//            }
//
//            override fun onAnimationCancel(animation: Animator) {
//                super.onAnimationCancel(animation)
//                handle!!.visibility = View.INVISIBLE
//                currentAnimator = null
//            }
//        })
//        currentAnimator!!.start()
//    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        scrollerHeight = h
    }

    private fun setPosition(y: Float) {
        val position = y / scrollerHeight
        val bubbleHeight = bubble?.height ?: 0
        bubble?.y = ((scrollerHeight - bubbleHeight) * position).toInt().coerceIn(0, scrollerHeight - bubbleHeight).toFloat()
//        val handleHeight = handle!!.height
//        handle!!.y = getValueInRange(0, scrollerHeight - handleHeight, ((scrollerHeight - handleHeight) * position).toInt()).toFloat()
    }

    fun setRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        recyclerView.addOnScrollListener(scrollListener)
    }

    private inner class ScrollListener : OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            val firstVisibleView = recyclerView.getChildAt(0)
//            val firstVisiblePosition = recyclerView.getChildAdapterPosition(firstVisibleView)
//            val lastVisiblePosition = firstVisiblePosition + recyclerView.childCount
//            val itemCount = recyclerView.adapter?.itemCount ?: 0
//            val position = when {
//                firstVisiblePosition == 0 -> 0
//                lastVisiblePosition == itemCount - 1 -> itemCount - 1
//                else -> firstVisiblePosition
//            }
//            val proportion = position.toFloat() / itemCount.toFloat()
//            Timber.d("$proportion")
            //   setPosition(scrollerHeight * proportion)

            val constant = firstVisibleView.height * recyclerView.getChildAdapterPosition(firstVisibleView)
            val scrollY = paddingTop + constant - (recyclerView.layoutManager?.getDecoratedTop(firstVisibleView) ?: 0)

            val scrollHeight = paddingTop +
                    ((recyclerView.layoutManager?.itemCount ?: 0) * firstVisibleView.height) +
                    paddingBottom -
                    height
            val barHeight = height - (bubble?.height ?: 0)
            //val bubbleHeight = bubble?.height ?: 0
            Timber.e("$scrollY ($constant) / $scrollHeight * $barHeight")
            bubble?.y = scrollY.toFloat() / scrollHeight * barHeight
        }
    }

    override fun onTouchEvent(@NonNull event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            setPosition(event.y)
            currentAnimator?.cancel()
            handler.removeCallbacks(handleHider)
//            if (handle!!.visibility == View.INVISIBLE) {
//                //showHandle()
//            }
            setRecyclerViewPosition(event.y)
            return true
        } else if (event.action == MotionEvent.ACTION_UP) {
            handler.postDelayed(handleHider, HANDLE_HIDE_DELAY.toLong())
            return true
        }
        return super.onTouchEvent(event)
    }

    private inner class HandleHider : Runnable {
        override fun run() {
            //  hideHandle()
        }
    }

    private fun setRecyclerViewPosition(y: Float) {
        recyclerView?.let {
            val itemCount = it.adapter?.itemCount ?: 0
            val proportion = when {
                bubble!!.y == 0f -> 0f
                bubble!!.y + bubble!!.height >= scrollerHeight - TRACK_SNAP_RANGE -> 1f
                else -> y / scrollerHeight.toFloat()
            }
            val targetPos = (proportion * itemCount.toFloat()).toInt().coerceIn(0, itemCount - 1)
            it.scrollToPosition(targetPos)
        }
    }

    companion object {
        private const val HANDLE_ANIMATION_DURATION = 100

        private const val SCALE_X = "scaleX"
        private const val SCALE_Y = "scaleY"
        private const val ALPHA = "alpha"

        private const val HANDLE_HIDE_DELAY = 1000
        private const val TRACK_SNAP_RANGE = 5
    }
}