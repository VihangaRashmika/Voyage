package com.example.voyage_v2.ui.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.voyage_v2.HomeActivity
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

    companion object {
        fun newInstance() = PackageFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPackageBinding.inflate(inflater, container, false)

        // Initialize adapter with the class-level packageList
        adapter = PackageAdapter(packageList)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        fetchPackagesFromFirebase()
    }

    private fun fetchPackagesFromFirebase() {
        val dbRef = FirebaseDatabase.getInstance().getReference("packages")
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val firebasePackages = ArrayList<TravelPackage>()

                for (child in snapshot.children) {
                    val pkg = child.getValue(Package_Firebase::class.java)
                    pkg?.let {
                        firebasePackages.add(
                            TravelPackage(
                                name = it.name,
                                price = it.price,
                                description = it.description,
                                image = it.image,
                                author = it.author
                            )
                        )
                    }
                }

                // Get hardcoded packages from HomeActivity
                val activity = requireActivity() as HomeActivity
                val hardcodedPackages = activity.hardcodedPackages()

                // Merge hardcoded + Firebase packages
                packageList.clear()
                packageList.addAll(hardcodedPackages)
                packageList.addAll(firebasePackages)

                // Notify adapter
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Optional: handle error
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun updatePackageList(newList: List<TravelPackage>) {
        packageList.clear()
        packageList.addAll(newList)
        adapter.notifyDataSetChanged()
    }
}
