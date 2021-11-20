package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.extensions.startActivity
import kotlinx.android.synthetic.main.fragment_collection_details.*

class CollectionDetailsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_collection_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        designersButton.setOnClickListener {
            startActivity<DesignersActivity>()
        }

        artistsButton.setOnClickListener {
            startActivity<ArtistsActivity>()
        }

        publishersButton.setOnClickListener {
            startActivity<PublishersActivity>()
        }

        mechanicsButton.setOnClickListener {
            startActivity<MechanicsActivity>()
        }

        categoriesButton.setOnClickListener {
            startActivity<CategoriesActivity>()
        }
    }
}
