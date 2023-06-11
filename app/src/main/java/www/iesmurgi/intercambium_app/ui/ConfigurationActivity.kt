package www.iesmurgi.intercambium_app.ui

import android.content.DialogInterface
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
                    val adImgUrl = adDocument.getString(Constants.ADS_FIELD_IMAGE)

                    // Delete each ad document
                    adsCollection.document(adId).delete()

                    // Delete each ad image
                    if (adImgUrl != null) {
                        Utils.deleteFirebaseImage(adImgUrl)
                    }
                }
            }
        }

        // Delete Firebase Storage information associated to the account
        if (user.photoUrl.isNotEmpty()) {
            Utils.deleteFirebaseImage(user.photoUrl)
        }

        // Delete Firebase Authentication information
        FirebaseAuth.getInstance().currentUser?.delete()?.addOnSuccessListener {
            signOut()
        }
    }

    /**
     * Signs out the user and finishes the activity.
     */
    private fun signOut() {
        FirebaseAuth.getInstance().signOut()
        finish()
    }

    /**
     * Creates and shows an [AlertDialog] with the specified title, view, and optional onSuccess callback.
     *
     * @param title The title of the [AlertDialog].
     * @param view The View to be displayed within the [AlertDialog].
     * @param onSuccess An optional callback function to be executed when the positive button is clicked.
     *                  It is invoked with no arguments.
     * @return The created [AlertDialog] instance.
     */
    private fun createAlertDialog(title: String, view: View, onSuccess: (() -> Unit)?): AlertDialog {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setView(view)
        alertDialogBuilder.setTitle(title)
        alertDialogBuilder.setIcon(R.mipmap.ic_launcher)
        alertDialogBuilder.setPositiveButton(getString(android.R.string.ok)) { _: DialogInterface, _: Int ->
            if (onSuccess != null) {
                onSuccess()
            }
        }
        alertDialogBuilder.setNegativeButton(getString(android.R.string.cancel), null)
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()

        return alertDialog
    }

    /**
     * Handles the click event for the edit password button.
     */
    private fun onEditPasswordClick() {
        val title = getString(R.string.dialog_edit_password_title)
        val etPassword = EditText(this).apply {
            hint = getString(R.string.password_hint)
            inputType = InputType.TYPE_CLASS_TEXT + InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        createAlertDialog(title, etPassword) {
            if (etPassword.text.isNotEmpty()) {
                updatePassword(etPassword.text.toString())
            }
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
        val etName = EditText(this).apply {
            hint = getString(R.string.dialog_edit_name_hint)
            isSingleLine = true
        }

        createAlertDialog(title, etName) {
            if (etName.text.isNotEmpty()) {
                updateValueDB(Constants.USERS_FIELD_NAME, etName.text.toString().trim())
            }
        }
    }

    /**
     * Handles the click event for the edit age button.
     */
    private fun onEditAgeClick() {
        val title = getString(R.string.dialog_edit_age)
        val npAge = NumberPicker(this)
        npAge.minValue = 1
        npAge.maxValue = 120

        if (user.age != 0L) {
            // Set current age for the NumberPicker
            npAge.value = Math.toIntExact(user.age!!)
        }

        createAlertDialog(title, npAge) {
            updateValueDB(Constants.USERS_FIELD_AGE, npAge.value.toLong())
        }
    }


    /**
     * Updates the specified field in the Firestore database with the given value.
     *
     * @param field The field to update.
     * @param value The new value.
     */
    private fun updateValueDB(field: String, value: Any) {
        val db = Firebase.firestore
        db.collection(Constants.COLLECTION_USERS)
            .document(user.email)
            .update(field, value)
            .addOnSuccessListener {
                if (field == Constants.USERS_FIELD_NAME) {
                    user.name = value as String
                } else if (field == Constants.USERS_FIELD_AGE) {
                    user.age = value as Long
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
        val title = getString(R.string.edit_image_title)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_image, null)
        val editImageBinding = DialogEditImageBinding.bind(view)

        val alertDialog = createAlertDialog(title, view, null)

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
            }
    }
}