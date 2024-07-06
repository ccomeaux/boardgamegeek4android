package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentNewPlaySavingBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NewPlaySavingFragment : Fragment() {
    private var _binding: FragmentNewPlaySavingBinding? = null
    private val binding get() = _binding!!

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentNewPlaySavingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.setSubtitle(R.string.title_saving)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
