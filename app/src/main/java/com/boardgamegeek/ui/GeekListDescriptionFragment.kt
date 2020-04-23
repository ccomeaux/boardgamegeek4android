package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.boardgamegeek.R
import com.boardgamegeek.io.model.GeekListResponse
import com.boardgamegeek.ui.viewmodel.GeekListViewModel
import com.boardgamegeek.util.DateTimeUtils
import com.boardgamegeek.util.StringUtils
import com.boardgamegeek.util.UIUtils
import com.boardgamegeek.util.XmlConverter
import kotlinx.android.synthetic.main.fragment_geeklist_description.*
import kotlinx.android.synthetic.main.header_geek_list.*

class GeekListDescriptionFragment : Fragment() {
    private val xmlConverter = XmlConverter()
    private val viewModel by activityViewModels<GeekListViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_geeklist_description, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.geekList.observe(viewLifecycleOwner, Observer { response ->
            response?.data?.let {
                usernameView.text = it.username
                itemCountView.text = StringUtils.parseInt(it.numitems).toString()
                thumbCountView.text = StringUtils.parseInt(it.thumbs).toString()
                UIUtils.setWebViewText(bodyView, xmlConverter.toHtml(it.description))
                postedDateView.timestamp = DateTimeUtils.tryParseDate(DateTimeUtils.UNPARSED_DATE, it.postdate, GeekListResponse.FORMAT)
                editedDateView.timestamp = DateTimeUtils.tryParseDate(DateTimeUtils.UNPARSED_DATE, it.editdate, GeekListResponse.FORMAT)
                container.visibility = View.VISIBLE
                progressBar.hide()
            }
        })
    }

    companion object {
        @JvmStatic
        fun newInstance(): GeekListDescriptionFragment {
            return GeekListDescriptionFragment()
        }
    }
}
