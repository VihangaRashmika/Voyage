package com.example.voyage_v2.models

data class TravelPackage(
    val name: String,
    val price: Double,
    val description: String,
    val image: String,
    val author: String,
    val phone: String = "",
    val facilities: String = "",
    val location: String = ""
)