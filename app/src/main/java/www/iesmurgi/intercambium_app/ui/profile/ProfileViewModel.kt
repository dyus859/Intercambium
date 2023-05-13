package www.iesmurgi.intercambium_app.ui.profile

import android.app.Activity.RESULT_OK
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import www.iesmurgi.intercambium_app.db.DbUtils

class ProfileViewModel : ViewModel() {
//    private val _text = MutableLiveData<String>().apply {
//        value = "This is profile Fragment"
//    }
//    val text: LiveData<String> = _text

    // Initialize the LiveData with the current authentication state
    val isAuthenticated = MutableLiveData<Boolean>().apply {
        value = FirebaseAuth.getInstance().currentUser != null
    }

    fun signInWithGoogle(signInLauncher: ActivityResultLauncher<Intent>) {
        val providerGoogle = arrayListOf(AuthUI.IdpConfig.GoogleBuilder().build())

        val intent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providerGoogle)
            .build()
        signInLauncher.launch(intent)
    }

    fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        if (result.resultCode == RESULT_OK) {
            // Successfully signed in
            val user = FirebaseAuth.getInstance().currentUser

            if (user != null) {
                val response = result.idpResponse
                // If it is a new account, insert data into the DB
                if (response != null && response.isNewUser) {
                    DbUtils.createNewUser(response)
                }
            }

            isAuthenticated.value = true
        }
    }

    fun signOut() {
        FirebaseAuth.getInstance().signOut()
        isAuthenticated.value = false
    }
}