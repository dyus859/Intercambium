package www.iesmurgi.intercambium_app.ui.profile

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import www.iesmurgi.intercambium_app.db.DbUtils

class ProfileViewModel() : ViewModel() {
//    private val _text = MutableLiveData<String>().apply {
//        value = "This is profile Fragment"
//    }
//    val text: LiveData<String> = _text

    // Initialize the LiveData with the current authentication state
//    val isAuthenticated = MutableLiveData<Boolean>().apply {
//        value = FirebaseAuth.getInstance().currentUser != null
//    }

}