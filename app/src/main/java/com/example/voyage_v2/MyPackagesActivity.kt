package com.example.voyage_v2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.voyage_v2.adapters.PackageAdapter
import com.example.voyage_v2.models.Package_Firebase
import com.example.voyage_v2.models.TravelPackage
import com.google.firebase.database.*

class MyPackagesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var packageAdapter: PackageAdapter
    private val myPackagesList = ArrayList<TravelPackage>()
    private lateinit var userName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_packages)

        userName = intent.getStringExtra("userName") ?: ""

        // 🔥 ADD NEW PACKAGE BUTTON
        findViewById<Button>(R.id.btnAddPackage).setOnClickListener {
            val intent = Intent(this, PackageCreateActivity::class.java)
            intent.putExtra("userName", userName)
            startActivity(intent)
        }

        recyclerView = findViewById(R.id.rvMyPackages)
        recyclerView.layoutManager = LinearLayoutManager(this)

        packageAdapter = PackageAdapter(
            packages = myPackagesList,
            onPackageClick = { openPackageDetails(it) },
            onEditClick = { editPackage(it) },
            onDeleteClick = { deletePackage(it) },
            showActions = true
        )
        recyclerView.adapter = packageAdapter

        loadMyPackages()
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
        intent.putExtra("userType", "BusinessOwner")
        intent.putExtra("userName", userName)
        startActivity(intent)
    }

    private fun loadMyPackages() {
        val dbRef = FirebaseDatabase.getInstance().getReference("packages")
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                myPackagesList.clear()

                val allChildren = snapshot.children.toList().reversed()
                for (child in allChildren) {
                    val pkg = child.getValue(Package_Firebase::class.java)
                    if (pkg != null && pkg.author == userName) {
                        myPackagesList.add(
                            TravelPackage(
                                name = pkg.name,
                                price = pkg.price,
                                description = pkg.description,
                                image = pkg.image,
                                author = pkg.author,
                                phone = pkg.phone,
                                facilities = pkg.facilities,
                                location = pkg.location
                            )
                        )
                    }
                }
                packageAdapter.notifyDataSetChanged()

                if (myPackagesList.isEmpty()) {
                    Toast.makeText(this@MyPackagesActivity, "You haven't created any packages yet", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MyPackagesActivity, "Failed to load packages", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showPackageOptions(travelPackage: TravelPackage) {
        val options = arrayOf("View Details", "Edit", "Delete")
        AlertDialog.Builder(this)
            .setTitle(travelPackage.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewPackageDetails(travelPackage)
                    1 -> editPackage(travelPackage)
                    2 -> deletePackage(travelPackage)
                }
            }
            .show()
    }

    private fun viewPackageDetails(travelPackage: TravelPackage) {
        val intent = Intent(this, PackageDetailsActivity::class.java)
        intent.putExtra("packageName", travelPackage.name)
        intent.putExtra("price", travelPackage.price)
        intent.putExtra("description", travelPackage.description)
        intent.putExtra("image", travelPackage.image)
        intent.putExtra("author", travelPackage.author)
        intent.putExtra("phone", travelPackage.phone)
        intent.putExtra("facilities", travelPackage.facilities)
        intent.putExtra("location", travelPackage.location)
        intent.putExtra("userType", "BusinessOwner")
        intent.putExtra("userName", userName)
        startActivity(intent)
    }

    private fun editPackage(travelPackage: TravelPackage) {
        val intent = Intent(this, EditPackageActivity::class.java)
        intent.putExtra("packageName", travelPackage.name)
        intent.putExtra("author", travelPackage.author)
        startActivity(intent)
    }

    private fun deletePackage(travelPackage: TravelPackage) {
        AlertDialog.Builder(this)
            .setTitle("Delete Package")
            .setMessage("Are you sure you want to delete '${travelPackage.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                val dbRef = FirebaseDatabase.getInstance().getReference("packages")
                dbRef.orderByChild("name").equalTo(travelPackage.name)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (child in snapshot.children) {
                                child.ref.removeValue()
                                    .addOnSuccessListener {
                                        Toast.makeText(this@MyPackagesActivity, "Package deleted!", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this@MyPackagesActivity, "Failed to delete", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
