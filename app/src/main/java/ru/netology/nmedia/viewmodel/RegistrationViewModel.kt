package ru.netology.nmedia.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.netology.nmedia.api.AuthApi
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.util.SingleLiveEvent
import java.io.IOException

class RegistrationViewModel : ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _passwordMismatch = SingleLiveEvent<Unit>()
    val passwordMismatch: LiveData<Unit> = _passwordMismatch

    private val _registerError = SingleLiveEvent<Unit>()
    val registerError: LiveData<Unit> = _registerError

    private val _registerSuccess = SingleLiveEvent<Unit>()
    val registerSuccess: LiveData<Unit> = _registerSuccess

    fun register(name: String, login: String, pass: String, confirm: String) {
        if (pass != confirm) {
            _passwordMismatch.value = Unit
            return
        }
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = AuthApi.service.register(login.trim(), pass, name.trim())
                if (!response.isSuccessful) {
                    _registerError.value = Unit
                    return@launch
                }
                val body = response.body()
                if (body == null) {
                    _registerError.value = Unit
                    return@launch
                }
                AppAuth.getInstance().setAuth(body.id, body.token)
                _registerSuccess.value = Unit
            } catch (e: IOException) {
                e.printStackTrace()
                _registerError.value = Unit
            } finally {
                _loading.value = false
            }
        }
    }
}
