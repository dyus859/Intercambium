package www.iesmurgi.intercambium_app.db

import com.firebase.ui.auth.IdpResponse
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import www.iesmurgi.intercambium_app.utils.Constants

class DbUtils {
    companion object {
        private fun getDefaultUserData(): MutableMap<String, Any> {
            return hashMapOf(
                "administrator" to false,
                "name" to "",
                "phoneNumber" to "",
                "photoUrl" to "",
            )
        }

        fun createNewUserWithGoogle(response: IdpResponse) {
            val data = getDefaultUserData()
            data["name"] = response.user.name ?: ""
            data["photoNumber"] = response.phoneNumber ?: ""
            data["photoUrl"] = (response.user.photoUri ?: "") as String
            val email = response.email.toString()

            // If name is not specified, then take first part of the email
            if ((data["name"] as String).isEmpty()) {
                data["name"] = email.substringBefore("@")
            }

            createNewUserDocument(email, data)
        }

        fun createNewUserWithEmail(email: String) {
            val data = getDefaultUserData()
            data["name"] = email.substringBefore("@")

            createNewUserDocument(email, data)
        }

        private fun createNewUserDocument(email: String, data: MutableMap<String, Any>) {
            println("DOCUMENT: ${email}, data: ${data}")
            val db = Firebase.firestore
            db.collection(Constants.COLLECTION_USERS)
                .document(email)
                .set(data)
        }
    }
}