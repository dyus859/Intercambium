package www.iesmurgi.intercambium_app.utils

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import www.iesmurgi.intercambium_app.models.User

/**
 * Helper class for sending Firebase Cloud Messaging (FCM) notifications.
 *
 * @author Denis Yushkin
 */
class FCMHelper {
    companion object {
        private const val serverKey = "AAAARf7AhGI:APA91bFeSjcXvlekdUOnIq7di00a6_HdF5lgW5uJtVEvU4auIo6qDF1EFRRwGzJKiWY3PFNDudtBhfl8zPVxYmpTcpgzQrWfd_ynGahXDMa43rzECEwkHzgLjLSzGG32Tn12xHeH-SCF"
        private const val fcmUrl = "https://fcm.googleapis.com/fcm/send"
        private val mediaType = "application/json".toMediaType()
        private val client = OkHttpClient()
        private val gson = Gson()

        /**
         * Sends a notification to a device using its FCM token.
         *
         * @param fcmToken The FCM token of the device.
         * @param notificationBody The body of the notification.
         * @param type The type of the notification.
         * @param userData The user data associated with the notification.
         */
        fun sendNotificationToDevice(
            fcmToken: String,
            notificationBody: String,
            type: String,
            userData: User?
        ) {
            val userDataJson = gson.toJson(userData)

            val json = """
            {
                "to": "$fcmToken",
                "notification": {
                    "body": "$notificationBody"
                },
                "data": {
                    "type": "$type",
                    "userData": $userDataJson
                }
            }
            """.trimIndent()


            val requestBody = json.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(fcmUrl)
                .post(requestBody)
                .addHeader("Authorization", "key=$serverKey")
                .build()

            GlobalScope.launch(Dispatchers.IO) {
                val response = client.newCall(request).execute()

                // Handle the response as needed
                if (response.isSuccessful) {
                    println("Notification sent successfully.")
                } else {
                    println("Failed to send notification. Error: ${response.code} ${response.message}")
                }
            }
        }
    }
}