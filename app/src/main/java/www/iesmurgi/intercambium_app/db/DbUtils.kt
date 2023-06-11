package www.iesmurgi.intercambium_app.db

import com.firebase.ui.auth.IdpResponse
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import www.iesmurgi.intercambium_app.models.Ad
import www.iesmurgi.intercambium_app.models.User
import www.iesmurgi.intercambium_app.utils.Constants
import java.util.HashMap

/**
 * Utility class for database operations.
 *
 * @author Denis Yushkin
 */
class DbUtils {
    companion object {
        /**
         * Retrieves the default user data.
         *
         * @return A mutable map containing the default user data.
         */
        private fun getDefaultUserData(): MutableMap<String, Any> {
            return hashMapOf(
                Constants.USERS_FIELD_ADMINISTRATOR to false,
                Constants.USERS_FIELD_NAME to "",
                Constants.USERS_FIELD_PHONE_NUMBER to "",
                Constants.USERS_FIELD_PHOTO_URL to "",
            )
        }

        /**
         * Retrieves the data of an ad and converts it into a [HashMap].
         *
         * @param ad The [Ad] object containing the data.
         * @return A [HashMap] containing the ad data with field names as keys.
         */
        fun getAdData(ad: Ad): HashMap<String, Any> {
            val titleWords = ad.title.lowercase().split(" ")
            val descriptionWords = ad.description.lowercase().split(" ")

            return hashMapOf(
                Constants.ADS_FIELD_TITLE to ad.title,
                Constants.ADS_FIELD_DESCRIPTION to ad.description,
                Constants.ADS_FIELD_PROVINCE to ad.province,
                Constants.ADS_FIELD_STATUS to ad.status,
                Constants.ADS_FIELD_CREATED_AT to ad.createdAt,
                Constants.ADS_FIELD_IMAGE to ad.imgUrl,
                Constants.ADS_FIELD_AUTHOR to ad.author.email,
                Constants.ADS_FIELD_TITLE_SEARCH to titleWords,
                Constants.ADS_FIELD_DESCRIPTION_SEARCH to descriptionWords
            )
        }

        /**
         * Creates a new user with Google authentication.
         *
         * @param response The [IdpResponse] object containing the authentication response.
         */
        fun createNewUserWithGoogle(response: IdpResponse) {
            val data = getDefaultUserData()
            data[Constants.USERS_FIELD_NAME] = response.user.name ?: ""
            data[Constants.USERS_FIELD_PHONE_NUMBER] = response.phoneNumber ?: ""
            data[Constants.USERS_FIELD_PHOTO_URL] = (response.user.photoUri ?: "") as String
            val email = response.email.toString()

            // If name is not specified, then take first part of the email
            if ((data[Constants.USERS_FIELD_NAME] as String).isEmpty()) {
                data[Constants.USERS_FIELD_NAME] = email.substringBefore("@")
            }

            createNewUserDocument(email, data)
        }

        /**
         * Creates a new user with email authentication.
         *
         * @param email The email address of the user.
         */
        fun createNewUserWithEmail(email: String) {
            val data = getDefaultUserData()
            data[Constants.USERS_FIELD_NAME] = email.substringBefore("@")

            createNewUserDocument(email, data)
        }

        /**
         * Creates a new user document in the database.
         *
         * @param email The email address of the user.
         * @param data A mutable map containing the user data.
         */
        private fun createNewUserDocument(email: String, data: MutableMap<String, Any>) {
            val db = Firebase.firestore
            db.collection(Constants.COLLECTION_USERS)
                .document(email)
                .set(data)
        }

        /**
         * Extension function to convert a [DocumentSnapshot] to a [User] object.
         *
         * @return The converted [User] object.
         */
        fun DocumentSnapshot.toUser(): User {
            val email = id
            val name = getString(Constants.USERS_FIELD_NAME).orEmpty()
            val phoneNumber = getString(Constants.USERS_FIELD_PHONE_NUMBER).orEmpty()
            val photoUrl = getString(Constants.USERS_FIELD_PHOTO_URL).orEmpty()
            val isAdministrator = getBoolean(Constants.USERS_FIELD_ADMINISTRATOR) ?: false

            return User(email, name, phoneNumber, photoUrl, isAdministrator)
        }

        /**
         * Extension function to convert a [DocumentSnapshot] to an [Ad] object.
         *
         * @param author The [User] object representing the author of the [Ad].
         * @return The converted [Ad] object.
         */
        fun DocumentSnapshot.toAd(author: User): Ad {
            val id = id
            val title = getString(Constants.ADS_FIELD_TITLE).orEmpty()
            val description = getString(Constants.ADS_FIELD_DESCRIPTION).orEmpty()
            val province = getString(Constants.ADS_FIELD_PROVINCE).orEmpty()
            val status = getString(Constants.ADS_FIELD_STATUS).orEmpty()
            val createdAt = getTimestamp(Constants.ADS_FIELD_CREATED_AT) ?: Timestamp.now()
            val imgUrl = getString(Constants.ADS_FIELD_IMAGE).orEmpty()

            return Ad(id, title, description, province, status, createdAt, imgUrl, author)
        }
    }
}