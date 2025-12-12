package com.example.voyage_v2.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.voyage_v2.BrowsePackagesActivity
import com.example.voyage_v2.TopRatedActivity
import com.example.voyage_v2.BudgetPackagesActivity
import com.example.voyage_v2.databinding.FragmentTravellerHomeBinding

class TravellerHomeFragment : Fragment() {

    private var _binding: FragmentTravellerHomeBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance() = TravellerHomeFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTravellerHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBrowseAll.setOnClickListener {
            startActivity(Intent(requireContext(), BrowsePackagesActivity::class.java))
        }

        binding.btnSearch.setOnClickListener {
            // Switch to Package tab which has search
            Toast.makeText(requireContext(), "Use the Package tab to search", Toast.LENGTH_SHORT).show()
        }

        binding.btnTopRated.setOnClickListener {
            startActivity(Intent(requireContext(), TopRatedActivity::class.java))
        }

        binding.btnBudget.setOnClickListener {
            startActivity(Intent(requireContext(), BudgetPackagesActivity::class.java))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}