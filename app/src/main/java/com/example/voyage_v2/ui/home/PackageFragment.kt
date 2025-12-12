package com.example.voyage_v2.ui.home

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.voyage_v2.HomeActivity
import com.example.voyage_v2.PackageDetailsActivity
import com.example.voyage_v2.adapters.PackageAdapter
import com.example.voyage_v2.databinding.FragmentPackageBinding
import com.example.voyage_v2.models.Package_Firebase
import com.example.voyage_v2.models.TravelPackage
import com.google.firebase.database.*

class PackageFragment : Fragment() {

    private var _binding: FragmentPackageBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PackageAdapter
    private val packageList = ArrayList<TravelPackage>()
    private val filteredList = ArrayList<TravelPackage>()

    private var selectedLocation: String? = null
    private var selectedPriceRange: Pair<Double, Double>? = null

    companion object {
        fun newInstance() = PackageFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPackageBinding.inflate(inflater, container, false)

        // Initialize adapter with click listener
        // Initialize adapter with click listener
        adapter = PackageAdapter(
            packages = filteredList,
            onPackageClick = { travelPackage ->
                openPackageDetails(travelPackage)
            },
            onEditClick = { _ -> },
            onDeleteClick = { _ -> },
            showActions = false
        )


        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        setupSearch()
        setupFilter()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        fetchPackagesFromFirebase()
    }

    private fun setupSearch() {
        // Toggle search bar visibility
        binding.btnSearchToggle.setOnClickListener {
            if (binding.searchLayout.visibility == View.VISIBLE) {
                binding.searchLayout.visibility = View.GONE
                binding.filterChipsLayout.visibility = View.GONE
            } else {
                binding.searchLayout.visibility = View.VISIBLE
            }
        }

        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterPackages()
            }
        })
    }

    private fun setupFilter() {
        binding.btnFilter.setOnClickListener {
            if (binding.filterChipsLayout.visibility == View.VISIBLE) {
                binding.filterChipsLayout.visibility = View.GONE
            } else {
                binding.filterChipsLayout.visibility = View.VISIBLE
            }
        }

        binding.chipLocation.setOnClickListener {
            showLocationFilter()
        }

        binding.chipPrice.setOnClickListener {
            showPriceFilter()
        }
    }

    private fun showLocationFilter() {
        val locations = packageList.map { it.location }.distinct().toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Filter by Location")
            .setItems(locations) { _, which ->
                selectedLocation = locations[which]
                binding.chipLocation.text = locations[which]
                binding.chipLocation.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2196F3"))
                binding.chipLocation.setTextColor(android.graphics.Color.BLACK)
                filterPackages()
            }
            .setNegativeButton("Clear") { _, _ ->
                selectedLocation = null
                binding.chipLocation.text = "Location"
                binding.chipLocation.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E0E0E0"))
                binding.chipLocation.setTextColor(android.graphics.Color.BLACK)
                filterPackages()
            }
            .show()
    }

    private fun showPriceFilter() {
        val priceRanges = arrayOf(
            "Under LKR 1000",
            "LKR 1000 - 2000",
            "LKR 2000 - 5000",
            "Above LKR 5000"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Filter by Price")
            .setItems(priceRanges) { _, which ->
                selectedPriceRange = when(which) {
                    0 -> Pair(0.0, 1000.0)
                    1 -> Pair(1000.0, 2000.0)
                    2 -> Pair(2000.0, 5000.0)
                    else -> Pair(5000.0, Double.MAX_VALUE)
                }
                binding.chipPrice.text = priceRanges[which]
                binding.chipPrice.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2196F3"))
                binding.chipPrice.setTextColor(android.graphics.Color.BLACK)
                filterPackages()
            }
            .setNegativeButton("Clear") { _, _ ->
                selectedPriceRange = null
                binding.chipPrice.text = "Price"
                binding.chipPrice.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E0E0E0"))
                binding.chipPrice.setTextColor(android.graphics.Color.BLACK)
                filterPackages()
            }
            .show()
    }

    private fun filterPackages() {
        val searchText = binding.searchBar.text.toString().lowercase()

        filteredList.clear()

        for (pkg in packageList) {
            var matches = true

            // Search filter
            if (searchText.isNotEmpty()) {
                matches = pkg.name.lowercase().contains(searchText) ||
                        pkg.description.lowercase().contains(searchText) ||
                        pkg.location.lowercase().contains(searchText)
            }

            // Location filter
            if (matches && selectedLocation != null) {
                matches = pkg.location == selectedLocation
            }

            // Price filter
            if (matches && selectedPriceRange != null) {
                matches = pkg.price >= selectedPriceRange!!.first &&
                        pkg.price <= selectedPriceRange!!.second
            }

            if (matches) {
                filteredList.add(pkg)
            }
        }

        adapter.notifyDataSetChanged()

        if (filteredList.isEmpty()) {
            binding.txtNoResults.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.txtNoResults.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }

    private fun fetchPackagesFromFirebase() {
        val dbRef = FirebaseDatabase.getInstance().getReference("packages")
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val firebasePackages = ArrayList<TravelPackage>()

                val allChildren = snapshot.children.toList().reversed()
                for (child in allChildren) {
                    val pkg = child.getValue(Package_Firebase::class.java)
                    pkg?.let {
                        firebasePackages.add(
                            TravelPackage(
                                name = it.name,
                                price = it.price,
                                description = it.description,
                                image = it.image,
                                author = it.author,
                                phone = it.phone,
                                facilities = it.facilities,
                                location = it.location
                            )
                        )
                    }
                }

                val activity = requireActivity() as HomeActivity
                val hardcodedPackages = activity.hardcodedPackages()

                packageList.clear()
                packageList.addAll(firebasePackages)
                packageList.addAll(hardcodedPackages)

                filterPackages()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load packages", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun openPackageDetails(travelPackage: TravelPackage) {
        val intent = Intent(requireContext(), PackageDetailsActivity::class.java)
        intent.putExtra("packageName", travelPackage.name)
        intent.putExtra("price", travelPackage.price)
        intent.putExtra("description", travelPackage.description)
        intent.putExtra("image", travelPackage.image)
        intent.putExtra("author", travelPackage.author)
        intent.putExtra("phone", travelPackage.phone)
        intent.putExtra("facilities", travelPackage.facilities)
        intent.putExtra("location", travelPackage.location)

        val homeActivity = activity as? HomeActivity
        intent.putExtra("userType", homeActivity?.getUserType())
        intent.putExtra("userName", homeActivity?.getUserName())

        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun updatePackageList(newList: List<TravelPackage>) {
        packageList.clear()
        packageList.addAll(newList)
        filterPackages()
    }
}