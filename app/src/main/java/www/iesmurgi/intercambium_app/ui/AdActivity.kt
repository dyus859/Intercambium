package www.iesmurgi.intercambium_app.ui

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import www.iesmurgi.intercambium_app.R
import www.iesmurgi.intercambium_app.databinding.ActivityAdBinding
import www.iesmurgi.intercambium_app.databinding.DialogConfirmationBinding
import www.iesmurgi.intercambium_app.db.DbUtils.Companion.toAd
import www.iesmurgi.intercambium_app.db.DbUtils.Companion.toUser
import www.iesmurgi.intercambium_app.models.Ad
import www.iesmurgi.intercambium_app.utils.Constants
import www.iesmurgi.intercambium_app.utils.SharedData
import www.iesmurgi.intercambium_app.utils.Utils

/**
 * Activity class for displaying and managing an advertisement.
 * This activity is responsible for displaying the details of an advertisement,
 * including the ad title, description, location, user information, and images.
 * It also provides actions for publishing, hiding, editing, and deleting the ad,
 * depending on the user's privileges.
 *
 * @property binding The view binding object for the activity layout.
 *
 * @author Denis Yushkin
 */
class AdActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdBinding
    private lateinit var activityLauncher: ActivityResultLauncher<Intent>
    private var adId: String = ""
    private var adImgUrl: String = ""

    /**
     * Called when the activity is starting.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being
     *     shut down, this Bundle contains the data it most recently supplied.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Return to the previous activity
        actionBar?.setDisplayHomeAsUpEnabled(true)

        activityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data

                if (data != null) {
                    val ad = data.getSerializableExtra("AD") as Ad

                    val msg = getString(R.string.ad_successfully_edited)
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

                    // Update the values
                    handleSuccess(ad)
                }
            }
        }

        getData()
        setListeners()
    }

    /**
     * Retrieves the ID of the ad from the previous activity and loads the ad information.
     */
    private fun getData() {
        val extras = intent.extras ?: return
        extras.getString("AD")?.let {
            binding.pbAdInfo.show()
            loadAd(it)
        }
    }

    /**
     * Loads the ad information from the Firebase Firestore database and displays it in the activity.
     *
     * @param id The ID of the ad to load.
     */
    private fun loadAd(id: String) {
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
        binding.pbAdInfo.hide()
        finish()

        val text = getString(R.string.ad_could_not_be_loaded)
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    /**
     * Handles the case when the ad was successfully loaded.
     * Displays the ad information and user details in the activity.
     *
     * @param ad The loaded [Ad] object.
     */
    private fun handleSuccess(ad: Ad) {
        // Hide ProgressBar
        binding.pbAdInfo.hide()

        adId = ad.id
        adImgUrl = ad.imgUrl

        with(binding) {
            // Render images
            ivAdProvinceInfo.visibility = View.VISIBLE
            sivItemAdUserPhotoInfo.visibility = View.VISIBLE

            // Set actions layout visibility
            handleActionsVisibility(ad.status)

            // Set content
            tvItemAdLocation.text = ad.province
            tvItemAdTitleInfo.text = ad.title
            tvItemAdDescriptionInfo.text = ad.description
            tvItemAdUserNameInfo.text = ad.author.name

            // Set chat button visibility
            val currentUser = SharedData.getUser().value!!
            btnOpenChatAd.visibility = if (ad.author.email == currentUser.email) View.GONE else View.VISIBLE

            // Set ad image
            Glide.with(this@AdActivity)
                .load(ad.imgUrl.takeIf { it.isNotEmpty() })
                .placeholder(R.drawable.no_image)
                .into(ivItemAdImageInfo)

            // Set user's profile picture
            Glide.with(this@AdActivity)
                .load(ad.author.photoUrl.takeIf { it.isNotEmpty() })
                .placeholder(R.drawable.default_avatar)
                .into(sivItemAdUserPhotoInfo)
        }
    }

    /**
     * Sets the visibility of the actions LinearLayout based on the user's privileges.
     */
    private fun handleActionsVisibility(status: String) {
        val user = SharedData.getUser().value!!

        with(binding) {
            if (user.administrator) {
                // Set layout visible
                llActionsAd.visibility = View.VISIBLE

                // Handle buttons visibility which depends on the current ad status
                btnPublishAd.visibility = if (status == Constants.AD_STATUS_IN_REVISION) View.VISIBLE else View.GONE
                btnHideAd.visibility = if (status == Constants.AD_STATUS_PUBLISHED) View.VISIBLE else View.GONE

                // Edit and Delete are always visible
                btnEditAd.visibility = View.VISIBLE
                btnDeleteAd.visibility = View.VISIBLE
            } else {
                // Not an administrator user cannot see the action buttons
                llActionsAd.visibility = View.GONE
            }

            // Show 'Open Chat' button
            btnOpenChatAd.visibility = View.VISIBLE
        }
    }


    /**
     * Sets the click listeners for all buttons in the activity.
     */
    private fun setListeners() {
        with(binding) {
            btnPublishAd.setOnClickListener { onPublishClick() }
            btnHideAd.setOnClickListener { onHideClick() }
            btnEditAd.setOnClickListener { onEditClick() }
            btnDeleteAd.setOnClickListener { onDeleteClick() }
//            btnOpenChatAd.setOnClickListener { onOpenChatClick()}
        }
    }

    /**
     * Called when the user clicks on the 'Publish ad' button.
     */
    private fun onPublishClick() {
        if (adId.isEmpty()) {
            return
        }

        val title = getString(R.string.dialog_publish_ad)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_confirmation, null)
        val dialogConfirmationBinding = DialogConfirmationBinding.bind(view)

        val alertDialog = Utils.createAlertDialog(this, title, view)

        with(dialogConfirmationBinding) {
            btnCancelAction.setOnClickListener { alertDialog.dismiss() }
            btnConfirmAction.setOnClickListener { setNewAdStatus(adId, Constants.AD_STATUS_PUBLISHED) }
        }
    }

    /**
     * Called when the user clicks on the 'Hide ad' button.
     */
    private fun onHideClick() {
        if (adId.isEmpty()) {
            return
        }

        val title = getString(R.string.dialog_hide_ad)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_confirmation, null)
        val dialogConfirmationBinding = DialogConfirmationBinding.bind(view)

        val alertDialog = Utils.createAlertDialog(this, title, view)

        with(dialogConfirmationBinding) {
            btnCancelAction.setOnClickListener { alertDialog.dismiss() }
            btnConfirmAction.setOnClickListener { setNewAdStatus(adId, Constants.AD_STATUS_IN_REVISION) }
        }
    }

    /**
     * Called when the user clicks on the 'Edit ad' button.
     */
    private fun onEditClick() {
        if (adId.isNotEmpty()) {
            val intent = Intent(this, AddEditAdActivity::class.java)
            intent.putExtra("GOTO", "AdActivity")
            intent.putExtra("AD", adId)
            activityLauncher.launch(intent)
        }
    }

    /**
     * Called when the user clicks on the 'Delete ad' button.
     */
    private fun onDeleteClick() {
        if (adId.isEmpty()) {
            return
        }

        val title = getString(R.string.dialog_delete_ad)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_confirmation, null)
        val dialogConfirmationBinding = DialogConfirmationBinding.bind(view)

        val alertDialog = Utils.createAlertDialog(this, title, view)

        with(dialogConfirmationBinding) {
            btnCancelAction.setOnClickListener { alertDialog.dismiss() }
            btnConfirmAction.setOnClickListener { deleteAd(adId, adImgUrl) }
        }
    }

    /**
     * Sets a new status for the ad.
     *
     * @param id The ID of the ad to update.
     * @param status The new status for the ad.
     */
    private fun setNewAdStatus(id: String, status: String) {
        val db = Firebase.firestore
        val adsCollection = db.collection(Constants.COLLECTION_ADS)

        // Show ProgressBar
        binding.pbAdInfo.show()

        adsCollection.document(id)
            .update(Constants.ADS_FIELD_STATUS, status)
            .addOnCompleteListener { task ->
                val msg = if (task.isSuccessful) {
                    getString(R.string.status_successfully_updated)
                } else {
                    getString(R.string.error_operation_could_not_be_done)
                }

                // Hide ProgressBar
                binding.pbAdInfo.hide()

                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                handleActionsVisibility(status)
            }
    }

    private fun deleteAd(id: String, imgUrl: String) {
        val db = Firebase.firestore
        val adsCollection = db.collection(Constants.COLLECTION_ADS)

        // Show ProgressBar
        binding.pbAdInfo.show()

        println("imgUrl: $imgUrl")

        adsCollection.document(id)
            .delete()
            .addOnCompleteListener { task ->
                // Delete the Firebase Storage image associated to that ad
                Utils.deleteFirebaseImage(imgUrl)

                // Hide ProgressBar
                binding.pbAdInfo.hide()

                val msg = if (task.isSuccessful) {
                    getString(R.string.ad_successfully_deleted)
                } else {
                    getString(R.string.error_operation_could_not_be_done)
                }

                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                finish()
            }
    }
}