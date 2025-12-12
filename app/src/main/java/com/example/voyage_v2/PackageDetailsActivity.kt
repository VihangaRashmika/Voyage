package com.example.voyage_v2

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.voyage_v2.databinding.ActivityPackageDetailsBinding
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

data class Review(
    val userName: String = "",
    val rating: Float = 0f,
    val reviewText: String = "",
    val timestamp: Long = 0,
    val userId: String = ""
)

class PackageDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPackageDetailsBinding
    private lateinit var reviewAdapter: ReviewAdapter
    private lateinit var imageAdapter: ImageSliderAdapter
    private val reviewList = ArrayList<Review>()
    private val imageList = ArrayList<String>()
    private var packageName: String = ""
    private var userType: String = ""
    private var userName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPackageDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get data from intent
        packageName = intent.getStringExtra("packageName") ?: ""
        val price = intent.getDoubleExtra("price", 0.0)
        val description = intent.getStringExtra("description") ?: ""
        val image = intent.getStringExtra("image") ?: ""
        val author = intent.getStringExtra("author") ?: ""
        val phone = intent.getStringExtra("phone") ?: ""
        val facilities = intent.getStringExtra("facilities") ?: ""
        val location = intent.getStringExtra("location") ?: ""
        userType = intent.getStringExtra("userType") ?: ""
        userName = intent.getStringExtra("userName") ?: ""

        // Set data to views
        binding.txtPackageName.text = packageName
        binding.txtLocation.text = location
        binding.txtPrice.text = "LKR $price"
        binding.txtPhone.text = phone
        binding.txtDescription.text = description
        binding.txtFacilities.text = facilities

        // Setup image gallery
        setupImageGallery(image)

        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Phone call button
        binding.btnCall.setOnClickListener {
            if (phone.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                startActivity(intent)
            } else {
                Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show()
            }
        }

        // Setup reviews RecyclerView
        setupReviewsRecyclerView()

        // Load average rating
        loadAverageRating()
        loadUserRating()

        // Load reviews
        loadReviews()

        // Write Review Button - only for customers
        if (userType == "Traveller") {
            binding.btnWriteReview.visibility = View.VISIBLE
            binding.btnWriteReview.setOnClickListener {
                showWriteReviewDialog(null)
            }
        }

        // Rating bar - only for customers
        if (userType == "Traveller") {
            binding.ratingBar.setIsIndicator(false)

            binding.ratingBar.setOnRatingBarChangeListener { _, rating, fromUser ->
                if (fromUser && rating > 0) {
                    AlertDialog.Builder(this)
                        .setTitle("Rate Package")
                        .setMessage("Rate this package ${rating.toInt()} stars?")
                        .setPositiveButton("Confirm") { _, _ ->
                            saveRating(rating)
                        }
                        .setNegativeButton("Cancel") { _, _ ->
                            loadUserRating()
                        }
                        .show()
                }
            }

            binding.ratingBar.setOnLongClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Clear Rating")
                    .setMessage("Remove your rating?")
                    .setPositiveButton("Yes") { _, _ ->
                        clearRating()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                true
            }
        } else {
            binding.ratingBar.setIsIndicator(true)
        }
    }

    private fun setupImageGallery(imagesString: String) {
        // Split comma-separated image URLs
        imageList.clear()
        val urls = imagesString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        imageList.addAll(urls)

        // Setup ViewPager2 with adapter
        imageAdapter = ImageSliderAdapter(imageList)
        binding.imageViewPager.adapter = imageAdapter

        // Setup dots indicator
        setupDotsIndicator(imageList.size)

        // Update dots on page change
        binding.imageViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDotsIndicator(position)
            }
        })
    }

    private fun setupDotsIndicator(count: Int) {
        binding.dotsIndicator.removeAllViews()

        if (count <= 1) {
            binding.dotsIndicator.visibility = View.GONE
            return
        }

        val dots = Array(count) { ImageView(this) }

        for (i in dots.indices) {
            dots[i] = ImageView(this)
            dots[i].setImageResource(android.R.drawable.presence_invisible)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(8, 0, 8, 0)

            binding.dotsIndicator.addView(dots[i], params)
        }

        if (dots.isNotEmpty()) {
            dots[0].setImageResource(android.R.drawable.presence_online)
        }
    }

    private fun updateDotsIndicator(position: Int) {
        val childCount = binding.dotsIndicator.childCount

        for (i in 0 until childCount) {
            val imageView = binding.dotsIndicator.getChildAt(i) as ImageView
            if (i == position) {
                imageView.setImageResource(android.R.drawable.presence_online)
            } else {
                imageView.setImageResource(android.R.drawable.presence_invisible)
            }
        }
    }

    private fun setupReviewsRecyclerView() {
        reviewAdapter = ReviewAdapter(reviewList, userName, userType,
            onEdit = { review -> showWriteReviewDialog(review) },
            onDelete = { review -> deleteReview(review) }
        )
        binding.recyclerViewReviews.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewReviews.adapter = reviewAdapter
    }

    private fun loadReviews() {
        val dbRef = FirebaseDatabase.getInstance().getReference("ratings")
        dbRef.orderByChild("packageName").equalTo(packageName)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    reviewList.clear()

                    for (child in snapshot.children) {
                        val userName = child.child("userName").getValue(String::class.java) ?: ""
                        val rating = child.child("rating").getValue(Float::class.java) ?: 0f
                        val reviewText = child.child("reviewText").getValue(String::class.java) ?: ""
                        val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                        val userId = child.child("userId").getValue(String::class.java) ?: userName

                        if (reviewText.isNotEmpty()) {
                            reviewList.add(Review(userName, rating, reviewText, timestamp, userId))
                        }
                    }

                    reviewList.sortByDescending { it.timestamp }
                    reviewAdapter.notifyDataSetChanged()

                    if (reviewList.isEmpty()) {
                        binding.txtNoReviews.visibility = View.VISIBLE
                        binding.recyclerViewReviews.visibility = View.GONE
                    } else {
                        binding.txtNoReviews.visibility = View.GONE
                        binding.recyclerViewReviews.visibility = View.VISIBLE
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun showWriteReviewDialog(existingReview: Review?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_write_review, null)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBarDialog)
        val edtReview = dialogView.findViewById<EditText>(R.id.edtReviewText)
        val btnSubmit = dialogView.findViewById<Button>(R.id.btnSubmitReview)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelReview)

        if (existingReview != null) {
            ratingBar.rating = existingReview.rating
            edtReview.setText(existingReview.reviewText)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnSubmit.setOnClickListener {
            val rating = ratingBar.rating
            val reviewText = edtReview.text.toString().trim()

            if (rating == 0f) {
                Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (reviewText.isEmpty()) {
                Toast.makeText(this, "Please write a review", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveReview(rating, reviewText)
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveReview(rating: Float, reviewText: String) {
        val dbRef = FirebaseDatabase.getInstance().getReference("ratings")
        val queryKey = "${packageName}_$userName"

        dbRef.orderByChild("packageName").equalTo(packageName)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var existingRatingKey: String? = null

                    for (child in snapshot.children) {
                        val ratingUserName = child.child("userName").getValue(String::class.java)
                        if (ratingUserName == userName) {
                            existingRatingKey = child.key
                            break
                        }
                    }

                    val reviewData = hashMapOf(
                        "packageName" to packageName,
                        "userName" to userName,
                        "userId" to userName,
                        "rating" to rating,
                        "reviewText" to reviewText,
                        "timestamp" to System.currentTimeMillis(),
                        "queryKey" to queryKey
                    )

                    if (existingRatingKey != null) {
                        dbRef.child(existingRatingKey).setValue(reviewData)
                            .addOnSuccessListener {
                                Toast.makeText(this@PackageDetailsActivity, "Review updated! 💬", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        val reviewId = dbRef.push().key ?: return
                        dbRef.child(reviewId).setValue(reviewData)
                            .addOnSuccessListener {
                                Toast.makeText(this@PackageDetailsActivity, "Review posted! 💬", Toast.LENGTH_SHORT).show()
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun deleteReview(review: Review) {
        AlertDialog.Builder(this)
            .setTitle("Delete Review")
            .setMessage("Are you sure you want to delete this review?")
            .setPositiveButton("Delete") { _, _ ->
                val dbRef = FirebaseDatabase.getInstance().getReference("ratings")
                dbRef.orderByChild("packageName").equalTo(packageName)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (child in snapshot.children) {
                                val ratingUserName = child.child("userName").getValue(String::class.java)
                                if (ratingUserName == review.userName) {
                                    child.ref.removeValue()
                                        .addOnSuccessListener {
                                            Toast.makeText(this@PackageDetailsActivity, "Review deleted", Toast.LENGTH_SHORT).show()
                                        }
                                    break
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadAverageRating() {
        val dbRef = FirebaseDatabase.getInstance().getReference("ratings")
        dbRef.orderByChild("packageName").equalTo(packageName)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var totalRating = 0.0
                    var count = 0

                    for (child in snapshot.children) {
                        val rating = child.child("rating").getValue(Float::class.java) ?: 0f
                        totalRating += rating
                        count++
                    }

                    if (count > 0) {
                        val average = (totalRating / count).toFloat()
                        binding.ratingBar.rating = average
                        binding.txtRatingCount.text = "($count ratings)"
                    } else {
                        binding.txtRatingCount.text = "(No ratings yet)"
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadUserRating() {
        if (userType != "Traveller") return

        val dbRef = FirebaseDatabase.getInstance().getReference("ratings")
        dbRef.orderByChild("packageName").equalTo(packageName)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        val ratingUserName = child.child("userName").getValue(String::class.java)
                        if (ratingUserName == userName) {
                            val rating = child.child("rating").getValue(Float::class.java) ?: 0f
                            binding.ratingBar.rating = rating
                            break
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun saveRating(rating: Float) {
        val dbRef = FirebaseDatabase.getInstance().getReference("ratings")
        val queryKey = "${packageName}_$userName"

        dbRef.orderByChild("packageName").equalTo(packageName)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var existingRatingKey: String? = null

                    for (child in snapshot.children) {
                        val ratingUserName = child.child("userName").getValue(String::class.java)
                        if (ratingUserName == userName) {
                            existingRatingKey = child.key
                            break
                        }
                    }

                    val ratingData = hashMapOf(
                        "packageName" to packageName,
                        "userName" to userName,
                        "userId" to userName,
                        "rating" to rating,
                        "timestamp" to System.currentTimeMillis(),
                        "queryKey" to queryKey,
                        "reviewText" to ""
                    )

                    if (existingRatingKey != null) {
                        val existingReview = snapshot.children.find {
                            it.child("userName").getValue(String::class.java) == userName
                        }
                        val existingText = existingReview?.child("reviewText")?.getValue(String::class.java) ?: ""
                        ratingData["reviewText"] = existingText

                        dbRef.child(existingRatingKey).setValue(ratingData)
                            .addOnSuccessListener {
                                Toast.makeText(this@PackageDetailsActivity, "Rating updated! ⭐", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        val ratingId = dbRef.push().key ?: return
                        dbRef.child(ratingId).setValue(ratingData)
                            .addOnSuccessListener {
                                Toast.makeText(this@PackageDetailsActivity, "Thank you for rating! ⭐", Toast.LENGTH_SHORT).show()
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun clearRating() {
        val dbRef = FirebaseDatabase.getInstance().getReference("ratings")

        dbRef.orderByChild("packageName").equalTo(packageName)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        val ratingUserName = child.child("userName").getValue(String::class.java)
                        if (ratingUserName == userName) {
                            child.ref.removeValue()
                                .addOnSuccessListener {
                                    binding.ratingBar.rating = 0f
                                    Toast.makeText(this@PackageDetailsActivity, "Rating removed", Toast.LENGTH_SHORT).show()
                                    loadAverageRating()
                                }
                            break
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}

// Image Slider Adapter
class ImageSliderAdapter(private val images: List<String>) : RecyclerView.Adapter<ImageSliderAdapter.ImageViewHolder>() {

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageSlide)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image_slide, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        Glide.with(holder.itemView.context)
            .load(images[position])
            .placeholder(android.R.color.darker_gray)
            .into(holder.imageView)
    }

    override fun getItemCount() = images.size
}

// Review Adapter (Keep existing)
class ReviewAdapter(
    private val reviews: List<Review>,
    private val currentUserName: String,
    private val userType: String,
    private val onEdit: (Review) -> Unit,
    private val onDelete: (Review) -> Unit
) : RecyclerView.Adapter<ReviewAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtUserAvatar: TextView = view.findViewById(R.id.txtUserAvatar)
        val txtUserName: TextView = view.findViewById(R.id.txtUserName)
        val txtReviewTime: TextView = view.findViewById(R.id.txtReviewTime)
        val ratingBar: RatingBar = view.findViewById(R.id.ratingBarReview)
        val txtReviewText: TextView = view.findViewById(R.id.txtReviewText)
        val layoutActions: LinearLayout = view.findViewById(R.id.layoutActions)
        val btnEdit: TextView = view.findViewById(R.id.btnEditReview)
        val btnDelete: TextView = view.findViewById(R.id.btnDeleteReview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val review = reviews[position]

        holder.txtUserAvatar.text = review.userName.firstOrNull()?.uppercase() ?: "?"
        holder.txtUserName.text = review.userName
        holder.ratingBar.rating = review.rating
        holder.txtReviewText.text = review.reviewText
        holder.txtReviewTime.text = getTimeAgo(review.timestamp)

        if (userType == "Traveller" && review.userName == currentUserName) {
            holder.layoutActions.visibility = View.VISIBLE
            holder.btnEdit.setOnClickListener { onEdit(review) }
            holder.btnDelete.setOnClickListener { onDelete(review) }
        } else {
            holder.layoutActions.visibility = View.GONE
        }
    }

    override fun getItemCount() = reviews.size

    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            diff < 604800000 -> "${diff / 86400000}d ago"
            else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }
}