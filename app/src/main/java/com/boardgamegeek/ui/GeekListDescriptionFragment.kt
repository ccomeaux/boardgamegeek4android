package com.boardgamegeek.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.boardgamegeek.R
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.setWebViewText
import com.boardgamegeek.ui.viewmodel.GeekListViewModel
import com.boardgamegeek.util.XmlApiMarkupConverter
import kotlinx.android.synthetic.main.fragment_geeklist_description.*
import kotlinx.android.synthetic.main.header_geek_list.*

class GeekListDescriptionFragment : Fragment(R.layout.fragment_geeklist_description) {
    private val viewModel by activityViewModels<GeekListViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val markupConverter = XmlApiMarkupConverter(requireContext())
        viewModel.geekList.observe(viewLifecycleOwner, Observer { (status, data, _) ->
            if (status == Status.SUCCESS) {
                data?.let {
                    usernameView.text = it.username
                    itemCountView.text = it.numberOfItems.toString()
                    thumbCountView.text = it.numberOfThumbs.toString()
                    bodyView.setWebViewText(markupConverter.toHtml(it.description))
                    postedDateView.timestamp = it.postTicks
                    editedDateView.timestamp = it.editTicks
                    container.visibility = View.VISIBLE
                    progressBar.hide()
                }
            }
        })
    }
}
