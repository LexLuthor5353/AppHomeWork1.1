package ru.netology.nmedia.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nmedia.api.AuthApiService
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.util.SingleLiveEvent
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val api: AuthApiService,
    private val appAuth: AppAuth,
) : ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _loginError = SingleLiveEvent<Unit>()
    val loginError: LiveData<Unit> = _loginError

    private val _loginSuccess = SingleLiveEvent<Unit>()
    val loginSuccess: LiveData<Unit> = _loginSuccess

    fun login(login: String, pass: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = api.authenticate(login.trim(), pass)
                if (!response.isSuccessful) {
                    _loginError.value = Unit
                    return@launch
                }
                val body = response.body()
                if (body == null) {
                    _loginError.value = Unit
                    return@launch
                }
                appAuth.setAuth(body.id, body.token)
                _loginSuccess.value = Unit
            } catch (e: IOException) {
                e.printStackTrace()
                _loginError.value = Unit
            } finally {
                _loading.value = false
            }
        }
    }
}
