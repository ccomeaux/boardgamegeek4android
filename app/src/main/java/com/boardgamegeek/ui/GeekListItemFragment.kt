package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.util.UIUtils
import com.boardgamegeek.util.XmlConverter
import kotlinx.android.synthetic.main.fragment_geeklist_item.*
import org.jetbrains.anko.support.v4.withArguments

class GeekListItemFragment : Fragment() {
    private var order = 0
    private var geekListTitle = ""
    private var type = ""
    private var username = ""
    private var numberOfThumbs = 0
    private var postedDate: Long = 0
    private var editedDate: Long = 0
    private var body = ""
    private var xmlConverter = XmlConverter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            order = it.getInt(KEY_ORDER, 0)
            geekListTitle = it.getString(KEY_TITLE).orEmpty()
            type = it.getString(KEY_TYPE).orEmpty()
            username = it.getString(KEY_USERNAME).orEmpty()
            numberOfThumbs = it.getInt(KEY_THUMBS, 0)
            postedDate = it.getLong(KEY_POSTED_DATE, 0)
            editedDate = it.getLong(KEY_EDITED_DATE, 0)
            body = it.getString(KEY_BODY).orEmpty()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_geeklist_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        orderView.text = order.toString()
        geekListTitleView.text = geekListTitle
        typeView.text = type
        usernameView.text = username
        thumbsView.text = numberOfThumbs.toString()
        UIUtils.setWebViewText(bodyView, xmlConverter.toHtml(body))
        postedDateView.timestamp = postedDate
        editedDateView.timestamp = editedDate
        datetimeDividerView.isVisible = editedDate != postedDate
        editedDateView.isVisible = editedDate != postedDate
    }

    companion object {
        private const val KEY_ORDER = "GEEK_LIST_ORDER"
        private const val KEY_TITLE = "GEEK_LIST_TITLE"
        private const val KEY_TYPE = "GEEK_LIST_TYPE"
        private const val KEY_USERNAME = "GEEK_LIST_USERNAME"
        private const val KEY_THUMBS = "GEEK_LIST_THUMBS"
        private const val KEY_POSTED_DATE = "GEEK_LIST_POSTED_DATE"
        private const val KEY_EDITED_DATE = "GEEK_LIST_EDITED_DATE"
        private const val KEY_BODY = "GEEK_LIST_BODY"

        fun newInstance(order: Int, title: String, type: String, username: String, numberOfThumbs: Int, postedDate: Long, editedDate: Long, body: String): GeekListItemFragment {
            return GeekListItemFragment().withArguments(
                    KEY_ORDER to order,
                    KEY_TITLE to title,
                    KEY_TYPE to type,
                    KEY_USERNAME to username,
                    KEY_THUMBS to numberOfThumbs,
                    KEY_POSTED_DATE to postedDate,
                    KEY_EDITED_DATE to editedDate,
                    KEY_BODY to body
            )
        }
    }
}