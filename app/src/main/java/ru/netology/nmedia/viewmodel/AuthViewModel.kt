package ru.netology.nmedia.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import ru.netology.nmedia.auth.AppAuth
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val appAuth: AppAuth,
) : ViewModel() {
    val data = appAuth.authStateFlow.asLiveData()
    val authenticated: Boolean
        get() {
            val state = data.value
            return state != null && state.id != 0L && !state.token.isNullOrEmpty()
        }
}
