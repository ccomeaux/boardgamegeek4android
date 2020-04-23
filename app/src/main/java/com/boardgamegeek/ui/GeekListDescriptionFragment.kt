package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.ui.model.GeekList
import com.boardgamegeek.util.UIUtils
import com.boardgamegeek.util.XmlConverter
import kotlinx.android.synthetic.main.fragment_geeklist_description.*
import kotlinx.android.synthetic.main.header_geek_list.*

class GeekListDescriptionFragment : Fragment() {
    private val xmlConverter = XmlConverter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_geeklist_description, container, false)
    }

    fun setData(geekList: GeekList?) {
        geekList?.let {
            usernameView.text = it.username
            itemCountView.text = it.numberOfItems.toString()
            thumbCountView.text = it.numberOfThumbs.toString()
            UIUtils.setWebViewText(bodyView, xmlConverter.toHtml(it.description))
            postedDateView.timestamp = it.postTicks
            editedDateView.timestamp = it.EditTicks
            container.visibility = View.VISIBLE
            progressBar.hide()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(): GeekListDescriptionFragment {
            return GeekListDescriptionFragment()
        }
    }
}
