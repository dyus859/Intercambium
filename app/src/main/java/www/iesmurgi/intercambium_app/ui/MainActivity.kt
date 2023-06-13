package www.iesmurgi.intercambium_app.ui

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import www.iesmurgi.intercambium_app.R
import www.iesmurgi.intercambium_app.databinding.ActivityMainBinding
import www.iesmurgi.intercambium_app.utils.DbUtils.Companion.toUser
import www.iesmurgi.intercambium_app.models.User
import www.iesmurgi.intercambium_app.utils.Constants
import www.iesmurgi.intercambium_app.utils.SharedData

/**
 * The main activity of the application.
 *
 * @author Denis Yushkin
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    /**
     * Called when the activity is starting.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being
     *     shut down, this Bundle contains the data it most recently supplied.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_chats,
                R.id.navigation_profile,
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        setFirebaseAuthListener()
    }

    override fun onStop() {
        super.onStop()
        setUserOnlineStatus()
    }

    /**
     * Sets the Firebase authentication state listener to listen for changes in the user's authentication state.
     */
    private fun setFirebaseAuthListener() {
        val auth = FirebaseAuth.getInstance()
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser

            if (user != null) {
                val email = user.email.toString()

                val db = Firebase.firestore
                val usersCollection = db.collection(Constants.COLLECTION_USERS)
                val userDocument = usersCollection.document(email)

                userDocument.get().addOnSuccessListener { document ->
                    val sharedUser = document.toUser()
                    SharedData.setUser(sharedUser)

                    FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                        userDocument.update(Constants.USERS_FIELD_FCM_TOKEN, token).addOnSuccessListener {
                            sharedUser.fcmToken = token
                        }
                    }
                }

                // Set online field to true when user signs in
                userDocument.update(Constants.USERS_FIELD_ONLINE, true)
            } else {
                setUserOnlineStatus()
                SharedData.setUser(User())
            }

            // Update the visibility of "Chats" menu item based on the current user status
            val navView: BottomNavigationView = binding.navView
            val menu = navView.menu
            menu.findItem(R.id.navigation_chats).isVisible = user != null
        }

        // Register the listener with FirebaseAuth
        auth.addAuthStateListener(authStateListener)
    }

    private fun setUserOnlineStatus(email: String? = null) {
        val db = Firebase.firestore
        val usersCollection = db.collection(Constants.COLLECTION_USERS)
        var currentEmail = ""

        if (email == null) {
            val currentUser = SharedData.getUser().value
            if (currentUser != null && currentUser.email.isNotEmpty()) {
                currentEmail = currentUser.email
            }
        }

        if (currentEmail.isNotEmpty()) {
            usersCollection
                .document(currentEmail)
                .update(Constants.USERS_FIELD_ONLINE, false)
        }
    }
}