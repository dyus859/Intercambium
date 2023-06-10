package www.iesmurgi.intercambium_app.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import www.iesmurgi.intercambium_app.R
import www.iesmurgi.intercambium_app.databinding.FragmentHomeBinding
import www.iesmurgi.intercambium_app.models.Ad
import www.iesmurgi.intercambium_app.models.User
import www.iesmurgi.intercambium_app.models.adapters.AdAdapter
import www.iesmurgi.intercambium_app.ui.AdActivity
import www.iesmurgi.intercambium_app.ui.AddEditAdActivity
import www.iesmurgi.intercambium_app.utils.Constants
import www.iesmurgi.intercambium_app.utils.Utils

/**
 * A fragment representing the Home screen.
 * This fragment displays a list of ads and provides functionality to add a new ad,
 * refresh the list, and handle item click events.
 *
 * @author Denis Yushkin
 */
class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var adapter: AdAdapter

    /**
     * Inflates the layout for the [HomeFragment] and initializes UI components.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return The root View of the inflated layout for the fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        handleAddButton()
        handleSwipeRefresh()

        return root
    }

    /**
     * Called when the fragment is resumed. Updates the recyclerView.
     */
    override fun onResume() {
        super.onResume()
        recyclerView()
    }

    /**
     * Handles the behavior of the 'Add an ad' button based on user authorization status.
     */
    private fun handleAddButton() {
        // Don't show 'Add an ad' button if user is not authorized
        if (FirebaseAuth.getInstance().currentUser == null) {
            binding.fabAddHome.visibility = View.GONE
        } else {
            binding.fabAddHome.visibility = View.VISIBLE
        }

        // When user clicks on 'Add an ad'
        binding.fabAddHome.setOnClickListener {
            val intent = Intent(activity!!, AddEditAdActivity::class.java)
            intent.putExtra("GOTO", "MainActivity")
            startActivity(intent)
        }
    }

    /**
     * Sets up the behavior of the swipe refresh layout.
     */
    private fun handleSwipeRefresh() {
        val swipeRefreshLayout = binding.swipeRefreshLayoutHome
        swipeRefreshLayout.setOnRefreshListener {
            recyclerView()
        }
    }

    /**
     * Sets up the RecyclerView and loads ads from the database.
     */
    private fun recyclerView() {
        val context = requireContext()

        if (!this::adapter.isInitialized) {
            adapter = AdAdapter(context) { onItemClick(it) }
            binding.rvAdsHome.adapter = adapter
            binding.rvAdsHome.layoutManager = LinearLayoutManager(context)
        } else {
            // Avoid "E/RecyclerView: No adapter attached; skipping layout"
            binding.rvAdsHome.adapter = adapter
            // Avoid "E/RecyclerView: No layout manager attached; skipping layout"
            binding.rvAdsHome.layoutManager = LinearLayoutManager(context)
        }

        loadAdsFromDB()
    }

    /**
     * Handles the item click event in the [androidx.recyclerview.widget.RecyclerView].
     *
     * @param ad The clicked [Ad] object.
     */
    private fun onItemClick(ad: Ad) {
        // If user is not authorized, open the profile fragment to authorize
        if (FirebaseAuth.getInstance().currentUser == null) {
            Utils.navigateToFragment(view, R.id.navigation_profile)
            return
        }

        val intent = Intent(requireContext(), AdActivity::class.java)
        intent.putExtra("AD", ad.id)
        startActivity(intent)
    }

    /**
     * Loads ads from the Firestore database.
     */
    private fun loadAdsFromDB() {
        binding.pbHome.show()

        val db = Firebase.firestore
        val adsCollection = db.collection(Constants.COLLECTION_ADS)
        adsCollection.orderBy(Constants.ADS_FIELD_CREATED_AT, Query.Direction.DESCENDING)
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
                    val author = adDocument.getString(Constants.ADS_FIELD_AUTHOR).toString()
                    val usersCollection = db.collection(Constants.COLLECTION_USERS)
                    val usersDocument = usersCollection.document(author)
                    usersDocument.get()
                        .addOnSuccessListener { userDocument ->
                            // If this account doesn't exist anymore, don't show the ad
                            if (userDocument.exists()) {
                                val userEmail = userDocument.id
                                val userName =
                                    userDocument.getString(Constants.USERS_FIELD_NAME).toString()
                                val userAge =
                                    userDocument.getLong(Constants.USERS_FIELD_AGE)
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

                                val user = User(userEmail, userName, userAge, userPhoneNumber, userPhotoUrl)
                                val ad =
                                    Ad(adId, adTitle, adDesc, adProvince, adStatus,
                                        adCreatedAt, adImgUrl, user)
                                ad.visible = Utils.isAdVisibleForUser(ad)

                                // Add the add to the list
                                adapter.notifyItemInserted(adapter.adList.size)
                                adapter.adList.add(ad)
                            }

                            handleNoAdsMsg()
                        }
                }

                // There are no ads in the database
                if (adDocuments.size() == 0) {
                    handleNoAdsMsg()
                }
            }
    }

    /**
     * Handles the visibility of the 'No ads' message based on the presence and visibility of ads.
     */
    private fun handleNoAdsMsg() {
        binding.swipeRefreshLayoutHome.isRefreshing = false
        binding.pbHome.hide()

        if (adapter.adList.size == 0 || adapter.getVisibleAdsCount() == 0) {
            binding.tvNoAdsHome.visibility = View.VISIBLE
        } else {
            binding.tvNoAdsHome.visibility = View.GONE
        }
    }
}