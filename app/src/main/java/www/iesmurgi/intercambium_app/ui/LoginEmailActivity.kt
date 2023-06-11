package www.iesmurgi.intercambium_app.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import www.iesmurgi.intercambium_app.R
import www.iesmurgi.intercambium_app.databinding.ActivityLoginEmailBinding
import www.iesmurgi.intercambium_app.db.DbUtils
import www.iesmurgi.intercambium_app.utils.Constants

/**
 * Activity for email-based login and registration.
 *
 * @author Denis Yushkin
 */
class LoginEmailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginEmailBinding
    private var modeSignIn = true

    /**
     * Called when the activity is starting.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being
     *     shut down, this Bundle contains the data it most recently supplied.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Return to the previous activity
        actionBar?.setDisplayHomeAsUpEnabled(true)

        // Set ActionBar title
        supportActionBar?.title = if (modeSignIn) {
            getString(R.string.sign_in)
        } else {
            getString(R.string.sign_up)
        }

        setListeners()
    }

    /**
     * Sets the click listeners for the buttons and text view.
     */
    private fun setListeners() {
        val signInButton = binding.btnSignInLogin
        signInButton.setOnClickListener {
            tryActionButton(true)
        }

        val signUpButton = binding.btnSignUpLogin
        signUpButton.setOnClickListener {
            tryActionButton(false)
        }

        val forgotYourPassword = binding.tvForgotYourPasswordLogin
        forgotYourPassword.setOnClickListener {
            tryResetPassword()
        }
    }

    /**
     * Validates the email and password inputs and performs the sign-in or sign-up action accordingly.
     *
     * @param signIn True if signing in, false if signing up.
     */
    private fun tryActionButton(signIn: Boolean) {
        val email = binding.tieEmailLogin.text.toString()
        val password = binding.tiePasswordLogin.text.toString()
        val required = getString(R.string.required)
        val emailPattern = "[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}".toRegex()

        if (email.isEmpty()) {
            binding.tieEmailLogin.error = required
        } else if (!email.matches(emailPattern)) {
            val invalidEmailMsg = getString(R.string.auth_invalid_email_pattern)
            binding.tieEmailLogin.error = invalidEmailMsg
        } else if (password.isEmpty()) {
            binding.tiePasswordLogin.error = required
        } else {
            if (signIn) {
                signInWithEmailAndPassword(email, password)
            } else {
                if (password.length < Constants.MIN_PASSWORD_LENGTH) {
                    val weakPasswordMsg = getString(R.string.auth_weak_password_exception)
                    binding.tiePasswordLogin.error = weakPasswordMsg
                } else {
                    createUserWithEmailAndPassword(email, password)
                }
            }
        }
    }

    /**
     * Performs sign-in with the provided email and password.
     *
     * @param email The user's email.
     * @param password The user's password.
     */
    private fun signInWithEmailAndPassword(email: String, password: String) {
        FirebaseAuth.getInstance()
            .signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                val message = if (task.isSuccessful) {
                    getString(R.string.sign_in_successful)
                } else {
                    getString(R.string.sign_in_unsuccessful)
                }

                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

                if (task.isSuccessful) {
                    finishLoginActivity()
                }
            }
    }

    /**
     * Creates a new user account with the provided email and password.
     *
     * @param email The user's email.
     * @param password The user's password.
     */
    private fun createUserWithEmailAndPassword(email: String, password: String) {
        FirebaseAuth.getInstance()
            .createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                var errorMessage: String? = null

                try {
                    if (task.exception != null) {
                        throw task.exception!!
                    }
                } catch (_: FirebaseAuthUserCollisionException) {
                    errorMessage = getString(R.string.auth_user_collision_exception)
                } finally {
                    if (errorMessage != null) {
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }

                if (task.isSuccessful) {
                    DbUtils.createNewUserWithEmail(email)
                    finishLoginActivity()
                }
            }
    }

    /**
     * Sends a password reset email to the user's email address.
     */
    private fun tryResetPassword() {
        val email = binding.tieEmailLogin.text.toString()
        val required = getString(R.string.required)
        val emailPattern = "[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}".toRegex()

        if (email.isEmpty()) {
            binding.tieEmailLogin.error = required
        } else if (!email.matches(emailPattern)) {
            val invalidEmailMsg = getString(R.string.auth_invalid_email_pattern)
            binding.tieEmailLogin.error = invalidEmailMsg
        } else {
            FirebaseAuth.getInstance().sendPasswordResetEmail(email)

            val text = getString(R.string.reset_password_link_sent)
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Finishes the [LoginEmailActivity] and returns to the previous activity.
     */
    private fun finishLoginActivity() {
        finish()
    }
}