package www.iesmurgi.intercambium_app.ui

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import www.iesmurgi.intercambium_app.R
import www.iesmurgi.intercambium_app.databinding.ActivityMyAdsBinding
import www.iesmurgi.intercambium_app.utils.DbUtils.Companion.toAd
import www.iesmurgi.intercambium_app.utils.DbUtils.Companion.toUser
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
    private var filtering = false

    private lateinit var user: User

    // Keep track of whether more ads are being loaded
    private var isLoadingMore = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyAdsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Return to the previous activity
        actionBar?.setDisplayHomeAsUpEnabled(true)

        user = SharedData.getUser().value!!
        setupUIComponents()
    }

    override fun onResume() {
        super.onResume()
        recyclerView()
    }

    /**
     * Sets up the UI components and event listeners.
     */
    private fun setupUIComponents() {
        handleSwipeRefresh()
        handleRecyclerViewScrollListener()
        handleSearchView()
    }

    /**
     * Sets up the RecyclerView and loads ads from the database.
     */
    private fun recyclerView(query: String = "") {
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

        loadAdsFromDB(query)
    }

    /**
     * Handles the item click event in the [androidx.recyclerview.widget.RecyclerView].
     *
     * @param ad The clicked [Ad] object.
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
     * Sets up the behavior of the swipe refresh layout.
     */
    private fun handleSwipeRefresh() {
        val swipeRefreshLayout = binding.swipeRefreshLayoutMyAds
        swipeRefreshLayout.setOnRefreshListener {
            // Need to take into account that there may be a search query at the moment
            val query = if (binding.svMyAds.query.isNullOrBlank()) "" else binding.svMyAds.query.toString().trim()
            recyclerView(query)
        }
    }

    /**
     * Sets up the scroll listener for the RecyclerView to handle infinite scrolling.
     */
    private fun handleRecyclerViewScrollListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.rvAdsMyAds.setOnScrollChangeListener { _, _, _, _, _ ->
                if (!binding.rvAdsMyAds.canScrollVertically(1) && !isLoadingMore) {
                    isLoadingMore = true

                    if (filtering) {
                        val query = if (binding.svMyAds.query.isNullOrBlank()) {
                            ""
                        } else {
                            binding.svMyAds.query.toString().trim()
                        }

                        loadAdsFromDB(query, loadMore = true)
                    } else {
                        loadAdsFromDB("", loadMore = true)
                    }
                }
            }
        }
    }

    /**
     * Sets up the search functionality for the search view.
     */
    private fun handleSearchView() {
        // Register an `OnQueryTextListener` to handle search query submission.
        binding.svMyAds.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filtering = true
                loadAdsFromDB(query.orEmpty())

                // Clear the focus and collapse the SearchView
                // Prevents calling onQueryTextSubmit twice
                binding.svMyAds.clearFocus()

                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val query = newText.orEmpty().lowercase().trim()
                if (query.isEmpty() && filtering) {
                    filtering = false
                    loadAdsFromDB(query)
                }
                return false
            }
        })
    }

    /**
     * Loads ads from the database based on the given query.
     *
     * @param query The search query string. Default is an empty string.
     */
    private fun loadAdsFromDB(query: String = "", loadMore: Boolean = false) {
        val isNetWorkAvailable = Utils.isNetworkAvailable(this)

        if (!isNetWorkAvailable) {
            binding.swipeRefreshLayoutMyAds.isRefreshing = false
            binding.tvNoAdsMyAds.text = getString(R.string.no_access_to_internet)
            binding.tvNoAdsMyAds.visibility = View.VISIBLE
            return
        }

        binding.swipeRefreshLayoutMyAds.isRefreshing = true

        val db = Firebase.firestore
        val adsCollection = db.collection(Constants.COLLECTION_ADS)
        val queryTask = adsCollection
            .orderBy(Constants.ADS_FIELD_CREATED_AT, Query.Direction.DESCENDING)
            .whereEqualTo(Constants.ADS_FIELD_AUTHOR, user.email)

        // Only load limited ads initially
        var limit = Constants.ADS_INITIAL_LOAD_COUNT

        if (loadMore) {
            // If loading more ads, increase the limit
            limit += Constants.ADS_MORE_COUNT
        }

        if (query.isNotEmpty()) {
            // Create both tasks (documents containing title and documents containing description)
            val titleQuery = adsCollection.whereArrayContains(Constants.ADS_FIELD_TITLE_SEARCH, query)
            val descriptionQuery = adsCollection.whereArrayContains(Constants.ADS_FIELD_DESCRIPTION_SEARCH, query)

            val titleTask = titleQuery.get()
            val descriptionTask = descriptionQuery.get()

            // Execute both tasks first, then listen
            Tasks.whenAllSuccess<List<DocumentSnapshot>>(titleTask, descriptionTask)
                .addOnSuccessListener { results ->
                    val titleDocuments = results[0] as QuerySnapshot
                    val descriptionDocuments = results[1] as QuerySnapshot

                    // Merge both document into one
                    val mergedDocuments = titleDocuments.documents +
                            descriptionDocuments.documents.distinctBy { it.id }

                    // Sort by created_at field (descending order) to always show the newest
                    val sortedDocuments = mergedDocuments.sortedByDescending {
                        val timestamp = it.getLong(Constants.ADS_FIELD_CREATED_AT) ?: 0
                        java.util.Date(timestamp)
                    }
                    processQueryResults(sortedDocuments, loadMore)
                }
                .addOnFailureListener {
                    handleNoAdsMsg()
                }
        } else {
            // There is no query, execute it directly

            queryTask.limit(limit.toLong()).get().addOnSuccessListener { adDocuments ->
                processQueryResults(adDocuments.documents, loadMore)
            }.addOnFailureListener {
                handleNoAdsMsg()
            }
        }
    }

    /**
     * Processes the query results and updates the RecyclerView.
     *
     * @param adDocuments The list of ad documents from the query result.
     */
    private fun processQueryResults(adDocuments: List<DocumentSnapshot>, loadMore: Boolean = false) {
        // Clear the existing ad list only if it's not a load more operation
        if (!loadMore) {
            adapter.adList.clear()
        }

        val db = Firebase.firestore
        val usersCollection = db.collection(Constants.COLLECTION_USERS)

        // Create a list to hold the new ad items
        val newAdList = mutableListOf<Ad>()

        // Add new ad items to the ad list
        newAdList.forEachIndexed { index, ad ->
            if (!loadMore || index >= adapter.adList.size) {
                // Only add new ads or if it's a load more operation, add ads beyond the current list size
                adapter.adList.add(ad)
            }
        }

        // Create a HashSet to keep track of unique ad IDs
        val uniqueAdIds = HashSet<String>()

        // Create a coroutine scope
        val coroutineScope = CoroutineScope(Dispatchers.Main)

        // Create a suspend function to process each ad document
        suspend fun processAdDocument(adDocument: DocumentSnapshot) {
            val author = adDocument.getString(Constants.ADS_FIELD_AUTHOR).toString()
            val usersDocument = usersCollection.document(author)
            val userDocument = withContext(Dispatchers.IO) { usersDocument.get().await() }

            // If this account doesn't exist anymore, don't show the ad
            if (userDocument.exists()) {
                val user = userDocument.toUser()
                val ad = adDocument.toAd(user)

                if (Utils.isAdVisibleForUser(ad) && !uniqueAdIds.contains(ad.id)) {
                    newAdList.add(ad)
                    uniqueAdIds.add(ad.id)
                }
            }
        }

        // Create a suspend function to process all ad documents sequentially
        suspend fun processAllAdDocuments() {
            for (adDocument in adDocuments) {
                processAdDocument(adDocument)
            }
        }

        // Start the coroutine to process all ad documents sequentially
        coroutineScope.launch {
            processAllAdDocuments()

            // Clear the existing ad list and add the new ad items
            adapter.adList.clear()
            adapter.adList.addAll(newAdList)

            // Notify the adapter of the changes
            adapter.notifyDataSetChanged()

            // Handle no ads message
            handleNoAdsMsg()
        }

        if (adDocuments.isEmpty()) {
            handleNoAdsMsg()
        }
    }

    /**
     * Handles the visibility of the 'No ads' message based on the presence and visibility of ads.
     */
    private fun handleNoAdsMsg() {
        with(binding) {
            // Hide Swipe Refresh animation
            swipeRefreshLayoutMyAds.isRefreshing = false

            tvNoAdsMyAds.text = getString(R.string.no_ads)
            tvNoAdsMyAds.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
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