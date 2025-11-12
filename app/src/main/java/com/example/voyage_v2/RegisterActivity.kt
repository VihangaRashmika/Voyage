package com.example.voyage_v2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.voyage_v2.databinding.ActivityRegisterBinding
import com.example.voyage_v2.model.User
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.txtLoginLink.setOnClickListener {
            intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        binding.btnSubmit.setOnClickListener {
            Log.d("RegisterActivity", "Submit button clicked")
            saveUserData()
        }

        binding.btnReset.setOnClickListener {
            binding.inputUsername.text.clear()
            binding.inputEmail.text.clear()
            binding.inputPassword.text.clear()
            binding.inputConfirmPassword.text.clear()
            binding.radioTraveller.isChecked = true
        }
    }

    private fun saveUserData() {
        val username = binding.inputUsername.text.toString().trim()
        val email = binding.inputEmail.text.toString().trim()
        val password = binding.inputPassword.text.toString().trim()
        val confirmPassword = binding.inputConfirmPassword.text.toString().trim()
        val userType =
            if (binding.radioTraveller.isChecked) "Traveller" else "BusinessOwner"

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        val user = User(username, email, password, userType)

        val dbRef = FirebaseDatabase.getInstance().getReference("users")
        val userId = dbRef.push().key!!
        dbRef.child(userId).setValue(user)
            .addOnSuccessListener {

                Toast.makeText(this, "User registered successfully!", Toast.LENGTH_SHORT).show()
                Log.d("RegisterActivity", "User saved: $userId -> $user")

                binding.inputUsername.text.clear()
                binding.inputEmail.text.clear()
                binding.inputPassword.text.clear()
                binding.inputConfirmPassword.text.clear()
                binding.radioTraveller.isChecked = true

                intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("RegisterActivity", "Firebase save error", e)
            }

        Log.d("RegisterActivity", "Firebase reference: $dbRef")
        Log.d("RegisterActivity", "Generated userId: $userId")
    }

}