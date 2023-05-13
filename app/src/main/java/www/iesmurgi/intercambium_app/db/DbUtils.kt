package www.iesmurgi.intercambium_app.db

import com.firebase.ui.auth.IdpResponse
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class DbUtils {
    companion object {
        const val COLLECTION_USERS = "users"

        private fun getDefaultUserData(): HashMap<String, String> {
            return hashMapOf(
                "name" to "",
                "phone" to "",
            )
        }

        fun createNewUser(response: IdpResponse) {
            println(response)
            val db = Firebase.firestore
            val data = getDefaultUserData()

            db.collection(COLLECTION_USERS)
                .document(response.email.toString())
                .set(data)
        }
    }
}