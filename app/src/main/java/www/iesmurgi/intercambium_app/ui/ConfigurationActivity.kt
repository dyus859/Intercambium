package www.iesmurgi.intercambium_app.ui

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import www.iesmurgi.intercambium_app.R
import www.iesmurgi.intercambium_app.databinding.ActivityConfigurationBinding
import www.iesmurgi.intercambium_app.databinding.DialogEditImageBinding
import www.iesmurgi.intercambium_app.databinding.DialogEditNameBinding
import www.iesmurgi.intercambium_app.databinding.DialogEditPasswordBinding
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

        actionBar?.setDisplayHomeAsUpEnabled(true)
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
                uploadAndSetProfilePicture()
            }
        }

        takeImageResult = registerForActivityResult(
            ActivityResultContracts.TakePicture()) { isSuccess ->
            if (isSuccess) {
                latestImgUri?.let { _ ->
                    previewImage.setImageURI(null)
                    uploadAndSetProfilePicture()
                }
            }
        }
    }

    /**
     * Sets the listeners for the UI elements.
     */
    private fun setListeners() {
        with(binding) {
            btnEditProfilePicture.setOnClickListener { onEditProfilePictureClick() }
            btnEditPasswordConfiguration.setOnClickListener { onEditPasswordClick() }
            btnEditNameConfiguration.setOnClickListener { onEditNameClick() }
            btnDeleteAccount.setOnClickListener { onDeleteAccountClick() }
        }
    }


    /**
     * Fetches the user's data and updates the UI.
     */
    private fun fetchData() {
        with(binding) {
            tvEmailConfiguration.text = user.email
            tvNameConfiguration.text = user.name

            if (user.photoUrl.isNotEmpty()) {
                Glide.with(this@ConfigurationActivity)
                    .load(user.photoUrl)
                    .placeholder(R.drawable.default_avatar)
                    .into(ivProfilePicture)
            }
        }
    }

    /**
     * Handles the click event for the edit password button.
     */
    private fun onEditPasswordClick() {
        val title = getString(R.string.dialog_edit_password_title)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_password, null)
        val editPasswordBinding = DialogEditPasswordBinding.bind(view)

        val alertDialog = Utils.createAlertDialog(this, title, view)

        with(editPasswordBinding) {
            btnCancelPassword.setOnClickListener { alertDialog.dismiss() }
            btnApplyPassword.setOnClickListener {
                val currentPassword = tieUserCurrentPassword.text.toString()
                val newPassword = tieUserNewPassword.text.toString()
                val required = getString(R.string.required)

                if (currentPassword.isEmpty()) {
                    tieUserCurrentPassword.error = required
                    tieUserCurrentPassword.requestFocus()
                } else if (newPassword.isEmpty()) {
                    tieUserNewPassword.error = required
                    tieUserNewPassword.requestFocus()
                } else {
                    validateCurrentPassword(currentPassword) { isValid ->
                        if (!isValid) {
                            val msg = getString(R.string.error_edit_password_invalid_current_password)
                            tieUserCurrentPassword.error = msg
                            tieUserCurrentPassword.requestFocus()
                        } else {
                            updatePassword(newPassword)
                            alertDialog.dismiss()
                        }
                    }
                }
            }
        }
    }

    /**
     * Validates the provided current password.
     *
     * @param currentPassword The current password to be validated.
     * @param callback The callback function to be invoked with the validation result.
     *                 It receives a boolean value indicating whether the current password is valid or not.
     */
    private fun validateCurrentPassword(currentPassword: String, callback: (isValid: Boolean) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val credentials = EmailAuthProvider.getCredential(user.email!!, currentPassword)
            user.reauthenticate(credentials)
                .addOnCompleteListener { task ->
                    callback(task.isSuccessful)
                }
        } else {
            callback(false)
        }
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
        val title = getString(R.string.dialog_edit_name_title)

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_name, null)
        val editNameBinding = DialogEditNameBinding.bind(view)

        val alertDialog = Utils.createAlertDialog(this, title, view)

        with(editNameBinding) {
            btnCancelName.setOnClickListener { alertDialog.dismiss() }
            btnApplyName.setOnClickListener {
                val text = tieUserName.text.toString().trim()
                val required = getString(R.string.required)

                if (text.isEmpty()) {
                    tieUserName.error = required
                    tieUserName.requestFocus()
                } else if (text.length < Constants.MIN_NAME_LENGTH) {
                    tieUserName.error = getString(R.string.error_name_too_short, Constants.MIN_NAME_LENGTH)
                    tieUserName.requestFocus()
                } else if (text.length > Constants.MAX_NAME_LENGTH) {
                    tieUserName.error = getString(R.string.error_name_too_long,
                        text.length, Constants.MAX_NAME_LENGTH)
                    tieUserName.requestFocus()
                } else {
                    updateValueDB(Constants.USERS_FIELD_NAME, text)
                    alertDialog.dismiss()
                }
            }
        }
    }


    /**
     * Updates the specified field in the Firestore database with the given value.
     *
     * @param field The field to update.
     * @param value The new value.
     */
    private fun updateValueDB(field: String, value: String) {
        val db = Firebase.firestore
        val usersCollections = db.collection(Constants.COLLECTION_USERS)
        val userDocument = usersCollections.document(user.email)

        userDocument.update(field, value).addOnSuccessListener {
            user.name = value
            fetchData()

            // Update nameSearch field
            val nameWords = value.lowercase().split(" ")
            userDocument.update(Constants.USERS_FIELD_NAME_SEARCH, nameWords)
        }
    }

    /**
     * Handles the click event for the 'Edit image' button.
     * This function displays a dialog with options to choose an image from the gallery or capture a
     *  new photo. When the user selects an option, the corresponding function ([selectImageFromGallery]
     *  or [takeImage]) is called, and the dialog is dismissed.
     */
    private fun onEditProfilePictureClick() {
        val title = getString(R.string.edit_image_title)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_image, null)
        val editImageBinding = DialogEditImageBinding.bind(view)

        val alertDialog = Utils.createAlertDialog(this, title, view)

        with(editImageBinding) {
            if (user.photoUrl.isNotEmpty()) {
                llDeletePhoto.visibility = View.VISIBLE
            } else {
                llDeletePhoto.visibility = View.GONE
            }

            // User has selected 'Gallery'
            tvChooseFromGallery.setOnClickListener {
                selectImageFromGallery()
                alertDialog.dismiss()
            }

            // User has selected 'Take a photo'
            tvTakeAPhoto.setOnClickListener {
                takeImage()
                alertDialog.dismiss()
            }

            // User has selected 'Delete photo'
            btnDeletePhotoUser.setOnClickListener {
                if (user.photoUrl.isNotEmpty()) {
                    Utils.deleteFirebaseImage(user.photoUrl)
                    user.photoUrl = ""
                    
                    val msg = getString(R.string.you_successfully_deleted_your_profile_picture)
                    Toast.makeText(this@ConfigurationActivity, msg, Toast.LENGTH_SHORT).show()
                }

                alertDialog.dismiss()
                previewImage.setImageResource(R.drawable.default_avatar)
            }
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
                            Utils.deleteFirebaseImage(user.photoUrl)
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
                previewImage.setImageURI(latestImgUri)
            }
    }

    /**
     * Handles the click event for the delete account button.
     */
    private fun onDeleteAccountClick() {
        val title = getString(R.string.dialog_delete_account_title)
        Utils.createConfirmationAlertDialog(this, title) {
            deleteAccount()
        }
    }

    /**
     * Deletes the user's account and associated data.
     */
    private fun deleteAccount() {
        // Delete Firebase Firestore information associated to the account
        val db = Firebase.firestore
        val usersCollections = db.collection(Constants.COLLECTION_USERS)
        val userDocument = usersCollections.document(user.email)

        // Delete ads associated with the user's email
        userDocument.delete().addOnSuccessListener {
            val adsCollection = db.collection(Constants.COLLECTION_ADS)
            val adsQuery = adsCollection.whereEqualTo(Constants.ADS_FIELD_AUTHOR, user.email)

            adsQuery.get().addOnSuccessListener { adsDocuments ->
                for (adDocument in adsDocuments) {
                    val adId = adDocument.id
                    val adImgUrl = adDocument.getString(Constants.ADS_FIELD_IMAGE).orEmpty()

                    // Delete each ad document
                    adsCollection.document(adId).delete()

                    // Delete each ad image
                    if (adImgUrl.isNotEmpty()) {
                        Utils.deleteFirebaseImage(adImgUrl)
                    }
                }
            }
        }

        // Delete Firebase Storage information associated to the account
        if (user.photoUrl.isNotEmpty()) {
            Utils.deleteFirebaseImage(user.photoUrl)
        }

        // Delete each chat related to the user
        Utils.deleteUserChats(user.uid)

        val firebaseAuthUser = FirebaseAuth.getInstance().currentUser

        firebaseAuthUser?.delete()?.addOnSuccessListener {
            val msg = getString(R.string.delete_account_success)
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

            signOut()
        }?.addOnFailureListener {
            val msg = getString(R.string.delete_account_failed)
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Signs out the user and finishes the activity.
     */
    private fun signOut() {
        FirebaseAuth.getInstance().signOut()
        finish()
    }
}