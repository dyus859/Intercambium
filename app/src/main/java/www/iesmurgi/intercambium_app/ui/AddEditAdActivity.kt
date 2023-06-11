package www.iesmurgi.intercambium_app.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NavUtils
import com.bumptech.glide.Glide
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import www.iesmurgi.intercambium_app.R
import www.iesmurgi.intercambium_app.databinding.ActivityAddEditAdBinding
import www.iesmurgi.intercambium_app.databinding.DialogEditImageBinding
import www.iesmurgi.intercambium_app.db.DbUtils
import www.iesmurgi.intercambium_app.db.DbUtils.Companion.toAd
import www.iesmurgi.intercambium_app.db.DbUtils.Companion.toUser
import www.iesmurgi.intercambium_app.models.Ad
import www.iesmurgi.intercambium_app.models.Province
import www.iesmurgi.intercambium_app.models.User
import www.iesmurgi.intercambium_app.models.adapters.ProvinceAdapter
import www.iesmurgi.intercambium_app.utils.Constants
import www.iesmurgi.intercambium_app.utils.SharedData
import www.iesmurgi.intercambium_app.utils.Utils

/**
 * Represents the AddEditAdActivity class.
 * This activity is called when the user wants to add or edit an ad.
 *
 * @author Denis Yushkin
 */
class AddEditAdActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddEditAdBinding
    private lateinit var ad: Ad

    private var selectedProvinceName: String = ""
    private var latestImgUri: Uri? = null
    private val previewImage by lazy { binding.ivImageAdd }

    private lateinit var selectImageFromGalleryResult: ActivityResultLauncher<String>
    private lateinit var takeImageResult: ActivityResultLauncher<Uri>

    // Stores if 'save' function can be used at the moment
    private var canSave = true

    /**
     * Called when the activity is starting.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being
     *     shut down, this Bundle contains the data it most recently supplied.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditAdBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setLaunchers()
        setActionBarProps()
        setListeners()
        getData()
    }

    /**
     * Retrieves the intent to be used as the parent activity [Intent].
     *
     * @return The parent activity [Intent].
     */
    override fun getSupportParentActivityIntent(): Intent? {
        return getParentActivityIntentImplement()
    }

    /**
     * Retrieves the intent to be used as the parent activity [Intent].
     *
     * @return The parent activity [Intent].
     */
    override fun getParentActivityIntent(): Intent? {
        return getParentActivityIntentImplement()
    }


    /**
     * Retrieves the intent to be used as the parent activity [Intent].
     *
     * @return The parent activity [Intent].
     */
    private fun getParentActivityIntentImplement(): Intent? {
        val data = intent.extras
        var intent: Intent? = null

        if (data != null) {
            val goToIntent = data.getString("GOTO")

            if (goToIntent == "MainActivity") {
                intent = Intent(this, MainActivity::class.java)

                // Set flags to reuse the previous activity instead of creating a new activity instance.
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            } else if (goToIntent == "AdActivity") {
                intent = Intent(this, AdActivity::class.java)

                // Set flags to reuse the previous activity instead of creating a new activity instance.
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        }

        return intent
    }


    /**
     * Sets up the action bar properties.
     * This function sets up the title and display options for the action bar.
     */
    private fun setActionBarProps() {
        // Return to the previous activity
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Set ActionBar title
        supportActionBar?.title = if (!isEditing()) {
            getString(R.string.publish_ad)
        } else {
            getString(R.string.edit_ad)
        }
    }

    /**
     * Checks if the activity is in editing mode.
     *
     * @return true if the activity is in editing mode, false otherwise.
     */
    private fun isEditing(): Boolean {
        return this::ad.isInitialized
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
            }
        }

        takeImageResult = registerForActivityResult(
            ActivityResultContracts.TakePicture()) { isSuccess ->
            if (isSuccess) {
                latestImgUri?.let { uri ->
                    previewImage.setImageURI(null)
                    previewImage.setImageURI(uri)
                }
            }
        }
    }

    /**
     * Sets up the event listeners for the activity.
     */
    private fun setListeners() {
        // When a user clicks on 'Edit image'
        binding.btnEditImageAd.setOnClickListener {
            onEditImageClick()
        }

        binding.mactAdProvince.setAdapter(ProvinceAdapter(this, Province.provinceSource))
        // When a user clicks on a Province item
        binding.mactAdProvince.setOnItemClickListener { parent, _, position, _ ->
            val province = parent.adapter.getItem(position) as Province
            selectedProvinceName = province.name
        }
    }

    /**
     * Retrieves the ID of the ad from the previous activity and loads the ad information.
     */
    private fun getData() {
        val extras = intent.extras ?: return
        extras.getString("AD")?.let {
            binding.pbAddEditAd.show()
            loadAd(it)
        }
    }

    /**
     * Loads the ad information from the Firebase Firestore database and displays it in the activity.
     *
     * @param id The ID of the ad to load.
     */
    private fun loadAd(id: String) {
        // Show the ProgressBar
        binding.pbAddEditAd.visibility = View.VISIBLE
        binding.pbAddEditAd.show()

        val db = Firebase.firestore

        db.collection(Constants.COLLECTION_ADS)
            .document(id)
            .get()
            .addOnSuccessListener { adDocument ->
                if (adDocument.exists()) {
                    val author = adDocument.getString(Constants.ADS_FIELD_AUTHOR).toString()

                    if (author.isNotEmpty()) {
                        db.collection(Constants.COLLECTION_USERS)
                            .document(author)
                            .get()
                            .addOnSuccessListener { userDocument ->
                                if (userDocument.exists()) {
                                    val user = userDocument.toUser()
                                    val ad = adDocument.toAd(user)
                                    handleSuccess(ad)
                                } else {
                                    handleFailure()
                                }
                            }
                            .addOnFailureListener {
                                handleFailure()
                            }
                    } else {
                        handleFailure()
                    }
                } else {
                    handleFailure()
                }
            }
            .addOnFailureListener {
                handleFailure()
            }
    }

    /**
     * Handles the case when the ad failed to load or does not exist anymore.
     * Displays an error message and finishes the activity.
     */
    private fun handleFailure() {
        // Hide the ProgressBar
        binding.pbAddEditAd.hide()

        val text = getString(R.string.ad_could_not_be_loaded)
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

        finish()
    }

    /**
     * Handles the case when the ad was successfully loaded.
     * Displays the ad information and user details in the activity.
     *
     * @param ad The loaded [Ad] object.
     */
    private fun handleSuccess(ad: Ad) {
        // Hide the ProgressBar
        binding.pbAddEditAd.hide()

        // Set content
        binding.tieAdTitle.setText(ad.title)
        binding.tieAdDescription.setText(ad.description)

        // Load ad image
        if (ad.imgUrl.isNotEmpty()) {
            Glide.with(this)
                .load(ad.imgUrl)
                .into(binding.ivImageAdd)
        }

        // Set the province name
        selectedProvinceName = ad.province
        binding.mactAdProvince.setText(ad.province, false)
        binding.mactAdProvince.setSelection(binding.mactAdProvince.text.length)
    }

    /**
     * Initializes the options menu for the activity.
     * This function inflates the menu layout and sets the title and icon for the save menu item.
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_save, menu)
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Prepares the options menu to be displayed.
     * This function sets the title and icon for the save menu item.
     */
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.item_save)?.apply {
            setTitle(R.string.save)
            setIcon(R.drawable.ic_baseline_done_24)
        }

        return super.onPrepareOptionsMenu(menu)
    }

    /**
     * Handles the selection of an options menu item.
     * This function handles the click event for the home and save menu items. When the home menu
     *  item is selected, the activity navigates up to the parent activity. When the save menu item
     *  is selected, the [onSaveClick] function is called to save the advertisement.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> NavUtils.navigateUpFromSameTask(this)
            R.id.item_save -> {
                // Prevent user from spamming (multiple insertions)
                if (canSave) {
                    onSaveClick()
                }
            }
        }

        return true
    }

    /**
     * Handles the click event for the 'Edit image' button.
     * This function displays a dialog with options to choose an image from the gallery or capture a
     *  new photo. When the user selects an option, the corresponding function ([selectImageFromGallery]
     *  or [takeImage]) is called, and the dialog is dismissed.
     */
    private fun onEditImageClick() {
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
     * Validates the input data for the ad title, description, province and image.
     *
     * @param title The title of the ad.
     * @param description The description of the ad.
     * @return `true` if the input data is valid, `false` otherwise.
     */
    private fun validInputData(title: String, description: String): Boolean {
        if (title.length < Constants.MIN_TITLE_LENGTH) {
            binding.tieAdTitle.error = getString(R.string.error_title_min_length,
                Constants.MIN_TITLE_LENGTH)
            binding.tieAdTitle.requestFocus()
            return false
        }

        if (title.length > Constants.MAX_TITLE_LENGTH) {
            binding.tieAdTitle.error = getString(R.string.error_title_max_length,
                title.length,
                Constants.MAX_TITLE_LENGTH)
            binding.tieAdTitle.requestFocus()
            return false
        }

        if (description.length < Constants.MIN_DESCRIPTION_LENGTH) {
            binding.tieAdDescription.error = getString(R.string.error_desc_min_length,
                Constants.MIN_DESCRIPTION_LENGTH)
            binding.tieAdDescription.requestFocus()
            return false
        }

        if (description.length > Constants.MAX_DESCRIPTION_LENGTH) {
            binding.tieAdDescription.error = getString(R.string.error_desc_max_length,
                description.length,
                Constants.MAX_DESCRIPTION_LENGTH)
            binding.tieAdDescription.requestFocus()
            return false
        }

        // User hasn't selected any province
        if (selectedProvinceName.isEmpty()) {
            binding.mactAdProvince.error = getString(R.string.required)
            binding.mactAdProvince.requestFocus()
            return false
        }

        // The selected value doesn't match with the current value in the AutoCompleteTextView
        if (selectedProvinceName != binding.mactAdProvince.text.toString()) {
            binding.mactAdProvince.error = getString(R.string.error_invalid_province_name)
            binding.mactAdProvince.requestFocus()
            return false
        }

        // Image is required
        if (binding.ivImageAdd.drawable == null) {
            val msg = getString(R.string.image_missing)
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

            return false
        }

        return true
    }

    /**
     * Handles the click event for saving the ad data.
     * Validates the input fields and updates or publishes the ad.
     */
    private fun onSaveClick() {
        val title = binding.tieAdTitle.text.toString().trim()
        val description = binding.tieAdDescription.text.toString().trim()

        if (!validInputData(title, description)) {
            // Something is invalid, cannot save
            return
        }

        // Disable saving option for now
        canSave = false

        val tempAdId = if (isEditing()) {
            ad.id
        } else {
            ""
        }

        val tempAd = Ad(tempAdId, title, description, selectedProvinceName)
        tempAd.author = User(SharedData.getUser().value!!)

        if (isEditing()) {
            tempAd.imgUrl = ad.imgUrl
        }

        // Show ProgressBar
        binding.pbAddEditAd.show()

        if (latestImgUri == null) {
            // Image is not updated, just update the other information
            updatePublishAd(tempAd)
        } else {
            // Image is updated, need to upload the image first

            val directory = Constants.STORAGE_ADS_IMAGES_PATH
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
                            tempAd.imgUrl = uri.toString()
                            updatePublishAd(tempAd)
                        }
                }
        }
    }

    /**
     * Updates or publishes the ad data to the Firebase Firestore database.
     *
     * @param ad The [Ad] object to be updated or published.
     */
    private fun updatePublishAd(ad: Ad) {
        val db = Firebase.firestore
        val collection = db.collection(Constants.COLLECTION_ADS)
        val data = DbUtils.getAdData(ad)

        val document = if (!isEditing()) {
            collection.document()
        } else {
            collection.document(ad.id)
        }

        document.set(data)
            .addOnFailureListener {
                handleUpdatingPublishingFailure()
            }
            .addOnSuccessListener {
                ad.id = document.id
                finishActivity(ad)
            }
    }

    /**
     * Handles the failure case when updating or publishing the ad data.
     * Hides the [androidx.core.widget.ContentLoadingProgressBar], allows to try again, and shows an error message.
     */
    private fun handleUpdatingPublishingFailure() {
        // Hide the ProgressBar
        binding.pbAddEditAd.hide()

        // Allow to try again
        canSave = true

        val msg = getString(R.string.error_operation_could_not_be_done)
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    /**
     * Finishes the activity and sets the result with the [Ad] data.
     *
     * @param ad The [Ad] object to be sent as the result.
     */
    private fun finishActivity(ad: Ad) {
        // Hide the ProgressBar
        binding.pbAddEditAd.hide()

        val intent = Intent()
        intent.putExtra("AD", ad)
        setResult(Activity.RESULT_OK, intent)

        finish()
    }
}