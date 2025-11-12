package com.example.voyage_v2

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.voyage_v2.databinding.ActivityPackageCreateBinding
import com.example.voyage_v2.models.Package_Firebase
import com.google.firebase.database.FirebaseDatabase

class PackageCreateActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPackageCreateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPackageCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCreatePackage.setOnClickListener {
            savePackageToFirebase()
        }
    }

    private fun savePackageToFirebase() {
        val name = binding.inputName.text.toString().trim()
        val priceText = binding.inputPrice.text.toString().trim()
        val description = binding.inputDescription.text.toString().trim()

        if (name.isEmpty() || priceText.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val price = priceText.toDoubleOrNull()
        if (price == null || price <= 0) {
            Toast.makeText(this, "Enter a valid price", Toast.LENGTH_SHORT).show()
            return
        }

        val imageUrl =
            "https://res.cloudinary.com/dtozv7and/image/upload/v1762953677/skeleton_pxntsi.png"
        val author = "Hotel Owner"

        val newPackage = Package_Firebase(name, price, description, imageUrl, author)

        val dbRef = FirebaseDatabase.getInstance().getReference("packages")
        val packageId = dbRef.push().key!!

        dbRef.child(packageId).setValue(newPackage)
            .addOnSuccessListener {
                Toast.makeText(this, "Package created successfully!", Toast.LENGTH_SHORT).show()

                binding.inputName.text.clear()
                binding.inputPrice.text.clear()
                binding.inputDescription.text.clear()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }


}