package ru.netology.nmedia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import ru.netology.nmedia.auth.AppAuth

class AuthViewModel : ViewModel() {
    val data = AppAuth.getInstance().authState.asLiveData()
    val authenticated: Boolean
        get() {
            val state = data.value
            return state != null && state.id != 0L && !state.token.isNullOrEmpty()
        }
}