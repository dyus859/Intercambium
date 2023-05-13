package www.iesmurgi.intercambium_app.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.android.gms.common.SignInButton
import com.google.firebase.auth.FirebaseAuth
import www.iesmurgi.intercambium_app.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var _viewModel: ProfileViewModel? = null
    private val viewModel get() = _viewModel!!

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { res ->
        viewModel.onSignInResult(res)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        _viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        val root: View = binding.root

//        val textView: TextView = binding.textProfile
//        dashboardViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }
        createListeners()
        createObservers()

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun createListeners() {
        val signInButton: SignInButton = binding.signInBtnProfile
        signInButton.setOnClickListener {
            viewModel.signInWithGoogle(signInLauncher)
        }

        val btnSignOut: Button = binding.btnSignOut
        btnSignOut.setOnClickListener {
            viewModel.signOut()
        }
    }

    private fun createObservers() {
        viewModel.isAuthenticated.observe(viewLifecycleOwner) { isAuthenticated ->
            Log.d("ProfileFragment", "isAuthenticated observer called: $isAuthenticated")
            setUserAuthenticated(isAuthenticated)
        }
    }

    private fun setUserAuthenticated(authenticated: Boolean) {
        val map = mutableMapOf(
            binding.sivLogoProfile to false,
            binding.signInBtnProfile to false,
            binding.btnSignOut to true,
        )

        map.forEach { (key, value) ->
            if (value) {
                key.visibility = if (authenticated) View.VISIBLE else View.GONE
            } else {
                key.visibility = if (authenticated) View.GONE else View.VISIBLE
                println("key: $key, value: $value, authtenticated: $authenticated")
            }
        }
    }
}