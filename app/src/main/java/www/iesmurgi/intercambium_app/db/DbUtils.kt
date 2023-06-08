package www.iesmurgi.intercambium_app.db

import com.firebase.ui.auth.IdpResponse
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import www.iesmurgi.intercambium_app.utils.Constants

class DbUtils {
    companion object {
        private fun getDefaultUserData(): MutableMap<String, Any> {
            return hashMapOf(
                Constants.USERS_FIELD_ADMINISTRATOR to false,
                Constants.USERS_FIELD_NAME to "",
                Constants.USERS_FIELD_AGE to 0,
                Constants.USERS_FIELD_PHONE_NUMBER to "",
                Constants.USERS_FIELD_PHOTO_URL to "",
            )
        }

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

        fun createNewUserWithEmail(email: String) {
            val data = getDefaultUserData()
            data[Constants.USERS_FIELD_NAME] = email.substringBefore("@")

            createNewUserDocument(email, data)
        }

        private fun createNewUserDocument(email: String, data: MutableMap<String, Any>) {
            val db = Firebase.firestore
            db.collection(Constants.COLLECTION_USERS)
                .document(email)
                .set(data)
        }
    }
}