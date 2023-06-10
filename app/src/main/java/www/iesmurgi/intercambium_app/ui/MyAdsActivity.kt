package www.iesmurgi.intercambium_app.ui

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import www.iesmurgi.intercambium_app.databinding.ActivityMyAdsBinding
import www.iesmurgi.intercambium_app.models.Ad
import www.iesmurgi.intercambium_app.models.User
import www.iesmurgi.intercambium_app.models.adapters.AdAdapter
import www.iesmurgi.intercambium_app.utils.Constants
import www.iesmurgi.intercambium_app.utils.SharedData
import www.iesmurgi.intercambium_app.utils.Utils

/**
 * Activity for displaying the user's ads.
 *
 * @author Denis Yushkin
 */
class MyAdsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMyAdsBinding
    private lateinit var adapter: AdAdapter
    private lateinit var user: User

    /**
     * Called when the activity is starting.
     *
     * @param savedInstanceState The saved instance state.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyAdsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Return to the previous activity
        actionBar?.setDisplayHomeAsUpEnabled(true)

        user = SharedData.getUser().value!!
        handleSwipeRefresh()
    }


    /**
     * Called when the activity is resumed.
     */
    override fun onResume() {
        super.onResume()
        recyclerView()
    }

    /**
     * Sets up the swipe refresh layout and its listener.
     */
    private fun handleSwipeRefresh() {
        val swipeRefreshLayout = binding.swipeRefreshLayoutMyAds
        swipeRefreshLayout.setOnRefreshListener {
            recyclerView()
        }
    }

    /**
     * Sets up the recycler view and loads the ads from the database.
     */
    private fun recyclerView() {
        if (!this::adapter.isInitialized) {
            adapter = AdAdapter(applicationContext) { onItemClick(it) }
            binding.rvAdsMyAds.adapter = adapter
            binding.rvAdsMyAds.layoutManager = LinearLayoutManager(applicationContext)
        } else {
            // Avoid "E/RecyclerView: No adapter attached; skipping layout"
            binding.rvAdsMyAds.adapter = adapter
            // Avoid "E/RecyclerView: No layout manager attached; skipping layout"
            binding.rvAdsMyAds.layoutManager = LinearLayoutManager(applicationContext)
        }

        loadAdsFromDB()
    }

    /**
     * Handles the click event on an ad item in the recycler view.
     *
     * @param ad The clicked [Ad].
     */
    private fun onItemClick(ad: Ad) {
        // If user is not authorized, open the profile fragment to authorize
        if (FirebaseAuth.getInstance().currentUser == null) {
            finishMyAdsActivity()
            return
        }

        val intent = Intent(applicationContext, AdActivity::class.java)
        intent.putExtra("AD", ad.id)
        startActivity(intent)
    }

    /**
     * Loads the ads from the database and updates the recycler view.
     */
    private fun loadAdsFromDB() {
        // Show ProgressBar
        binding.pbMyAds.show()

        val db = Firebase.firestore
        val adsCollection = db.collection(Constants.COLLECTION_ADS)
        adsCollection.orderBy(Constants.ADS_FIELD_CREATED_AT, Query.Direction.DESCENDING)
            .whereEqualTo(Constants.ADS_FIELD_AUTHOR, user.email)
            .get()
            .addOnFailureListener {
                handleNoAdsMsg()
            }
            .addOnSuccessListener { adDocuments ->
                if (adapter.adList.size != 0) {
                    // List had items, so first we need to remove them
                    // notify the adapter, and then clear the list

                    adapter.notifyItemRangeRemoved(0, adapter.adList.size)
                    adapter.adList.clear()
                }

                for (adDocument in adDocuments) {
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

                    val ad = Ad(adId, adTitle, adDesc, adProvince, adStatus, adCreatedAt, adImgUrl, user)
                    ad.visible = Utils.isAdVisibleForUser(ad)

                    // Add the add to the list
                    adapter.notifyItemInserted(adapter.adList.size)
                    adapter.adList.add(ad)
                }

                handleNoAdsMsg()
            }
    }

    /**
     * Handles the display of the "No ads" message based on the state of the ad list.
     */
    private fun handleNoAdsMsg() {
        // Hide refreshing animation
        binding.swipeRefreshLayoutMyAds.isRefreshing = false

        // Hide ProgressBar
        binding.pbMyAds.hide()

        if (adapter.adList.size == 0 || adapter.getVisibleAdsCount() == 0) {
            binding.tvNoAdsMyAds.visibility = View.VISIBLE
        } else {
            binding.tvNoAdsMyAds.visibility = View.GONE
        }
    }

    /**
     * Finishes the activity and sets the result as OK.
     */
    private fun finishMyAdsActivity() {
        val intent = Intent()
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}