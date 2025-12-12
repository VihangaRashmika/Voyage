package com.example.voyage_v2

import com.example.voyage_v2.ui.home.TravellerHomeFragment
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.voyage_v2.databinding.ActivityHomeMenuBinding
import com.example.voyage_v2.models.Package_Firebase
import com.example.voyage_v2.models.TravelPackage
import com.example.voyage_v2.ui.home.HomeFragment
import com.example.voyage_v2.ui.home.PackageFragment
import com.example.voyage_v2.ui.home.SettingsFragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class HomeActivity : AppCompatActivity() {

    var packageList = ArrayList<TravelPackage>()
    private lateinit var binding: ActivityHomeMenuBinding
    private var userType: String? = null
    private var userEmail: String? = null
    private var userName: String? = null

    private fun fetchPackagesFromFirebase() {
        val dbRef = FirebaseDatabase.getInstance().getReference("packages")
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newPackages = ArrayList<TravelPackage>()
                val allChildren = snapshot.children.toList().reversed() // Reverse to show newest first
                for (child in allChildren) {
                    val pkg = child.getValue(Package_Firebase::class.java)
                    pkg?.let {
                        newPackages.add(
                            TravelPackage(
                                name = it.name,
                                price = it.price,
                                description = it.description,
                                image = it.image,
                                author = it.author
                            )
                        )
                    }
                }
                packageList.clear()
                packageList.addAll(newPackages)
                val currentFragment = supportFragmentManager.findFragmentById(R.id.container)
                if (currentFragment is PackageFragment) {
                    currentFragment.updatePackageList(packageList)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HomeActivity, "Failed to load packages", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userType = intent.getStringExtra("userType")
        userEmail = intent.getStringExtra("userEmail")
        userName = intent.getStringExtra("userName")
        packageList.addAll(hardcodedPackages())
        fetchPackagesFromFirebase()

        // Set default fragment based on user type
        if (savedInstanceState == null) {
            val defaultFragment = if (userType == "BusinessOwner") {
                HomeFragment.newInstance()
            } else {
                TravellerHomeFragment.newInstance()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, defaultFragment)
                .commit()
        }

        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            val fragment = when(menuItem.itemId) {
                R.id.nav_home -> {
                    if (userType == "BusinessOwner") {
                        HomeFragment.newInstance()
                    } else {
                        TravellerHomeFragment.newInstance()
                    }
                }
                R.id.nav_package -> PackageFragment.newInstance()
                R.id.nav_settings -> SettingsFragment.newInstance()
                else -> HomeFragment.newInstance()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commit()
            true
        }
    }

    fun getUserType(): String? {
        return userType
    }

    fun getUserEmail(): String? {
        return userEmail
    }

    fun getUserName(): String? {
        return userName
    }

    fun logout() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }


    fun hardcodedPackages(): List<TravelPackage> {
        return listOf(
            TravelPackage(
                name = "Sigiriya & Cultural Triangle",
                price = 799.99,
                description = "Climb the lion-rock fortress of Sigiriya, explore ancient cities and temples in Sri Lanka's Cultural Triangle.",
                image = "https://images.unsplash.com/photo-1593284166821-0c90b50b9c23",
                author = "Ceylon Trails"
            ),
            TravelPackage(
                name = "Kandy Tea Country Retreat",
                price = 899.50,
                description = "Stay in the misty hills around Kandy, visit tea plantations and the Temple of the Tooth.",
                image = "https://images.unsplash.com/photo-1523186994990-4f1f1bc3fe20",
                author = "Hilltop Escapes"
            ),
            TravelPackage(
                name = "Ella Mountain Escape",
                price = 999.00,
                description = "Relax in Ella with views of little Adam's Peak, waterfalls and lush green valleys.",
                image = "https://images.unsplash.com/photo-1531177073171-1e415b93e06f",
                author = "UpCountry Journeys"
            ),
            TravelPackage(
                name = "South Coast Beach Paradise",
                price = 1099.99,
                description = "Enjoy the beaches of Mirissa and Unawatuna, whale-watching, chill seaside vibes.",
                image = "https://images.unsplash.com/photo-1518684079-3c830dcef090",
                author = "Island Retreats"
            ),
            TravelPackage(
                name = "Yala Safari & Wildlife Adventure",
                price = 1299.99,
                description = "Go on safari in Yala National Park, spot leopards, elephants and enjoy a coastal lodge stay.",
                image = "https://images.unsplash.com/photo-1505761671935-60b3a7427bad",
                author = "WildNature"
            ),
            TravelPackage(
                name = "Nuwara Eliya & Tea Trails",
                price = 1199.00,
                description = "Explore the Little England highland town, tea estates and cool climate vistas.",
                image = "https://images.unsplash.com/photo-1508264165352-258859e62245",
                author = "NorthTrail"
            ),
            TravelPackage(
                name = "Trincomalee East Coast Escape",
                price = 999.50,
                description = "Dive and snorkel in the azure waters of Trincomalee, visit temples and relaxed beaches.",
                image = "https://images.unsplash.com/photo-1533692326340-21b3f4a3d15a",
                author = "AsiaVibe"
            ),
            TravelPackage(
                name = "Galle & Southern Heritage Tour",
                price = 899.00,
                description = "Walk the ramparts of Galle Fort, enjoy colonial architecture, local cuisine and beach time.",
                image = "https://images.unsplash.com/photo-1502602898657-3e91760cbb34",
                author = "Dream Escapes"
            ),
            TravelPackage(
                name = "Wilpattu & Off-beat Sanctuary",
                price = 1099.00,
                description = "Visit Sri Lanka's largest national park, wild and less crowded, for wildlife lovers.",
                image = "https://images.unsplash.com/photo-1506744038136-46273834b3fb",
                author = "SafariVista"
            ),
            TravelPackage(
                name = "Colombo Urban & Coast Experience",
                price = 799.99,
                description = "Discover Colombo's city life, markets, coastal towns and vibrant street food.",
                image = "https://images.unsplash.com/photo-1600695263479-1d9dcd2b17a7",
                author = "UrbanGetaways"
            )
        )
    }}

