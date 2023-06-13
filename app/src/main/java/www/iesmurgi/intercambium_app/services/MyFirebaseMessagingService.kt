package www.iesmurgi.intercambium_app.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import www.iesmurgi.intercambium_app.R
import www.iesmurgi.intercambium_app.models.User
import www.iesmurgi.intercambium_app.ui.ChatActivity
import www.iesmurgi.intercambium_app.ui.MainActivity
import www.iesmurgi.intercambium_app.utils.Constants
import kotlin.random.Random

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Handle the received message and generate the notification
        remoteMessage.notification?.let { notification ->
            val body = notification.body
            val type = remoteMessage.data["type"]
            val userDataJson = remoteMessage.data["userData"]

            showNotification(body, type, userDataJson)
        }
    }

    private fun showNotification(body: String?, type: String?, userDataJson: String?) {
        val channelId = "NOTIFICATION_CHANNEL"
        var channelName: String

        val notificationId = Random.nextInt()

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val intent = when (type) {
            Constants.NOTIFICATION_TYPE_CHAT -> {
                channelName = Constants.NOTIFICATION_TYPE_CHAT.uppercase()

                // Deserialize the userData JSON into a User object
                val userData = Gson().fromJson(userDataJson, User::class.java)

                // Open ChatActivity with the user data
                val chatIntent = Intent(this, ChatActivity::class.java)
                chatIntent.putExtra("USER", userData)
                chatIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                chatIntent
            }
            else -> {
                channelName = Constants.NOTIFICATION_TYPE_MAIN.uppercase()

                // Open MainActivity for other types of notifications
                Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            pendingIntentFlags // setting the mutability flag
        )

        val notificationSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(notificationSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Check if the device is running Android Oreo or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT).apply {
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}