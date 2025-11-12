package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentCollectionDetailsBinding
import org.jetbrains.anko.support.v4.startActivity

class CollectionDetailsFragment : Fragment() {
    private var _binding: FragmentCollectionDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCollectionDetailsBinding.inflate(inflater, container, false)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}