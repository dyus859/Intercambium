package www.iesmurgi.intercambium_app.ui

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import www.iesmurgi.intercambium_app.R
import www.iesmurgi.intercambium_app.databinding.ActivityConfigurationBinding
import www.iesmurgi.intercambium_app.models.User
import www.iesmurgi.intercambium_app.utils.Constants
import www.iesmurgi.intercambium_app.utils.SharedData

/**
 * Activity for user configuration settings.
 *
 * This activity allows the user to configure their account settings, such as email, password,
 * name, and age. The user can edit these settings, delete their account, or sign out.
 *
 * @author Denis Yushkin
 */
class ConfigurationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConfigurationBinding
    private lateinit var user: User

    /**
     * Called when the activity is starting.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being
     *     shut down, this Bundle contains the data it most recently supplied.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigurationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Return to the previous activity
        actionBar?.setDisplayHomeAsUpEnabled(true)

        user = SharedData.getUser().value!!

        fetchData()

        setListeners()
    }

    /**
     * Fetches the user's data and updates the UI.
     */
    private fun fetchData() {
        val notSet = getString(R.string.value_not_set_configuration)

        binding.tvEmailConfiguration.text = getString(R.string.label_email_configuration, user.email)
        binding.tvPasswordConfiguration.text = getString(R.string.label_password_configuration)

        if (user.name.isNotEmpty()) {
            binding.tvNameConfiguration.text = getString(R.string.label_name_configuration, user.name)
        } else {
            binding.tvNameConfiguration.text = getString(R.string.label_name_configuration, notSet)
        }

        if (user.age != null) {
            binding.tvAgeConfiguration.text = getString(R.string.label_age_configuration, user.age.toString())
        } else {
            binding.tvAgeConfiguration.text = getString(R.string.label_age_configuration, notSet)
        }
    }

    /**
     * Sets the listeners for the UI elements.
     */
    private fun setListeners() {
        binding.btnEditPasswordConfiguration.setOnClickListener { onEditPasswordClick() }
        binding.btnEditNameConfiguration.setOnClickListener { onEditNameClick() }
        binding.btnEditAgeConfiguration.setOnClickListener { onEditAgeClick() }
        binding.btnDeleteAccount.setOnClickListener { onDeleteAccountClick() }
    }

    /**
     * Handles the click event for the delete account button.
     */
    private fun onDeleteAccountClick() {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(getString(R.string.dialog_delete_account_title))
        alertDialogBuilder.setIcon(R.mipmap.ic_launcher)
        alertDialogBuilder.setPositiveButton(getString(android.R.string.ok)) { _: DialogInterface, _: Int ->
            deleteAccount()
        }
        alertDialogBuilder.setNegativeButton(getString(android.R.string.cancel), null)
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }


    /**
     * Deletes the user's account and associated data.
     */
    private fun deleteAccount() {
        // Delete associated data to the email
        val db = Firebase.firestore
        db.collection(Constants.COLLECTION_USERS)
            .document(user.email)
            .delete()

        // Delete the account itself. On success, user will be signed out
        FirebaseAuth.getInstance().currentUser?.delete()?.addOnSuccessListener {
            signOut()
        }
    }

    /**
     * Signs out the user and finishes the activity.
     */
    private fun signOut() {
        val auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            auth.signOut()
        }

        val intent = Intent()
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    /**
     * Handles the click event for the edit password button.
     */
    private fun onEditPasswordClick() {
        val etPassword = EditText(this)
        etPassword.hint = getString(R.string.password_hint)
        etPassword.inputType = InputType.TYPE_CLASS_TEXT + InputType.TYPE_TEXT_VARIATION_PASSWORD

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setView(etPassword)
        alertDialogBuilder.setTitle(getString(R.string.dialog_edit_password_title))
        alertDialogBuilder.setIcon(R.mipmap.ic_launcher)
        alertDialogBuilder.setPositiveButton(getString(android.R.string.ok)) { _: DialogInterface, _: Int ->
            if (etPassword.text.isNotEmpty()) {
                updatePassword(etPassword.text.toString())
            }
        }
        alertDialogBuilder.setNegativeButton(getString(android.R.string.cancel), null)
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    /**
     * Updates the user's password.
     *
     * @param password The new password.
     */
    private fun updatePassword(password: String) {
        val msg: String = if (password.length >= Constants.MIN_PASSWORD_LENGTH) {
            FirebaseAuth.getInstance().currentUser?.updatePassword(password)
            getString(R.string.password_changed_successfully)
        } else {
            getString(R.string.auth_weak_password_exception)
        }

        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }


    /**
     * Handles the click event for the edit name button.
     */
    private fun onEditNameClick() {
        val etName = EditText(this)
        etName.hint = getString(R.string.dialog_edit_name_hint)
        etName.isSingleLine = true

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setView(etName)
        alertDialogBuilder.setTitle(getString(R.string.dialog_edit_name_title))
        alertDialogBuilder.setIcon(R.mipmap.ic_launcher)
        alertDialogBuilder.setPositiveButton(getString(android.R.string.ok)) { _: DialogInterface, _: Int ->
            if (etName.text.isNotEmpty()) {
                updateValueDB(Constants.USERS_FIELD_NAME, etName.text.toString().trim())
            }
        }
        alertDialogBuilder.setNegativeButton(getString(android.R.string.cancel), null)
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }


    /**
     * Handles the click event for the edit age button.
     */
    private fun onEditAgeClick() {
        val npAge = NumberPicker(this)
        npAge.minValue = 1
        npAge.maxValue = 120

        if (user.age != null) {
            // Set current age for the NumberPicker
            npAge.value = Math.toIntExact(user.age!!)
        }

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setView(npAge)
        alertDialogBuilder.setTitle(getString(R.string.dialog_edit_age))
        alertDialogBuilder.setIcon(R.mipmap.ic_launcher)
        alertDialogBuilder.setPositiveButton(getString(android.R.string.ok)) { _: DialogInterface, _: Int ->
            updateValueDB(Constants.USERS_FIELD_AGE, npAge.value.toLong())
        }
        alertDialogBuilder.setNegativeButton(getString(android.R.string.cancel), null)
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    /**
     * Updates the specified field in the Firestore database with the given value.
     *
     * @param field The field to update.
     * @param value The new value.
     */
    private fun updateValueDB(field: String, value: String) {
        val db = Firebase.firestore
        db.collection(Constants.COLLECTION_USERS)
            .document(user.email)
            .update(field, value)
            .addOnSuccessListener {
                if (field == Constants.USERS_FIELD_NAME) {
                    user.name = value
                }

                // Update activity values
                fetchData()
            }
    }

    /**
     * Updates the specified field in the Firestore database with the given value.
     *
     * @param field The field to update.
     * @param value The new value.
     */
    private fun updateValueDB(field: String, value: Long) {
        val db = Firebase.firestore
        db.collection(Constants.COLLECTION_USERS)
            .document(user.email)
            .update(field, value)
            .addOnSuccessListener {
                if (field == Constants.USERS_FIELD_AGE) {
                    user.age = value
                }

                // Update activity values
                fetchData()
            }
    }
}