package com.example.voyage_v2

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.voyage_v2.databinding.ActivityPackageCreateBinding
import com.example.voyage_v2.models.Package_Firebase
import com.google.firebase.database.FirebaseDatabase
import org.json.JSONObject

class PackageCreateActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPackageCreateBinding

    // Store selected images (max 3)
    private val selectedImageUris = mutableListOf<Uri>()

    // Multi-image picker launcher
    private val multiImagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUris.clear()

            val clipData = result.data?.clipData
            if (clipData != null) {
                // Multiple images selected
                val count = minOf(clipData.itemCount, 3) // Max 3 images
                for (i in 0 until count) {
                    val uri = clipData.getItemAt(i).uri
                    selectedImageUris.add(uri)
                }
            } else {
                // Single image selected
                result.data?.data?.let { uri ->
                    selectedImageUris.add(uri)
                }
            }

            if (selectedImageUris.isNotEmpty()) {
                displayImagePreviews()
                Toast.makeText(this, "${selectedImageUris.size} image(s) selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPackageCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Select images button
        binding.btnSelectImages.setOnClickListener {
            openMultiImagePicker()
        }

        // Create package button
        binding.btnCreatePackage.setOnClickListener {
            savePackageToFirebase()
        }
    }

    private fun openMultiImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true) // Enable multi-select
        intent.type = "image/*"
        multiImagePickerLauncher.launch(intent)
    }

    private fun displayImagePreviews() {
        // Show preview container
        binding.imagePreviewContainer.visibility = View.VISIBLE

        // Hide all previews first
        binding.imgPreview1.visibility = View.GONE
        binding.imgPreview2.visibility = View.GONE
        binding.imgPreview3.visibility = View.GONE
        binding.txtCoverBadge.visibility = View.GONE

        // Show selected images
        selectedImageUris.forEachIndexed { index, uri ->
            when (index) {
                0 -> {
                    binding.imgPreview1.setImageURI(uri)
                    binding.imgPreview1.visibility = View.VISIBLE
                    binding.txtCoverBadge.visibility = View.VISIBLE // First image is cover
                }
                1 -> {
                    binding.imgPreview2.setImageURI(uri)
                    binding.imgPreview2.visibility = View.VISIBLE
                }
                2 -> {
                    binding.imgPreview3.setImageURI(uri)
                    binding.imgPreview3.visibility = View.VISIBLE
                }
            }
        }

        // Update count text
        val countText = when (selectedImageUris.size) {
            1 -> "1 image selected (Cover photo)"
            else -> "${selectedImageUris.size} images selected (First is cover)"
        }
        binding.txtImageCount.text = countText
    }

    private fun savePackageToFirebase() {
        val name = binding.inputName.text.toString().trim()
        val priceText = binding.inputPrice.text.toString().trim()
        val location = binding.inputLocation.text.toString().trim()
        val phone = binding.inputPhone.text.toString().trim()
        val facilities = binding.inputFacilities.text.toString().trim()
        val description = binding.inputDescription.text.toString().trim()

        // Validation
        if (name.isEmpty() || priceText.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Please fill required fields (Name, Price, Description)", Toast.LENGTH_SHORT).show()
            return
        }

        if (location.isEmpty()) {
            Toast.makeText(this, "Please enter location", Toast.LENGTH_SHORT).show()
            return
        }

        if (phone.isEmpty()) {
            Toast.makeText(this, "Please enter contact phone number", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedImageUris.isEmpty()) {
            Toast.makeText(this, "Please select at least one image", Toast.LENGTH_SHORT).show()
            return
        }

        val price = priceText.toDoubleOrNull()
        if (price == null || price <= 0) {
            Toast.makeText(this, "Enter a valid price", Toast.LENGTH_SHORT).show()
            return
        }

        // Get username
        val userName = intent.getStringExtra("userName") ?: "Business Owner"

        // Show loading
        binding.btnCreatePackage.isEnabled = false
        binding.btnCreatePackage.text = "Uploading ${selectedImageUris.size} image(s)..."
        binding.btnSelectImages.isEnabled = false

        // Upload all images
        uploadAllImages { imageUrls ->
            if (imageUrls.isNotEmpty()) {
                savePackageToDatabase(name, price, location, phone, facilities, description, imageUrls, userName)
            } else {
                binding.btnCreatePackage.isEnabled = true
                binding.btnCreatePackage.text = "Create Package"
                binding.btnSelectImages.isEnabled = true
                Toast.makeText(this, "Failed to upload images", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadAllImages(callback: (List<String>) -> Unit) {
        val uploadedUrls = mutableListOf<String>()
        var uploadedCount = 0
        val totalImages = selectedImageUris.size

        selectedImageUris.forEachIndexed { index, uri ->
            uploadImageToCloudinary(uri) { url ->
                if (url != null) {
                    uploadedUrls.add(url)
                }

                uploadedCount++

                // Update progress
                runOnUiThread {
                    binding.btnCreatePackage.text = "Uploading... ($uploadedCount/$totalImages)"
                }

                // All images uploaded
                if (uploadedCount == totalImages) {
                    callback(uploadedUrls)
                }
            }
        }
    }

    private fun uploadImageToCloudinary(imageUri: Uri, callback: (String?) -> Unit) {
        Thread {
            try {
                val cloudName = "dtozv7and"
                val uploadPreset = "voyage_packages"

                val url = java.net.URL("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
                val boundary = "----WebKitFormBoundary" + System.currentTimeMillis()

                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

                val outputStream = connection.outputStream
                val writer = java.io.PrintWriter(java.io.OutputStreamWriter(outputStream, "UTF-8"), true)

                // Add upload preset
                writer.append("--$boundary\r\n")
                writer.append("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n")
                writer.append("$uploadPreset\r\n")
                writer.flush()

                // Add file
                writer.append("--$boundary\r\n")
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"image.jpg\"\r\n")
                writer.append("Content-Type: image/jpeg\r\n\r\n")
                writer.flush()

                val inputStream = contentResolver.openInputStream(imageUri)
                inputStream?.copyTo(outputStream)
                outputStream.flush()
                inputStream?.close()

                writer.append("\r\n")
                writer.append("--$boundary--\r\n")
                writer.close()

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)
                    val imageUrl = json.getString("secure_url")

                    runOnUiThread {
                        callback(imageUrl)
                    }
                } else {
                    runOnUiThread {
                        callback(null)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    callback(null)
                }
            }
        }.start()
    }

    private fun savePackageToDatabase(
        name: String,
        price: Double,
        location: String,
        phone: String,
        facilities: String,
        description: String,
        imageUrls: List<String>,
        userName: String
    ) {
        // Create comma-separated string of image URLs
        val imagesString = imageUrls.joinToString(",")

        val newPackage = Package_Firebase(
            name = name,
            price = price,
            description = description,
            image = imagesString,
            author = userName,
            phone = phone,
            facilities = facilities,
            location = location
        )

        val dbRef = FirebaseDatabase.getInstance().getReference("packages")
        val packageId = dbRef.push().key!!

        dbRef.child(packageId).setValue(newPackage)
            .addOnSuccessListener {
                Toast.makeText(this, "Package created with ${imageUrls.size} images! 🎉", Toast.LENGTH_LONG).show()

                // Clear all fields
                binding.inputName.text.clear()
                binding.inputPrice.text.clear()
                binding.inputLocation.text.clear()
                binding.inputPhone.text.clear()
                binding.inputFacilities.text.clear()
                binding.inputDescription.text.clear()

                selectedImageUris.clear()
                binding.imagePreviewContainer.visibility = View.GONE

                binding.btnCreatePackage.isEnabled = true
                binding.btnCreatePackage.text = "Create Package"
                binding.btnSelectImages.isEnabled = true

                // Go back
                finish()
            }
            .addOnFailureListener { e ->
                binding.btnCreatePackage.isEnabled = true
                binding.btnCreatePackage.text = "Create Package"
                binding.btnSelectImages.isEnabled = true
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}