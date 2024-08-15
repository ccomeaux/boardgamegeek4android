package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.boardgamegeek.databinding.FragmentCollectionCreditsBinding
import com.boardgamegeek.extensions.startActivity

class CollectionCreditsFragment : Fragment() {
    private var _binding: FragmentCollectionCreditsBinding? = null
    private val binding get() = _binding!!

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentCollectionCreditsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.designersButton.setOnClickListener {
            startActivity<DesignersActivity>()
        }

        binding.artistsButton.setOnClickListener {
            startActivity<ArtistsActivity>()
        }

        binding.publishersButton.setOnClickListener {
            startActivity<PublishersActivity>()
        }

        binding.mechanicsButton.setOnClickListener {
            startActivity<MechanicsActivity>()
        }

        binding.categoriesButton.setOnClickListener {
            startActivity<CategoriesActivity>()
        }
    }
}
