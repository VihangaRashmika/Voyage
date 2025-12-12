package com.example.voyage_v2.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.voyage_v2.databinding.ItemPackageBinding
import com.example.voyage_v2.models.TravelPackage
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PackageAdapter(
    private var packages: MutableList<TravelPackage>,
    private val onPackageClick: ((TravelPackage) -> Unit)? = null,
    private val onEditClick: ((TravelPackage) -> Unit)? = null,
    private val onDeleteClick: ((TravelPackage) -> Unit)? = null,
    private val showActions: Boolean = false
) : RecyclerView.Adapter<PackageAdapter.PackageViewHolder>() {

    inner class PackageViewHolder(val binding: ItemPackageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackageViewHolder {
        val binding = ItemPackageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PackageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PackageViewHolder, position: Int) {
        val item = packages[position]

        holder.binding.txtPackageName.text = item.name
        holder.binding.txtPackageDescription.text = item.description
        holder.binding.txtPackagePrice.text = "LKR ${item.price}"
        holder.binding.txtPackageAuthor.text = "By ${item.author}"

        // Extract cover image (first URL from comma-separated string)
        val coverImage = item.image.split(",").firstOrNull()?.trim() ?: item.image

        Glide.with(holder.itemView.context)
            .load(coverImage)
            .placeholder(android.R.color.darker_gray)
            .into(holder.binding.imgPackage)

        // Load and display average rating
        loadAverageRating(item.name, holder)

        // Show/hide action buttons
        if (showActions) {
            holder.binding.actionButtons.visibility = View.VISIBLE

            holder.binding.btnEdit.setOnClickListener {
                onEditClick?.invoke(item)
            }

            holder.binding.btnDelete.setOnClickListener {
                onDeleteClick?.invoke(item)
            }
        } else {
            holder.binding.actionButtons.visibility = View.GONE
        }

        // Add click listener for card
        holder.itemView.setOnClickListener {
            onPackageClick?.invoke(item)
        }
    }

    private fun loadAverageRating(packageName: String, holder: PackageViewHolder) {
        val dbRef = FirebaseDatabase.getInstance().getReference("ratings")
        dbRef.orderByChild("packageName").equalTo(packageName)
            .addListenerForSingleValueEvent(object : ValueEventListener {
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
                        holder.binding.ratingBar.rating = average
                        holder.binding.txtRatingCount.text = "($count)"
                    } else {
                        holder.binding.ratingBar.rating = 0f
                        holder.binding.txtRatingCount.text = "(0)"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    holder.binding.ratingBar.rating = 0f
                    holder.binding.txtRatingCount.text = "(0)"
                }
            })
    }

    override fun getItemCount(): Int = packages.size

    fun updateList(newList: List<TravelPackage>) {
        packages.clear()
        packages.addAll(newList)
        notifyDataSetChanged()
    }
}