package ru.netology.nmedia.auth

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.dto.PushToken

class AppAuth private constructor(context: Context) {
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val _authStateFlow = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState>
        get() = _authStateFlow

    init {
        val id = prefs.getLong(KEY_ID, 0)
        val token = prefs.getString(KEY_TOKEN, null)
        if (id != 0L && !token.isNullOrEmpty()) {
            _authStateFlow.value = AuthState(id, token)
        }
        sendPushToken()
    }

    fun setAuth(id: Long, token: String) {
        _authStateFlow.value = AuthState(id, token)
        prefs.edit {
            putLong(KEY_ID, id)
            putString(KEY_TOKEN, token)
        }
        sendPushToken()
    }

    fun removeAuth() {
        _authStateFlow.value = AuthState()
        prefs.edit {
            putLong(KEY_ID, 0)
            remove(KEY_TOKEN)
        }
        sendPushToken()
    }

    fun sendPushToken(token: String? = null) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val pushToken = token ?: FirebaseMessaging.getInstance().token.await()
                Log.d("FCM_TOKEN", pushToken)
                PostsApi.service.save(PushToken(pushToken))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        private const val KEY_ID = "id"
        private const val KEY_TOKEN = "token"
        private var INSTANCE: AppAuth? = null

        fun getInstance() = INSTANCE ?: throw IllegalStateException("AppAuth is not initialized")

        fun init(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = AppAuth(context.applicationContext)
            }
        }
    }
}

data class AuthState(val id: Long = 0, val token: String? = null)
