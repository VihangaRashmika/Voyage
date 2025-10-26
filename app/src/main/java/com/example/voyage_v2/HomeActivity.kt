package com.example.voyage_v2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.voyage_v2.databinding.ActivityHomeMenuBinding
import com.example.voyage_v2.ui.home.HomeFragment
import com.example.voyage_v2.ui.home.PackageFragment
import com.example.voyage_v2.ui.home.SettingsFragment

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set default fragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, HomeFragment.newInstance())
                .commit()
        }

        // Bottom nav listener
        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            val fragment = when(menuItem.itemId) {
                R.id.nav_home -> HomeFragment.newInstance()
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
}
