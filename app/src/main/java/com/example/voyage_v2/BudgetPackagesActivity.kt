package com.example.voyage_v2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.voyage_v2.adapters.PackageAdapter
import com.example.voyage_v2.databinding.ActivityBudgetPackagesBinding
import com.example.voyage_v2.models.Package_Firebase
import com.example.voyage_v2.models.TravelPackage
import com.google.firebase.database.*

class BudgetPackagesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBudgetPackagesBinding
    private lateinit var adapter: PackageAdapter
    private val packageList = ArrayList<TravelPackage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetPackagesBinding.inflate(layoutInflater)
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

        loadBudgetPackages()
    }

    private fun loadBudgetPackages() {
        val dbRef = FirebaseDatabase.getInstance().getReference("packages")
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                packageList.clear()

                for (child in snapshot.children) {
                    val pkg = child.getValue(Package_Firebase::class.java)
                    pkg?.let {
                        // Only show packages under LKR 1500
                        if (it.price <= 1500.0) {
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

                // Sort by price (lowest first)
                packageList.sortBy { it.price }

                adapter.notifyDataSetChanged()

                if (packageList.isEmpty()) {
                    Toast.makeText(this@BudgetPackagesActivity, "No budget packages available", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@BudgetPackagesActivity, "Failed to load", Toast.LENGTH_SHORT).show()
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