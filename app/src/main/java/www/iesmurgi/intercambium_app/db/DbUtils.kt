package www.iesmurgi.intercambium_app.db

import com.firebase.ui.auth.IdpResponse
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import www.iesmurgi.intercambium_app.utils.Constants

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
                Constants.USERS_FIELD_AGE to 0,
                Constants.USERS_FIELD_PHONE_NUMBER to "",
                Constants.USERS_FIELD_PHOTO_URL to "",
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
    }
}