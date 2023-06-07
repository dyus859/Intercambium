package www.iesmurgi.intercambium_app.ui.profile

import android.app.Activity
import android.app.AlertDialog
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
import www.iesmurgi.intercambium_app.databinding.DialogSignOutBinding
import www.iesmurgi.intercambium_app.databinding.FragmentProfileBinding
import www.iesmurgi.intercambium_app.db.DbUtils
import www.iesmurgi.intercambium_app.ui.LoginEmailActivity
import www.iesmurgi.intercambium_app.ui.MyAdsActivity
import www.iesmurgi.intercambium_app.utils.Utils

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private lateinit var activityLauncher: ActivityResultLauncher<Intent>

    private val signInGoogleLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { res ->
        onSignInResult(res)
    }

    private var showLogoutMessage: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        val root: View = binding.root

        activityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Navigate back to the Profile Fragment
                val fragmentManager = requireActivity().supportFragmentManager
                val transaction = fragmentManager.beginTransaction()
                transaction.replace(R.id.navigation_profile, ProfileFragment())
                transaction.commit()
            }
        }

        createListeners()

        // Enable options menu
        setHasOptionsMenu(true)

        return root
    }

    private fun createListeners() {
        FirebaseAuth.getInstance().addAuthStateListener {
            val authenticated = it.currentUser != null
            setUserAuthenticated(authenticated)
        }

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
    }

    private fun setUserAuthenticated(authenticated: Boolean) {
        val map = mutableMapOf(
            binding.sivLogoProfile to false,
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

    private fun openLoginEmailActivity() {
        val intent = Intent(requireContext(), LoginEmailActivity::class.java)
        activityLauncher.launch(intent)
    }

    private fun signInWithGoogle(signInLauncher: ActivityResultLauncher<Intent>) {
        val providerGoogle = arrayListOf(AuthUI.IdpConfig.GoogleBuilder().build())

        val intent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providerGoogle)
            .build()
        signInLauncher.launch(intent)
    }

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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (FirebaseAuth.getInstance().currentUser != null) {
            inflater.inflate(R.menu.menu_sign_out, menu)
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.item_sign_out && FirebaseAuth.getInstance().currentUser != null) {
            showSignOutDialog()
        }

        return true
    }

    private fun showSignOutDialog() {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_sign_out, null)
        val dialogSignOutBinding = DialogSignOutBinding.bind(view)

        val alertDialogBuilder = AlertDialog.Builder(activity)
            .setTitle(getString(R.string.menu_sign_out_title))
            .setNegativeButton(getString(android.R.string.cancel), null)
            .setView(view)

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()

        // When user clicks on 'Sign out'
        dialogSignOutBinding.btnSignOut.setOnClickListener {
            alertDialog.dismiss()

            showLogoutMessage = true
            FirebaseAuth.getInstance().signOut()
        }
    }

    // A method that is used to open a new activity (My Ads)
    private fun openMyAdsActivity() {
        val intent = Intent(requireContext(), MyAdsActivity::class.java)
        activityLauncher.launch(intent)
    }
}