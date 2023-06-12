package www.iesmurgi.intercambium_app.ui.profile

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import www.iesmurgi.intercambium_app.R
import www.iesmurgi.intercambium_app.databinding.FragmentProfileBinding
import www.iesmurgi.intercambium_app.db.DbUtils
import www.iesmurgi.intercambium_app.ui.ConfigurationActivity
import www.iesmurgi.intercambium_app.ui.LoginEmailActivity
import www.iesmurgi.intercambium_app.ui.MyAdsActivity
import www.iesmurgi.intercambium_app.utils.Utils

/**
 * A fragment representing the Profile screen.
 * This fragment displays the user's profile information and provides options for authentication,
 *  such as signing in with email or Google. It also allows the user to navigate to other activities,
 *  such as [MyAdsActivity] and [ConfigurationActivity].
 *
 * @constructor Creates an instance of [ProfileFragment].
 *
 * @author Denis Yushkin
 */
class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private lateinit var activityLauncher: ActivityResultLauncher<Intent>
    private lateinit var signInGoogleLauncher: ActivityResultLauncher<Intent>

    private var showLogoutMessage: Boolean = false

    /**
     * Inflates the layout for the [ProfileFragment] and initializes UI components.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return The root View of the inflated layout for the fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setLaunchers()
        setListeners()

        // Enable options menu
        setHasOptionsMenu(true)

        return root
    }

    /**
     * Sets up the activity result launchers for various actions.
     */
    private fun setLaunchers() {
        // Register activity result launcher for general activity launch
        activityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Navigate back to the Profile Fragment
                val fragmentManager = requireActivity().supportFragmentManager
                val transaction = fragmentManager.beginTransaction()
                transaction.replace(R.id.navigation_profile, ProfileFragment())
                transaction.commit()
            }
        }

        // Register activity result launcher for Firebase AuthUI activity
        signInGoogleLauncher = registerForActivityResult(FirebaseAuthUIActivityResultContract()) { res ->
            onSignInResult(res)
        }
    }

    /**
     * Sets up the listeners for various buttons and actions in the Profile fragment.
     */
    private fun setListeners() {
        val auth = FirebaseAuth.getInstance()
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            setUserAuthenticated(firebaseAuth.currentUser != null)
        }

        // Register the listener with FirebaseAuth
        auth.addAuthStateListener(authStateListener)

        val signInEmailBtn: Button = binding.signInEmail
        signInEmailBtn.setOnClickListener {
            openLoginEmailActivity()
        }

        val signInGoogleBtn: Button = binding.signInGoogle
        signInGoogleBtn.setOnClickListener {
            signInWithGoogle(signInGoogleLauncher)
        }

        val myAdsBtn: Button = binding.btnMyAdsProfile
        myAdsBtn.setOnClickListener {
            openMyAdsActivity()
        }

        val configurationBtn: Button = binding.btnConfigurationProfile
        configurationBtn.setOnClickListener {
            openConfigurationActivity()
        }
    }

    /**
     * Sets the visibility of UI elements based on the user's authentication status.
     *
     * @param authenticated True if the user is authenticated, false otherwise.
     */
    private fun setUserAuthenticated(authenticated: Boolean) {
        val map = mutableMapOf(
            binding.signInEmail to false,
            binding.signInGoogle to false,
            binding.btnMyAdsProfile to true,
            binding.btnConfigurationProfile to true,
        )

        map.forEach { (key, value) ->
            if (value) {
                key.visibility = if (authenticated) View.VISIBLE else View.GONE
            } else {
                key.visibility = if (authenticated) View.GONE else View.VISIBLE
            }
        }

        // Update options menu, to show logout option
        if (authenticated) {
            activity?.invalidateOptionsMenu()
        } else if (showLogoutMessage) {
            showLogoutMessage = false
            Toast.makeText(activity, getString(R.string.signed_out), Toast.LENGTH_LONG).show()
            Utils.navigateToFragment(view, R.id.navigation_home)
        }
    }

    /**
     * Opens the [LoginEmailActivity] to allow the user to sign in with email.
     */
    private fun openLoginEmailActivity() {
        val intent = Intent(requireContext(), LoginEmailActivity::class.java)
        activityLauncher.launch(intent)
    }

    /**
     * Initiates the Google sign-in process.
     *
     * @param signInLauncher The [ActivityResultLauncher] used to launch the sign-in activity.
     */
    private fun signInWithGoogle(signInLauncher: ActivityResultLauncher<Intent>) {
        val providerGoogle = arrayListOf(AuthUI.IdpConfig.GoogleBuilder().build())

        val intent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providerGoogle)
            .build()
        signInLauncher.launch(intent)
    }

    /**
     * Handles the result of a sign-in attempt using FirebaseUI.
     *
     * @param result The result of the sign-in attempt.
     */
    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            // Successfully signed in
            val user = FirebaseAuth.getInstance().currentUser

            if (user != null) {
                val response = result.idpResponse
                // If it is a new account, insert data into the DB
                if ((response != null) && response.isNewUser) {
                    DbUtils.createNewUserWithGoogle(response)
                }
            }
        }
    }

    /**
     * Inflate the options menu for the Profile fragment.
     *
     * @param menu The Menu object to inflate.
     * @param inflater The MenuInflater object that can be used to inflate the menu.
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (FirebaseAuth.getInstance().currentUser != null) {
            inflater.inflate(R.menu.menu_sign_out, menu)
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    /**
     * Handles options menu item selections.
     *
     * @param item The selected [MenuItem] object.
     * @return True if the selection was handled, false otherwise.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.item_sign_out && FirebaseAuth.getInstance().currentUser != null) {
            showSignOutDialog()
        }

        return true
    }

    /**
     * Displays a dialog asking the user to confirm the sign-out action.
     */
    private fun showSignOutDialog() {
        val title = getString(R.string.menu_sign_out_title)
        Utils.createConfirmationAlertDialog(requireActivity(), title) {
            showLogoutMessage = true
            FirebaseAuth.getInstance().signOut()
        }
    }

    /**
     * Opens the [MyAdsActivity] to allow the user to view their ads.
     */
    private fun openMyAdsActivity() {
        val intent = Intent(requireContext(), MyAdsActivity::class.java)
        activityLauncher.launch(intent)
    }

    /**
     * Opens the [ConfigurationActivity] to allow the user to configure their settings.
     */
    private fun openConfigurationActivity() {
        val intent = Intent(requireContext(), ConfigurationActivity::class.java)
        activityLauncher.launch(intent)
    }
}