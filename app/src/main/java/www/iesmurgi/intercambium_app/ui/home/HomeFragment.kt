package www.iesmurgi.intercambium_app.ui.home

import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import www.iesmurgi.intercambium_app.R
import www.iesmurgi.intercambium_app.databinding.FragmentHomeBinding
import www.iesmurgi.intercambium_app.utils.DbUtils.Companion.toAd
import www.iesmurgi.intercambium_app.utils.DbUtils.Companion.toUser
import www.iesmurgi.intercambium_app.models.Ad
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
    private var filtering = false

    // Keep track of whether more ads are being loaded
    private var isLoadingMore = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Enable options menu
        setHasOptionsMenu(true)

        // Prevents "E/RecyclerView: No adapter attached; skipping layout"
        recyclerView(false)

        setupUIComponents()
        return root
    }

    override fun onResume() {
        super.onResume()
        recyclerView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_app_info, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.item_app_info) {
            showAppInfoPopup()
        }

        return true
    }

    private fun showAppInfoPopup() {
        val context = requireContext()
        val inflater = context.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.popup_app_info, null)

        val width = LinearLayout.LayoutParams.MATCH_PARENT
        val height = LinearLayout.LayoutParams.WRAP_CONTENT
        val popupWindow = PopupWindow(popupView, width, height)
        val rootView = requireActivity().findViewById<View?>(android.R.id.content).rootView

        popupWindow.isFocusable = true
        popupWindow.isOutsideTouchable = true
        popupWindow.animationStyle = R.style.popup_window_animation
        popupWindow.showAtLocation(rootView, Gravity.BOTTOM, 0, 0)

        popupView.setOnTouchListener { view, _ ->
            popupWindow.dismiss()
            view.performClick()
            true
        }
    }

    /**
     * Sets up the UI components and event listeners.
     */
    private fun setupUIComponents() {
        handleAddButton()
        handleSwipeRefresh()
        handleRecyclerViewScrollListener()
        handleSearchView()
    }

    /**
     * Handles the behavior of the 'Add an ad' button based on user authorization status.
     */
    private fun handleAddButton() {
        // Don't show 'Add an ad' button if user is not authorized
        binding.fabAddHome.visibility = if (FirebaseAuth.getInstance().currentUser != null) View.VISIBLE else View.GONE

        // When user clicks on 'Add an ad'
        binding.fabAddHome.setOnClickListener {
            val intent = Intent(activity!!, AddEditAdActivity::class.java)
            intent.putExtra("GOTO", "MainActivity")
            startActivity(intent)
        }
    }

    /**
     * Sets up the RecyclerView and loads ads from the database.
     */
    private fun recyclerView(load: Boolean = true, query: String = "") {
        val context = requireContext()

        adapter = AdAdapter(context) { onItemClick(it) }
        // Avoid "E/RecyclerView: No adapter attached; skipping layout"
        binding.rvAdsHome.adapter = adapter
        // Avoid "E/RecyclerView: No layout manager attached; skipping layout"
        binding.rvAdsHome.layoutManager = LinearLayoutManager(context)

        if (load) {
            loadAdsFromDB(query)
        }
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
     * Sets up the behavior of the swipe refresh layout.
     */
    private fun handleSwipeRefresh() {
        val swipeRefreshLayout = binding.swipeRefreshLayoutHome
        swipeRefreshLayout.setOnRefreshListener {
            // Need to take into account that there may be a search query at the moment
            val query = if (binding.svAds.query.isNullOrBlank()) "" else binding.svAds.query.toString().trim()
            loadAdsFromDB(query)
        }
    }

    /**
     * Sets up the scroll listener for the RecyclerView to handle infinite scrolling.
     */
    private fun handleRecyclerViewScrollListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.rvAdsHome.setOnScrollChangeListener { _, _, _, _, _ ->
                if (!binding.rvAdsHome.canScrollVertically(1) && !isLoadingMore) {
                    isLoadingMore = true

                    if (filtering) {
                        val query = if (binding.svAds.query.isNullOrBlank()) "" else binding.svAds.query.toString().trim()
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
        binding.svAds.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filtering = true
                loadAdsFromDB(query.orEmpty())

                // Clear the focus and collapse the SearchView
                // Prevents calling onQueryTextSubmit twice
                binding.svAds.clearFocus()

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
        val isNetWorkAvailable = Utils.isNetworkAvailable(requireContext())

        if (!isNetWorkAvailable) {
            binding.swipeRefreshLayoutHome.isRefreshing = false
            binding.tvNoAdsHome.text = getString(R.string.no_access_to_internet)
            binding.tvNoAdsHome.visibility = View.VISIBLE
            return
        }

        binding.swipeRefreshLayoutHome.isRefreshing = true

        val db = Firebase.firestore
        val adsCollection = db.collection(Constants.COLLECTION_ADS)
        val queryTask = adsCollection.orderBy(Constants.ADS_FIELD_CREATED_AT, Query.Direction.DESCENDING)

        // Only load limited ads initially
        var limit = Constants.ADS_INITIAL_LOAD_COUNT

        if (loadMore) {
            // If loading more ads, increase the limit
            limit += Constants.ADS_MORE_COUNT
        }

        if (query.isNotEmpty()) {
            // Create both tasks (documents containing title and documents containing description)
            val queryWords = query.split(" ")
            val titleQuery = adsCollection.whereArrayContainsAny(Constants.ADS_FIELD_TITLE_SEARCH, queryWords)
            val descriptionQuery = adsCollection.whereArrayContainsAny(Constants.ADS_FIELD_DESCRIPTION_SEARCH, queryWords)

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
            adapter.notifyDataSetChanged()
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
        if (!isAdded) {
            return
        }

        isLoadingMore = false

        with(binding) {
            // Hide Swipe Refresh animation
            swipeRefreshLayoutHome.isRefreshing = false

            tvNoAdsHome.text = getString(R.string.no_ads)
            tvNoAdsHome.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
        }
    }
}