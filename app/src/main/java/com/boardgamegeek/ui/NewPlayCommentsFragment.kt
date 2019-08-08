package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.boardgamegeek.R
import com.boardgamegeek.ui.viewmodel.NewPlayViewModel
import kotlinx.android.synthetic.main.fragment_new_play_comments.*

class NewPlayCommentsFragment : Fragment() {
    private val viewModel: NewPlayViewModel by lazy {
        ViewModelProviders.of(requireActivity()).get(NewPlayViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_new_play_comments, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        doneButton.setOnClickListener {
            viewModel.setComments(commentsView.text.toString())

            // TODO finish
        }
    }

    companion object {
        fun newInstance(): NewPlayCommentsFragment {
            return NewPlayCommentsFragment()
        }
    }
}