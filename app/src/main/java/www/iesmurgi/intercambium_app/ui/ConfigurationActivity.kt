package www.iesmurgi.intercambium_app.ui

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import www.iesmurgi.intercambium_app.R
import www.iesmurgi.intercambium_app.databinding.ActivityConfigurationBinding
import www.iesmurgi.intercambium_app.databinding.DialogEditImageBinding
import www.iesmurgi.intercambium_app.models.User
import www.iesmurgi.intercambium_app.utils.Constants
import www.iesmurgi.intercambium_app.utils.SharedData
import www.iesmurgi.intercambium_app.utils.Utils

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

    private var latestImgUri: Uri? = null
    private val previewImage by lazy { binding.ivProfilePicture }

    private lateinit var selectImageFromGalleryResult: ActivityResultLauncher<String>
    private lateinit var takeImageResult: ActivityResultLauncher<Uri>

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

        // Get user's data
        user = SharedData.getUser().value!!

        setLaunchers()
        setListeners()
        fetchData()
    }

    /**
     * Sets up the activity result launchers for selecting an image from the gallery and taking
     *  a picture.
     * The selected or captured image URI is stored in the [latestImgUri] property, and the preview
     *  image view is updated accordingly.
     */
    private fun setLaunchers() {
        selectImageFromGalleryResult = registerForActivityResult(
            ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                latestImgUri = uri
                previewImage.setImageURI(null)
                previewImage.setImageURI(uri)
                uploadAndSetProfilePicture()
            }
        }

        takeImageResult = registerForActivityResult(
            ActivityResultContracts.TakePicture()) { isSuccess ->
            if (isSuccess) {
                latestImgUri?.let { uri ->
                    previewImage.setImageURI(null)
                    previewImage.setImageURI(uri)
                    uploadAndSetProfilePicture()
                }
            }
        }
    }

    /**
     * Sets the listeners for the UI elements.
     */
    private fun setListeners() {
        binding.btnEditProfilePicture.setOnClickListener { onEditProfilePictureClick() }
        binding.btnEditPasswordConfiguration.setOnClickListener { onEditPasswordClick() }
        binding.btnEditNameConfiguration.setOnClickListener { onEditNameClick() }
        binding.btnEditAgeConfiguration.setOnClickListener { onEditAgeClick() }
        binding.btnDeleteAccount.setOnClickListener { onDeleteAccountClick() }
    }


    /**
     * Fetches the user's data and updates the UI.
     */
    private fun fetchData() {
        val notSet = getString(R.string.value_not_set_configuration)

        if (user.photoUrl.isNotEmpty()) {
            Glide.with(this)
                .load(user.photoUrl)
                .into(binding.ivProfilePicture)
        }

        binding.tvEmailConfiguration.text = getString(R.string.label_email_configuration, user.email)
        binding.tvPasswordConfiguration.text = getString(R.string.label_password_configuration)

        if (user.name.isNotEmpty()) {
            binding.tvNameConfiguration.text = getString(R.string.label_name_configuration, user.name)
        } else {
            binding.tvNameConfiguration.text = getString(R.string.label_name_configuration, notSet)
        }

        if (user.age != 0L) {
            binding.tvAgeConfiguration.text = getString(R.string.label_age_configuration, user.age.toString())
        } else {
            binding.tvAgeConfiguration.text = getString(R.string.label_age_configuration, notSet)
        }
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

    /**
     * Handles the click event for the 'Edit image' button.
     * This function displays a dialog with options to choose an image from the gallery or capture a
     *  new photo. When the user selects an option, the corresponding function ([selectImageFromGallery]
     *  or [takeImage]) is called, and the dialog is dismissed.
     */
    private fun onEditProfilePictureClick() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_image, null)
        val editImageBinding = DialogEditImageBinding.bind(view)

        val alertDialogBuilder = AlertDialog.Builder(this)
            .setTitle(getString(R.string.edit_image_title))
            .setNegativeButton(getString(android.R.string.cancel), null)
            .setView(view)

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()

        // User has selected 'Gallery'
        editImageBinding.tvChooseFromGallery.setOnClickListener {
            selectImageFromGallery()
            alertDialog.dismiss()
        }

        // User has selected 'Take a photo'
        editImageBinding.tvTakeAPhoto.setOnClickListener {
            takeImage()
            alertDialog.dismiss()
        }
    }

    /**
     * Launches the camera to capture a new photo.
     * This function uses the activity result contract to start the camera activity to capture a new
     *  photo. The result is handled in the [takeImageResult] callback.
     */
    private fun takeImage() {
        Utils.getTmpFileUri(this).let { uri ->
            latestImgUri = uri
            takeImageResult.launch(uri)
        }
    }

    /**
     * Launches the activity to select an image from the gallery.
     * This function uses the activity result contract to start an activity to select an image
     *  from the gallery. The result is handled in the [selectImageFromGalleryResult] callback.
     */
    private fun selectImageFromGallery() {
        selectImageFromGalleryResult.launch("image/*")
    }

    /**
     * Uploads the selected profile picture to Firebase Storage and sets the user's profile picture URL.
     * Shows a progress bar while the upload is in progress.
     * If the user already has a profile picture, the previous image is deleted from storage
     *  before setting the new URL.
     */
    private fun uploadAndSetProfilePicture() {
        // Show ProgressBar
        binding.pbConfiguration.visibility = View.VISIBLE
        binding.pbConfiguration.show()

        val directory = Constants.STORAGE_USERS_IMAGES_PATH
        val imgReference = Utils.getImgPath(this, latestImgUri!!, directory)
        val storageReference = FirebaseStorage.getInstance().getReference(imgReference)

        storageReference.putFile(latestImgUri!!)
            .addOnFailureListener {
                handleUpdatingPublishingFailure()
            }
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.downloadUrl
                    .addOnFailureListener {
                        handleUpdatingPublishingFailure()
                    }
                    .addOnSuccessListener { uri ->
                        if (user.photoUrl.isNotEmpty()) {
                            deletePreviousImage(user.photoUrl)
                        }

                        // Set new url
                        updateUserProfilePicture(uri.toString())

                        // Hide the ProgressBar
                        binding.pbConfiguration.hide()
                    }
            }
    }

    /**
     * Handles the failure case when updating or publishing the user's profile picture.
     * Hides the [androidx.core.widget.ContentLoadingProgressBar] and shows an error message.
     */
    private fun handleUpdatingPublishingFailure() {
        // Hide the ProgressBar
        binding.pbConfiguration.hide()

        val msg = getString(R.string.error_operation_could_not_be_done)
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    /**
     * Deletes the previous profile picture from Firebase Storage.
     *
     * @param fileName The file name or path of the previous image to be deleted.
     */
    private fun deletePreviousImage(fileName: String) {
        // Create a FirebaseStorage instance
        val storage = FirebaseStorage.getInstance()

        // Get the reference to the image file using the provided URL
        val imageRef = storage.getReferenceFromUrl(fileName)

        // Delete the image file
        imageRef.delete()
    }

    /**
     * Updates the user's profile picture URL in the Firestore database.
     *
     * @param url The URL of the updated profile picture.
     */
    private fun updateUserProfilePicture(url: String) {
        val db = Firebase.firestore
        val usersCollection = db.collection(Constants.COLLECTION_USERS)
        usersCollection.document(user.email)
            .update(Constants.USERS_FIELD_PHOTO_URL, url)
            .addOnSuccessListener {
                user.photoUrl = url
            }
    }
}