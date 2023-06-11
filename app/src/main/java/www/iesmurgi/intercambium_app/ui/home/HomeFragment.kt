package www.iesmurgi.intercambium_app.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import www.iesmurgi.intercambium_app.R
import www.iesmurgi.intercambium_app.databinding.FragmentHomeBinding
import www.iesmurgi.intercambium_app.db.DbUtils.Companion.toAd
import www.iesmurgi.intercambium_app.db.DbUtils.Companion.toUser
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
        handleSearchView()

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
     * Sets up the RecyclerView and loads ads from the database.
     */
    private fun recyclerView(query: String = "") {
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
            if (!filtering) {
                recyclerView()
            } else {
                recyclerView(binding.svAds.query.toString().trim())
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
    private fun loadAdsFromDB(query: String = "") {
        binding.pbHome.show()

        val db = Firebase.firestore
        val adsCollection = db.collection(Constants.COLLECTION_ADS)
        val queryTask = adsCollection.orderBy(Constants.ADS_FIELD_CREATED_AT, Query.Direction.DESCENDING)

        // Perform separate queries for 'title' and 'description' fields if the query is not empty
        if (query.isNotEmpty()) {
            val titleQuery = adsCollection.whereArrayContains(Constants.ADS_FIELD_TITLE_SEARCH, query)
            val descriptionQuery = adsCollection.whereArrayContains(Constants.ADS_FIELD_DESCRIPTION_SEARCH, query)

            // Execute title query
            titleQuery.get().addOnSuccessListener { titleDocuments ->
                // Execute description query
                descriptionQuery.get().addOnSuccessListener { descriptionDocuments ->
                    // Merge the results
                    val mergedDocuments = mergeQueryDocuments(titleDocuments.documents, descriptionDocuments.documents)
                    // Process the merged documents
                    processQueryResults(mergedDocuments)
                }.addOnFailureListener {
                    handleNoAdsMsg()
                }
            }.addOnFailureListener {
                handleNoAdsMsg()
            }
        } else {
            // If the query is empty, execute the default query
            queryTask.get()
                .addOnFailureListener {
                    handleNoAdsMsg()
                }
                .addOnSuccessListener { adDocuments ->
                    // Process the query results
                    processQueryResults(adDocuments.documents)
                }
        }
    }

    /**
     * Merges Firestore query documents and removes duplicates based on document ID.
     *
     * @param query1 The first list of Firestore query documents.
     * @param query2 The second list of Firestore query documents.
     * @return The merged list of documents without duplicates.
     */
    private fun mergeQueryDocuments(
        query1: List<DocumentSnapshot>,
        query2: List<DocumentSnapshot>
    ): List<DocumentSnapshot> {
        val mergedDocuments = mutableListOf<DocumentSnapshot>()
        mergedDocuments.addAll(query1)
        mergedDocuments.addAll(query2)

        // Remove duplicates based on document ID
        val uniqueDocuments = linkedSetOf<DocumentSnapshot>()
        uniqueDocuments.addAll(mergedDocuments)

        return uniqueDocuments.toList()
    }

    /**
     * Processes the query results and updates the RecyclerView.
     *
     * @param adDocuments The list of ad documents from the query result.
     */
    private fun processQueryResults(adDocuments: List<DocumentSnapshot>) {
        val db = Firebase.firestore
        val usersCollection = db.collection(Constants.COLLECTION_USERS)

        // Create a list to hold the new ad items
        val newAdList = mutableListOf<Ad>()

        // Process each ad document in parallel
        val tasks = adDocuments.map { adDocument ->
            val author = adDocument.getString(Constants.ADS_FIELD_AUTHOR).toString()
            val usersDocument = usersCollection.document(author)
            usersDocument.get().addOnSuccessListener { userDocument ->
                // If this account doesn't exist anymore, don't show the ad
                if (userDocument.exists()) {
                    val user = userDocument.toUser()
                    val ad = adDocument.toAd(user)
                    ad.visible = Utils.isAdVisibleForUser(ad)

                    newAdList.add(ad)
                }
            }
        }

        // Wait for all tasks to complete
        Tasks.whenAllSuccess<DocumentSnapshot>(tasks)
            .addOnCompleteListener {
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
        binding.swipeRefreshLayoutHome.isRefreshing = false
        binding.pbHome.hide()

        if (adapter.adList.size == 0 || adapter.getVisibleAdsCount() == 0) {
            binding.tvNoAdsHome.visibility = View.VISIBLE
        } else {
            binding.tvNoAdsHome.visibility = View.GONE
        }
    }
}