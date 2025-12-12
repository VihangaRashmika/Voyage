package com.example.voyage_v2

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.voyage_v2.databinding.ActivityEditPackageBinding
import com.example.voyage_v2.models.Package_Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.json.JSONObject

class EditPackageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditPackageBinding
    private var packageId: String = ""

    // Current image URLs from database
    private var currentImageUrl1: String = ""
    private var currentImageUrl2: String = ""
    private var currentImageUrl3: String = ""

    // New selected images
    private var selectedImageUri1: Uri? = null
    private var selectedImageUri2: Uri? = null
    private var selectedImageUri3: Uri? = null

    // Track which image slot is being edited
    private var currentImageSlot = 1

    // Flags to track if image should be removed
    private var removeImage1 = false
    private var removeImage2 = false
    private var removeImage3 = false

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                when (currentImageSlot) {
                    1 -> {
                        selectedImageUri1 = uri
                        binding.imgPreview1.setImageURI(uri)
                        removeImage1 = false
                    }
                    2 -> {
                        selectedImageUri2 = uri
                        binding.imgPreview2.setImageURI(uri)
                        binding.btnRemoveImage2.visibility = View.VISIBLE
                        removeImage2 = false
                    }
                    3 -> {
                        selectedImageUri3 = uri
                        binding.imgPreview3.setImageURI(uri)
                        binding.btnRemoveImage3.visibility = View.VISIBLE
                        removeImage3 = false
                    }
                }
                Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditPackageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val packageName = intent.getStringExtra("packageName") ?: ""
        loadPackageData(packageName)

        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Image change buttons
        binding.btnChangeImage1.setOnClickListener {
            currentImageSlot = 1
            openGallery()
        }

        binding.btnChangeImage2.setOnClickListener {
            currentImageSlot = 2
            openGallery()
        }

        binding.btnChangeImage3.setOnClickListener {
            currentImageSlot = 3
            openGallery()
        }

        // Image remove buttons
        binding.btnRemoveImage1.setOnClickListener {
            // Cover image cannot be removed, only changed
            Toast.makeText(this, "Cover image is required. Please change it instead.", Toast.LENGTH_SHORT).show()
        }

        binding.btnRemoveImage2.setOnClickListener {
            removeImage2 = true
            selectedImageUri2 = null
            binding.imgPreview2.setImageResource(android.R.color.darker_gray)
            binding.btnRemoveImage2.visibility = View.GONE
            Toast.makeText(this, "Image 2 will be removed", Toast.LENGTH_SHORT).show()
        }

        binding.btnRemoveImage3.setOnClickListener {
            removeImage3 = true
            selectedImageUri3 = null
            binding.imgPreview3.setImageResource(android.R.color.darker_gray)
            binding.btnRemoveImage3.visibility = View.GONE
            Toast.makeText(this, "Image 3 will be removed", Toast.LENGTH_SHORT).show()
        }

        // Update button
        binding.btnUpdatePackage.setOnClickListener {
            updatePackage()
        }
    }

    private fun loadPackageData(packageName: String) {
        val dbRef = FirebaseDatabase.getInstance().getReference("packages")
        dbRef.orderByChild("name").equalTo(packageName)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        packageId = child.key ?: ""
                        val pkg = child.getValue(Package_Firebase::class.java)

                        pkg?.let {
                            binding.inputName.setText(it.name)
                            binding.inputPrice.setText(it.price.toString())
                            binding.inputLocation.setText(it.location)
                            binding.inputPhone.setText(it.phone)
                            binding.inputFacilities.setText(it.facilities)
                            binding.inputDescription.setText(it.description)

                            // Parse comma-separated image URLs
                            val imageUrls = it.image.split(",").map { url -> url.trim() }.filter { url -> url.isNotEmpty() }

                            // Load images
                            if (imageUrls.isNotEmpty()) {
                                currentImageUrl1 = imageUrls[0]
                                Glide.with(this@EditPackageActivity)
                                    .load(currentImageUrl1)
                                    .placeholder(android.R.color.darker_gray)
                                    .into(binding.imgPreview1)
                            }

                            if (imageUrls.size > 1) {
                                currentImageUrl2 = imageUrls[1]
                                Glide.with(this@EditPackageActivity)
                                    .load(currentImageUrl2)
                                    .placeholder(android.R.color.darker_gray)
                                    .into(binding.imgPreview2)
                                binding.btnRemoveImage2.visibility = View.VISIBLE
                            }

                            if (imageUrls.size > 2) {
                                currentImageUrl3 = imageUrls[2]
                                Glide.with(this@EditPackageActivity)
                                    .load(currentImageUrl3)
                                    .placeholder(android.R.color.darker_gray)
                                    .into(binding.imgPreview3)
                                binding.btnRemoveImage3.visibility = View.VISIBLE
                            }
                        }
                        break
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@EditPackageActivity, "Failed to load data", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun updatePackage() {
        val name = binding.inputName.text.toString().trim()
        val priceText = binding.inputPrice.text.toString().trim()
        val location = binding.inputLocation.text.toString().trim()
        val phone = binding.inputPhone.text.toString().trim()
        val facilities = binding.inputFacilities.text.toString().trim()
        val description = binding.inputDescription.text.toString().trim()

        // Validation
        if (name.isEmpty() || priceText.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val price = priceText.toDoubleOrNull()
        if (price == null || price <= 0) {
            Toast.makeText(this, "Enter a valid price", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading
        binding.btnUpdatePackage.isEnabled = false
        binding.btnUpdatePackage.text = "Updating..."

        // Upload new images and collect all URLs
        uploadImagesAndUpdate(name, price, location, phone, facilities, description)
    }

    private fun uploadImagesAndUpdate(
        name: String,
        price: Double,
        location: String,
        phone: String,
        facilities: String,
        description: String
    ) {
        val finalUrls = mutableListOf<String>()
        var uploadCount = 0
        val totalUploads = listOfNotNull(selectedImageUri1, selectedImageUri2, selectedImageUri3).size

        // Function to check if all uploads are done
        fun checkAndSave() {
            uploadCount++
            if (uploadCount >= totalUploads || totalUploads == 0) {
                // Build final image URLs list
                if (!removeImage1) {
                    finalUrls.add(0, if (selectedImageUri1 != null) "" else currentImageUrl1)
                }

                if (!removeImage2 && currentImageUrl2.isNotEmpty() && selectedImageUri2 == null) {
                    if (finalUrls.size < 2) finalUrls.add(currentImageUrl2)
                }

                if (!removeImage3 && currentImageUrl3.isNotEmpty() && selectedImageUri3 == null) {
                    if (finalUrls.size < 3) finalUrls.add(currentImageUrl3)
                }

                // Filter empty strings and save
                val cleanUrls = finalUrls.filter { it.isNotEmpty() }
                if (cleanUrls.isEmpty()) {
                    binding.btnUpdatePackage.isEnabled = true
                    binding.btnUpdatePackage.text = "Update Package"
                    Toast.makeText(this, "At least one image is required", Toast.LENGTH_SHORT).show()
                    return
                }

                saveUpdatedPackage(name, price, location, phone, facilities, description, cleanUrls)
            }
        }

        // Upload new images
        if (selectedImageUri1 != null) {
            uploadImageToCloudinary(selectedImageUri1!!) { url ->
                if (url != null) {
                    finalUrls.add(0, url)
                }
                checkAndSave()
            }
        }

        if (selectedImageUri2 != null) {
            uploadImageToCloudinary(selectedImageUri2!!) { url ->
                if (url != null) {
                    // Insert at position 1 if exists
                    if (finalUrls.size > 1) finalUrls.add(1, url) else finalUrls.add(url)
                }
                checkAndSave()
            }
        }

        if (selectedImageUri3 != null) {
            uploadImageToCloudinary(selectedImageUri3!!) { url ->
                if (url != null) {
                    finalUrls.add(url)
                }
                checkAndSave()
            }
        }

        // If no new images selected, just save with existing
        if (totalUploads == 0) {
            checkAndSave()
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

                writer.append("--$boundary\r\n")
                writer.append("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n")
                writer.append("$uploadPreset\r\n")
                writer.flush()

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

    private fun saveUpdatedPackage(
        name: String,
        price: Double,
        location: String,
        phone: String,
        facilities: String,
        description: String,
        imageUrls: List<String>
    ) {
        val author = intent.getStringExtra("author") ?: "Business Owner"
        val imagesString = imageUrls.joinToString(",")

        val updatedPackage = Package_Firebase(
            name = name,
            price = price,
            description = description,
            image = imagesString,
            author = author,
            phone = phone,
            facilities = facilities,
            location = location
        )

        val dbRef = FirebaseDatabase.getInstance().getReference("packages").child(packageId)
        dbRef.setValue(updatedPackage)
            .addOnSuccessListener {
                Toast.makeText(this, "Package updated with ${imageUrls.size} images!", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                binding.btnUpdatePackage.isEnabled = true
                binding.btnUpdatePackage.text = "Update Package"
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}