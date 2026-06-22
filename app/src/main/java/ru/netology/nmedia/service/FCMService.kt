package ru.netology.nmedia.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.AppActivity
import ru.netology.nmedia.auth.AppAuth
import kotlin.random.Random
import javax.inject.Inject

@AndroidEntryPoint
class FCMService : FirebaseMessagingService() {

    @Inject
    lateinit var appAuth: AppAuth

    private val action = "action"
    private val content = "content"
    private val recipientIdKey = "recipientId"
    private val channelId = "remote"
    private val gson = Gson()

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_remote_name)
            val descriptionText = getString(R.string.channel_remote_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val contentStr = data[content] ?: return
        val push = parsePush(contentStr)
        val recipientId = parseRecipientId(data) ?: push?.recipientId
        if (!needShow(recipientId)) {
            return
        }

        val actionStr = data[action]
        if (actionStr == null) {
            val text = push?.content ?: contentStr
            showText(text)
            return
        }

        val pushAction = try {
            Action.valueOf(actionStr)
        } catch (e: IllegalArgumentException) {
            return
        }

        when (pushAction) {
            Action.LIKE -> handleLike(gson.fromJson(contentStr, Like::class.java))
            Action.NEW_POST -> handleNewPost(gson.fromJson(contentStr, NewPost::class.java))
        }
    }

    override fun onNewToken(token: String) {
        appAuth.sendPushToken(token)
    }

    private fun parseRecipientId(data: Map<String, String>): Long? {
        if (!data.containsKey(recipientIdKey)) {
            return null
        }
        val raw = data[recipientIdKey] ?: return null
        if (raw == "null" || raw.isEmpty()) {
            return null
        }
        return raw.toLongOrNull()
    }

    private fun needShow(recipientId: Long?): Boolean {
        val myId = appAuth.authStateFlow.value.id
        if (recipientId == null) {
            return true
        }
        if (recipientId == myId) {
            return true
        }
        appAuth.sendPushToken()
        return false
    }

    private fun parsePush(contentStr: String): Push? {
        return try {
            gson.fromJson(contentStr, Push::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun showText(text: String) {
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.baseline_circle_notifications_24)
            .setContentTitle(getString(R.string.nmedia))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        notify(notification)
    }

    private fun handleLike(content: Like) {
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.baseline_circle_notifications_24)
            .setContentTitle(
                getString(
                    R.string.notification_user_liked,
                    content.userName,
                    content.postAuthor,
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notify(notification)
    }

    private fun handleNewPost(content: NewPost) {
        val title = getString(R.string.new_post_title, content.userName)

        val maxPreviewLength = 200
        val truncatedText = if (content.postText.length > maxPreviewLength) {
            content.postText.take(maxPreviewLength).trimEnd() + "…"
        } else {
            content.postText
        }

        val intent = Intent(this, AppActivity::class.java).apply {
            putExtra("postId", content.postId)
            putExtra("openPost", true)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            content.postId.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(truncatedText)
            .setSummaryText(getString(R.string.tap_to_read_full))

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.baseline_circle_notifications_24)
            .setContentTitle(title)
            .setContentText(truncatedText)
            .setStyle(bigTextStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notify(notification)
    }

    private fun notify(notification: Notification) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this).notify(Random.nextInt(100_000), notification)
        }
    }
}

data class Push(
    val recipientId: Long? = null,
    val content: String = "",
)

enum class Action {
    LIKE,
    NEW_POST
}

data class Like(
    val userId: Long,
    val userName: String,
    val postId: Long,
    val postAuthor: String,
)

data class NewPost(
    val userId: Long,
    val userName: String,
    val postId: Long,
    val postText: String
)
