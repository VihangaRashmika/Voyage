package com.example.voyage_v2.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.voyage_v2.databinding.ItemPackageBinding
import com.example.voyage_v2.models.TravelPackage

class PackageAdapter(private var packages: MutableList<TravelPackage>) :
    RecyclerView.Adapter<PackageAdapter.PackageViewHolder>() {

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

        Glide.with(holder.itemView.context)
            .load(item.image)
            .placeholder(android.R.color.darker_gray)
            .into(holder.binding.imgPackage)
    }

    override fun getItemCount(): Int = packages.size

    /** Call this method to refresh the RecyclerView with new data */
    fun updateList(newList: List<TravelPackage>) {
        packages.clear()
        packages.addAll(newList)
        notifyDataSetChanged()
    }
}
