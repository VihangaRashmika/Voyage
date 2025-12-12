package com.example.voyage_v2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.voyage_v2.adapters.PackageAdapter
import com.example.voyage_v2.databinding.ActivityBrowsePackagesBinding
import com.example.voyage_v2.models.Package_Firebase
import com.example.voyage_v2.models.TravelPackage
import com.google.firebase.database.*

class BrowsePackagesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBrowsePackagesBinding
    private lateinit var adapter: PackageAdapter
    private val packageList = ArrayList<TravelPackage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBrowsePackagesBinding.inflate(layoutInflater)
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

        loadAllPackages()
    }

    private fun loadAllPackages() {
        val dbRef = FirebaseDatabase.getInstance().getReference("packages")
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                packageList.clear()

                val allChildren = snapshot.children.toList().reversed()
                for (child in allChildren) {
                    val pkg = child.getValue(Package_Firebase::class.java)
                    pkg?.let {
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

                adapter.notifyDataSetChanged()

                if (packageList.isEmpty()) {
                    Toast.makeText(this@BrowsePackagesActivity, "No packages available", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@BrowsePackagesActivity, "Failed to load", Toast.LENGTH_SHORT).show()
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