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
import www.iesmurgi.intercambium_app.ui.AddAdActivity
import www.iesmurgi.intercambium_app.utils.Constants
import www.iesmurgi.intercambium_app.utils.Utils

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var adapter: AdAdapter

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

    override fun onResume() {
        super.onResume()
        recyclerView()
    }

    private fun handleAddButton() {
        // Don't show 'Add an ad' button if user is not authorized
        if (FirebaseAuth.getInstance().currentUser == null) {
            binding.fabAddHome.visibility = View.GONE
        } else {
            binding.fabAddHome.visibility = View.VISIBLE
        }

        // When user clicks on 'Add an ad'
        binding.fabAddHome.setOnClickListener {
            val intent = Intent(activity!!, AddAdActivity::class.java)
            startActivity(intent)
        }
    }

    private fun handleSwipeRefresh() {
        val swipeRefreshLayout = binding.swipeRefreshLayoutHome
        swipeRefreshLayout.setOnRefreshListener {
            recyclerView()
        }
    }

    private fun recyclerView() {
        if (!this::adapter.isInitialized) {
            adapter = AdAdapter(activity?.applicationContext!!) { onItemClick(it) }
            binding.rvAdsHome.adapter = adapter
            binding.rvAdsHome.layoutManager = LinearLayoutManager(activity?.applicationContext)
        } else {
            // Avoid "E/RecyclerView: No adapter attached; skipping layout"
            binding.rvAdsHome.adapter = adapter
            // Avoid "E/RecyclerView: No layout manager attached; skipping layout"
            binding.rvAdsHome.layoutManager = LinearLayoutManager(activity?.applicationContext)
        }

        loadAdsFromDB()
    }

    private fun onItemClick(ad: Ad) {
        // User is not authorized, so open the profile to authorize
        if (FirebaseAuth.getInstance().currentUser == null) {
            Utils.navigateToFragment(view, R.id.navigation_profile)
            return
        }
    }

    private fun loadAdsFromDB() {
        binding.pbHome.show()

        val db = Firebase.firestore
        val adsCollection = db.collection(Constants.COLLECTION_ADS)
        adsCollection.orderBy(Constants.ADS_FIELD_CREATED_AT, Query.Direction.ASCENDING)
            .get()
            .addOnFailureListener {
                binding.pbHome.hide()
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
                            val userEmail = userDocument.id
                            val userName = userDocument.getString(Constants.USERS_FIELD_NAME).toString()
                            val userPhoneNumber = userDocument.getString(Constants.USERS_FIELD_PHONE_NUMBER).toString()
                            val userPhotoUrl = userDocument.getString(Constants.USERS_FIELD_PHOTO_URL).toString()

                            val adId = adDocument.id
                            val adTitle = adDocument.getString(Constants.ADS_FIELD_TITLE).toString()
                            val adDesc = adDocument.getString(Constants.ADS_FIELD_DESCRIPTION).toString()
                            var adCreatedAt = adDocument.getTimestamp(Constants.ADS_FIELD_CREATED_AT)
                            val adImgUrl = adDocument.getString(Constants.ADS_FIELD_IMAGE).toString()

                            if (adCreatedAt == null) {
                                adCreatedAt = Timestamp.now()
                            }

                            val user = User(userEmail, userName, userPhoneNumber, userPhotoUrl)
                            val ad = Ad(adId, adTitle, adDesc, adCreatedAt, adImgUrl, user)

                            // Add the add to the list
                            adapter.notifyItemInserted(adapter.adList.size)
                            adapter.adList.add(ad)

                            binding.pbHome.hide()
                            handleNoAdsMsg()
                        }
                }

                // There are no ads in the database
                if (adDocuments.size() == 0) {
                    binding.pbHome.hide()
                    handleNoAdsMsg()
                }
            }
    }

    private fun handleNoAdsMsg() {
        binding.swipeRefreshLayoutHome.isRefreshing = false

        if (adapter.adList.size == 0) {
            binding.tvNoAdsHome.visibility = View.VISIBLE
        } else {
            binding.tvNoAdsHome.visibility = View.GONE
        }
    }
}