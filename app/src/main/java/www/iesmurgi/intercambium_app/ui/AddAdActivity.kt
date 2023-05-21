package www.iesmurgi.intercambium_app.ui

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NavUtils
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import www.iesmurgi.intercambium_app.R
import www.iesmurgi.intercambium_app.databinding.ActivityAddEditAdBinding
import www.iesmurgi.intercambium_app.databinding.EditImageBinding
import www.iesmurgi.intercambium_app.models.Ad
import www.iesmurgi.intercambium_app.models.User
import www.iesmurgi.intercambium_app.utils.Constants
import www.iesmurgi.intercambium_app.utils.SharedData
import www.iesmurgi.intercambium_app.utils.Utils

class AddAdActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddEditAdBinding

    private var latestImgUri: Uri? = null
    private val previewImage by lazy { binding.ivImageAdd }

    private val selectImageFromGalleryResult = registerForActivityResult(
        ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            latestImgUri = uri
            previewImage.setImageURI(null)
            previewImage.setImageURI(uri)
        }
    }

    private val takeImageResult = registerForActivityResult(
        ActivityResultContracts.TakePicture()) { isSuccess ->
        if (isSuccess) {
            latestImgUri?.let { uri ->
                previewImage.setImageURI(null)
                previewImage.setImageURI(uri)
            }
        }
    }

    // Stores if 'save' function can be used at the moment
    private var canSave = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditAdBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Return to the previous activity
        actionBar?.setDisplayHomeAsUpEnabled(true)

        setListeners()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_save, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.item_save)?.apply {
            setTitle("R.string.save")
            setIcon(R.drawable.ic_baseline_done_24)
        }

        return super.onPrepareOptionsMenu(menu)
    }

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

    private fun setListeners() {
        // When users click on 'Edit image'
        binding.ibEdit.setOnClickListener {
            editImageOnClick()
        }
    }

    private fun editImageOnClick() {
        val view = LayoutInflater.from(this).inflate(R.layout.edit_image, null)
        val editImageBinding = EditImageBinding.bind(view)

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

    private fun takeImage() {
        Utils.getTmpFileUri(this).let { uri ->
            latestImgUri = uri
            takeImageResult.launch(uri)
        }
    }

    private fun selectImageFromGallery() {
        selectImageFromGalleryResult.launch("image/*")
    }

    private fun onSaveClick() {
        val title = binding.tieAdTitle.text.toString().trim()

        if (title.length < Constants.MIN_TITLE_LENGTH) {
            binding.tieAdTitle.error = getString(R.string.error_title_min_length)
            binding.tieAdTitle.requestFocus()
            return
        }

        val description = binding.tieAdDescription.text.toString().trim()

        if (description.length < Constants.MIN_DESCRIPTION_LENGTH) {
            binding.tieAdDescription.error = getString(R.string.error_desc_min_length)
            binding.tieAdDescription.requestFocus()
            return
        }

        canSave = false

        val ad = Ad("", title, description)
        ad.author = User(SharedData.getUser().value!!)

        // If there is an image, first need to upload it
        if (latestImgUri != null) {
            val imgReference = Utils.getImgPath(this, latestImgUri!!)
            val storageReference = FirebaseStorage.getInstance().getReference(imgReference)

            storageReference.putFile(latestImgUri!!)
                .addOnFailureListener {
                    canSave = true
                }
                .addOnSuccessListener { taskSnapshot ->
                    taskSnapshot.storage.downloadUrl
                        .addOnFailureListener {
                            canSave = true
                        }
                        .addOnSuccessListener { uri ->
                            ad.imgUrl = uri.toString()
                            publishAd(ad)
                        }
                }
        } else {
            publishAd(ad)
        }
    }

    private fun publishAd(ad: Ad) {
        val db = Firebase.firestore
        val collection = db.collection(Constants.COLLECTION_ADS)
        val data = Utils.getAdData(ad)
        val document = collection.document()

        document.set(data)
            .addOnFailureListener {
                canSave = true
            }
            .addOnSuccessListener {
                ad.id = document.id
                finish()
            }
    }
}