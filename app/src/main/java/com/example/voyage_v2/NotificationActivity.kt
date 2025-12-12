package com.example.voyage_v2

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.voyage_v2.databinding.ActivityNotificationBinding
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

data class Notification(
    val packageName: String = "",
    val userName: String = "",
    val rating: Float = 0f,
    val timestamp: Long = 0
)

class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationBinding
    private lateinit var adapter: NotificationAdapter
    private val notificationList = ArrayList<Notification>()
    private var ownerName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ownerName = intent.getStringExtra("ownerName") ?: ""

        binding.btnBack.setOnClickListener {
            finish()
        }

        adapter = NotificationAdapter(notificationList) { notification ->
            openPackageDetails(notification.packageName)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        loadNotifications()
    }

    private fun loadNotifications() {
        // First, get owner's packages
        val packagesRef = FirebaseDatabase.getInstance().getReference("packages")
        packagesRef.orderByChild("author").equalTo(ownerName)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(packagesSnapshot: DataSnapshot) {
                    val ownerPackages = mutableListOf<String>()

                    for (child in packagesSnapshot.children) {
                        val packageName = child.child("name").getValue(String::class.java)
                        packageName?.let { ownerPackages.add(it) }
                    }

                    // Now get ratings for those packages
                    loadRatingsForPackages(ownerPackages)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadRatingsForPackages(ownerPackages: List<String>) {
        if (ownerPackages.isEmpty()) {
            binding.txtNoNotifications.visibility = View.VISIBLE
            return
        }

        val ratingsRef = FirebaseDatabase.getInstance().getReference("ratings")
        ratingsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                notificationList.clear()

                for (child in snapshot.children) {
                    val packageName = child.child("packageName").getValue(String::class.java) ?: ""

                    if (ownerPackages.contains(packageName)) {
                        val userName = child.child("userName").getValue(String::class.java) ?: "Someone"
                        val rating = child.child("rating").getValue(Float::class.java) ?: 0f
                        val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L

                        notificationList.add(
                            Notification(packageName, userName, rating, timestamp)
                        )
                    }
                }

                // Sort by timestamp (newest first)
                notificationList.sortByDescending { it.timestamp }

                adapter.notifyDataSetChanged()

                if (notificationList.isEmpty()) {
                    binding.txtNoNotifications.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                } else {
                    binding.txtNoNotifications.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun openPackageDetails(packageName: String) {
        // Get package details and open details screen
        val packagesRef = FirebaseDatabase.getInstance().getReference("packages")
        packagesRef.orderByChild("name").equalTo(packageName)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        val intent = Intent(this@NotificationsActivity, PackageDetailsActivity::class.java)
                        intent.putExtra("packageName", child.child("name").getValue(String::class.java))
                        intent.putExtra("price", child.child("price").getValue(Double::class.java))
                        intent.putExtra("description", child.child("description").getValue(String::class.java))
                        intent.putExtra("image", child.child("image").getValue(String::class.java))
                        intent.putExtra("author", child.child("author").getValue(String::class.java))
                        intent.putExtra("phone", child.child("phone").getValue(String::class.java))
                        intent.putExtra("facilities", child.child("facilities").getValue(String::class.java))
                        intent.putExtra("location", child.child("location").getValue(String::class.java))
                        intent.putExtra("userType", "BusinessOwner")
                        intent.putExtra("userName", ownerName)
                        startActivity(intent)
                        break
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}

class NotificationAdapter(
    private val notifications: List<Notification>,
    private val onClick: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtMessage: TextView = view.findViewById(R.id.txtMessage)
        val txtTime: TextView = view.findViewById(R.id.txtTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = notifications[position]

        val stars = "⭐".repeat(notification.rating.toInt())
        holder.txtMessage.text = "${notification.userName} rated '${notification.packageName}' $stars"

        val timeAgo = getTimeAgo(notification.timestamp)
        holder.txtTime.text = timeAgo

        holder.itemView.setOnClickListener {
            onClick(notification)
        }
    }

    override fun getItemCount() = notifications.size

    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000} minutes ago"
            diff < 86400000 -> "${diff / 3600000} hours ago"
            diff < 604800000 -> "${diff / 86400000} days ago"
            else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }
}