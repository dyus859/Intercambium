package www.iesmurgi.intercambium_app.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import www.iesmurgi.intercambium_app.R
import www.iesmurgi.intercambium_app.databinding.ActivityAdBinding
import www.iesmurgi.intercambium_app.models.Ad
import www.iesmurgi.intercambium_app.models.User
import www.iesmurgi.intercambium_app.utils.Constants

class AdActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Return to the previous activity
        actionBar?.setDisplayHomeAsUpEnabled(true)

        getData()
        setListeners()
    }

    private fun getData() {
        val extras = intent.extras ?: return
        extras.getString("AD")?.let {
            binding.pbAdInfo.show()
            loadAd(it)
        }
    }

    private fun loadAd(id: String) {
        val db = Firebase.firestore

        db.collection(Constants.COLLECTION_ADS)
            .document(id)
            .get()
            .addOnSuccessListener { adDocument ->
                if (adDocument.exists()) {
                    val author = adDocument.getString(Constants.ADS_FIELD_AUTHOR).toString()

                    if (author.isNotEmpty()) {
                        val usersCollection = db.collection(Constants.COLLECTION_USERS)
                        val usersDocument = usersCollection.document(author)

                        usersDocument.get()
                            .addOnSuccessListener { userDocument ->
                                if (userDocument.exists()) {
                                    val userEmail = userDocument.id
                                    val userName =
                                        userDocument.getString(Constants.USERS_FIELD_NAME).toString()
                                    val userPhoneNumber =
                                        userDocument.getString(Constants.USERS_FIELD_PHONE_NUMBER).toString()
                                    val userPhotoUrl =
                                        userDocument.getString(Constants.USERS_FIELD_PHOTO_URL).toString()

                                    val adId = adDocument.id
                                    val adTitle =
                                        adDocument.getString(Constants.ADS_FIELD_TITLE).toString()
                                    val adDesc =
                                        adDocument.getString(Constants.ADS_FIELD_DESCRIPTION).toString()
                                    val adProvince =
                                        adDocument.getString(Constants.ADS_FIELD_PROVINCE).toString()
                                    val adStatus =
                                        adDocument.getString(Constants.ADS_FIELD_STATUS).toString()
                                    var adCreatedAt =
                                        adDocument.getTimestamp(Constants.ADS_FIELD_CREATED_AT)
                                    val adImgUrl =
                                        adDocument.getString(Constants.ADS_FIELD_IMAGE).toString()

                                    if (adCreatedAt == null) {
                                        adCreatedAt = Timestamp.now()
                                    }

                                    val user = User(userEmail, userName, userPhoneNumber, userPhotoUrl)
                                    val ad = Ad(adId, adTitle, adDesc, adProvince, adStatus, adCreatedAt,
                                        adImgUrl, user)

                                    handleSuccess(ad, user)
                                }
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

    private fun setListeners() {
        val openChatBtn = binding.btnOpenChatAd
        openChatBtn.setOnClickListener {
            // TODO: Open chat functionality
        }
    }

    private fun handleSuccess(ad: Ad, user: User) {
        // Hide ProgressBar
        binding.pbAdInfo.hide()

        // Render images
        binding.ivAdProvinceInfo.visibility = View.VISIBLE
        binding.sivItemAdUserPhotoInfo.visibility = View.VISIBLE

        // Set content
        binding.tvItemAdLocation.text = ad.province
        binding.tvItemAdTitleInfo.text = ad.title
        binding.tvItemAdDescriptionInfo.text = ad.description
        binding.tvItemAdUserNameInfo.text = user.name

        // Set ad img
        if (ad.imgUrl.isNotEmpty()) {
            Glide.with(this)
                .load(ad.imgUrl)
                .into(binding.ivItemAdImageInfo)
        } else {
            binding.ivItemAdImageInfo.setImageResource(R.drawable.no_image)
        }

        // Set user's profile picture
        if (ad.author.photoUrl.isNotEmpty()) {
            Glide.with(this)
                .load(ad.author.photoUrl)
                .into(binding.sivItemAdUserPhotoInfo)
        } else {
            binding.sivItemAdUserPhotoInfo.setImageResource(R.drawable.default_avatar)
        }
    }

    private fun handleFailure() {
        binding.pbAdInfo.hide()
        finish()

        val text = getString(R.string.ad_does_not_exist_anymore)
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }
}