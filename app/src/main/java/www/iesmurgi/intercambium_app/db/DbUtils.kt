package www.iesmurgi.intercambium_app.db

import com.firebase.ui.auth.IdpResponse
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import www.iesmurgi.intercambium_app.utils.Constants

class DbUtils {
    companion object {
        private fun getDefaultUserData(): MutableMap<String, String> {
            return hashMapOf(
                "name" to "",
                "phoneNumber" to "",
                "photoUrl" to "",
            )
        }

        fun createNewUser(response: IdpResponse) {
            println(response)
            val db = Firebase.firestore
            val data = getDefaultUserData()
            data["name"] = response.user.name ?: ""
            data["photoNumber"] = response.phoneNumber ?: ""
            data["photoUrl"] = (response.user.photoUri ?: "") as String
            val email = response.email.toString()

            // If name is not specified, then take first part of the email
            if (data["name"]?.isEmpty() != false) {
                data["name"] = email.substringBefore("@")
            }

            db.collection(Constants.COLLECTION_USERS)
                .document(email)
                .set(data)
        }
    }
}