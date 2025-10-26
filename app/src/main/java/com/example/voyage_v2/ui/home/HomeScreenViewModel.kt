package com.example.voyage_v2.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeScreenViewModel : ViewModel() {
    private val _welcomeMessage = MutableLiveData("Welcome to Voyage!")
    val welcomeMessage: LiveData<String> get() = _welcomeMessage

    fun updateMessage(newMessage: String) {
        _welcomeMessage.value = newMessage
    }
}