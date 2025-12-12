package com.example.voyage_v2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.voyage_v2.adapters.PackageAdapter
import com.example.voyage_v2.databinding.ActivityTopRatedBinding
import com.example.voyage_v2.models.Package_Firebase
import com.example.voyage_v2.models.TravelPackage
import com.google.firebase.database.*

class TopRatedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTopRatedBinding
    private lateinit var adapter: PackageAdapter
    private val packageList = ArrayList<TravelPackage>()
    private val packageRatings = HashMap<String, Float>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTopRatedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
        }

        adapter = PackageAdapter(
            packages = packageList,
            onPackageClick = { travelPackage -> openPackageDetails(travelPackage) }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        loadTopRatedPackages()
    }

    private fun loadTopRatedPackages() {
        // First, load all ratings
        val ratingsRef = FirebaseDatabase.getInstance().getReference("ratings")
        ratingsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ratingsByPackage = HashMap<String, MutableList<Float>>()

                for (child in snapshot.children) {
                    val packageName = child.child("packageName").getValue(String::class.java) ?: continue
                    val rating = child.child("rating").getValue(Float::class.java) ?: continue

                    if (!ratingsByPackage.containsKey(packageName)) {
                        ratingsByPackage[packageName] = mutableListOf()
                    }
                    ratingsByPackage[packageName]?.add(rating)
                }

                // Calculate averages
                for ((packageName, ratings) in ratingsByPackage) {
                    val average = ratings.average().toFloat()
                    packageRatings[packageName] = average
                }

                // Now load packages
                loadPackagesAndSort()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TopRatedActivity, "Failed to load ratings", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadPackagesAndSort() {
        val dbRef = FirebaseDatabase.getInstance().getReference("packages")
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                packageList.clear()

                for (child in snapshot.children) {
                    val pkg = child.getValue(Package_Firebase::class.java)
                    pkg?.let {
                        // Only add packages with ratings of 3.5 or higher
                        val rating = packageRatings[it.name] ?: 0f
                        if (rating >= 3.5f) {
                            packageList.add(
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
                }

                // Sort by rating (highest first)
                packageList.sortByDescending { packageRatings[it.name] ?: 0f }

                adapter.notifyDataSetChanged()

                if (packageList.isEmpty()) {
                    Toast.makeText(this@TopRatedActivity, "No top-rated packages yet", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TopRatedActivity, "Failed to load", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun openPackageDetails(travelPackage: TravelPackage) {
        val intent = Intent(this, PackageDetailsActivity::class.java)
        intent.putExtra("packageName", travelPackage.name)
        intent.putExtra("price", travelPackage.price)
        intent.putExtra("description", travelPackage.description)
        intent.putExtra("image", travelPackage.image)
        intent.putExtra("author", travelPackage.author)
        intent.putExtra("phone", travelPackage.phone)
        intent.putExtra("facilities", travelPackage.facilities)
        intent.putExtra("location", travelPackage.location)
        intent.putExtra("userType", "Traveller")
        startActivity(intent)
    }
}