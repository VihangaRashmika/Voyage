package com.example.voyage_v2.ui.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.voyage_v2.HomeActivity
import com.example.voyage_v2.R
import com.example.voyage_v2.model.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SettingsFragment : Fragment() {

    companion object {
        fun newInstance() = SettingsFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        // Get user data from HomeActivity
        val homeActivity = activity as? HomeActivity
        val userType = homeActivity?.getUserType()
        val userEmail = homeActivity?.getUserEmail()
        val userName = homeActivity?.getUserName()

        // Find views
        val txtProfileInitial = view.findViewById<TextView>(R.id.txtProfileInitial)
        val txtUsername = view.findViewById<TextView>(R.id.txtUsername)
        val txtEmail = view.findViewById<TextView>(R.id.txtEmail)
        val txtUserType = view.findViewById<TextView>(R.id.txtUserType)
        val inputCurrentPassword = view.findViewById<EditText>(R.id.inputCurrentPassword)
        val inputNewPassword = view.findViewById<EditText>(R.id.inputNewPassword)
        val inputConfirmNewPassword = view.findViewById<EditText>(R.id.inputConfirmNewPassword)
        val btnChangePassword = view.findViewById<Button>(R.id.btnChangePassword)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        // Display user data
        txtUsername?.text = userName ?: "User"
        txtEmail?.text = userEmail ?: "user@example.com"

        // Display user type in friendly format
        val displayUserType = when(userType) {
            "BusinessOwner" -> "Business Owner"
            "Traveller" -> "Traveller"
            else -> userType ?: "User"
        }
        txtUserType?.text = displayUserType

        // Set profile initial (first letter of username)
        val initial = userName?.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
        txtProfileInitial?.text = initial

        // Change password button
        btnChangePassword?.setOnClickListener {
            val currentPassword = inputCurrentPassword?.text.toString().trim()
            val newPassword = inputNewPassword?.text.toString().trim()
            val confirmPassword = inputConfirmNewPassword?.text.toString().trim()

            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(requireContext(), "New passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword.length < 6) {
                Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            changePassword(
                userEmail ?: "",
                currentPassword,
                newPassword,
                inputCurrentPassword,
                inputNewPassword,
                inputConfirmNewPassword
            )
        }

        // Logout button
        btnLogout?.setOnClickListener {
            Toast.makeText(requireContext(), "Logging out...", Toast.LENGTH_SHORT).show()
            homeActivity?.logout()
        }

        return view
    }

    private fun changePassword(
        email: String,
        currentPassword: String,
        newPassword: String,
        inputCurrent: EditText?,
        inputNew: EditText?,
        inputConfirm: EditText?
    ) {
        if (email.isEmpty()) {
            Toast.makeText(requireContext(), "User email not found", Toast.LENGTH_SHORT).show()
            return
        }

        val dbRef = FirebaseDatabase.getInstance().getReference("users")
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var passwordChanged = false

                for (child in snapshot.children) {
                    val user = child.getValue(User::class.java)
                    if (user != null && user.email == email && user.password == currentPassword) {
                        // Update password
                        child.ref.child("password").setValue(newPassword)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Password changed successfully!", Toast.LENGTH_LONG).show()
                                inputCurrent?.text?.clear()
                                inputNew?.text?.clear()
                                inputConfirm?.text?.clear()
                            }
                            .addOnFailureListener {
                                Toast.makeText(requireContext(), "Failed to change password", Toast.LENGTH_SHORT).show()
                            }
                        passwordChanged = true
                        break
                    }
                }

                if (!passwordChanged) {
                    Toast.makeText(requireContext(), "Current password is incorrect", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}