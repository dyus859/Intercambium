package www.iesmurgi.intercambium_app.ui.chats

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import www.iesmurgi.intercambium_app.R
import www.iesmurgi.intercambium_app.databinding.FragmentChatsBinding
import www.iesmurgi.intercambium_app.utils.DbUtils.Companion.toMessage
import www.iesmurgi.intercambium_app.utils.DbUtils.Companion.toUser
import www.iesmurgi.intercambium_app.models.Chat
import www.iesmurgi.intercambium_app.models.adapters.ChatAdapter
import www.iesmurgi.intercambium_app.ui.ChatActivity
import www.iesmurgi.intercambium_app.utils.Constants
import www.iesmurgi.intercambium_app.utils.Utils
import kotlin.collections.ArrayList

/**
 * Fragment that displays the list of chats.
 *
 * @author Denis Yushkin
 */
class ChatsFragment : Fragment() {

    private lateinit var binding: FragmentChatsBinding
    private lateinit var adapter: ChatAdapter
    private var filtering = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Prevents "E/RecyclerView: No adapter attached; skipping layout"
        recyclerView(false)

        setupUIComponents()
        return root
    }

    override fun onResume() {
        super.onResume()
        recyclerView()
    }

    /**
     * Sets up the RecyclerView and loads ads from the database.
     */
    private fun recyclerView(load: Boolean = true, query: String = "") {
        val context = requireContext()

        adapter = ChatAdapter(context) { onItemClick(it) }
        // Avoid "E/RecyclerView: No adapter attached; skipping layout"
        binding.rvChats.adapter = adapter
        // Avoid "E/RecyclerView: No layout manager attached; skipping layout"
        binding.rvChats.layoutManager = LinearLayoutManager(context)

        if (load) {
            loadChatsFromDB(query)
        }
    }

    /**
     * Handles the item click event in the [androidx.recyclerview.widget.RecyclerView].
     *
     * @param chat The clicked [Chat] object.
     */
    private fun onItemClick(chat: Chat) {
        // If user is not authorized, open the profile fragment to authorize
        if (FirebaseAuth.getInstance().currentUser == null) {
            Utils.navigateToFragment(view, R.id.navigation_profile)
            return
        }

        val intent = Intent(requireContext(), ChatActivity::class.java)
        intent.putExtra("USER", chat.receiverUser)
        startActivity(intent)
    }

    /**
     * Sets up the UI components and event listeners.
     */
    private fun setupUIComponents() {
        handleSwipeRefresh()
        handleSearchView()
    }

    /**
     * Sets up the behavior of the swipe refresh layout.
     */
    private fun handleSwipeRefresh() {
        val swipeRefreshLayout = binding.swipeRefreshLayoutChats
        swipeRefreshLayout.setOnRefreshListener {
            // Need to take into account that there may be a search query at the moment
            val query = if (binding.svChats.query.isNullOrBlank()) "" else binding.svChats.query.toString().trim()
            loadChatsFromDB(query)
        }
    }

    /**
     * Sets up the search functionality for the search view.
     */
    private fun handleSearchView() {
        // Register an `OnQueryTextListener` to handle search query submission.
        binding.svChats.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filtering = true
                loadChatsFromDB(query.orEmpty())

                // Clear the focus and collapse the SearchView
                // Prevents calling onQueryTextSubmit twice
                binding.svChats.clearFocus()

                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val query = newText.orEmpty().lowercase().trim()
                if (query.isEmpty() && filtering) {
                    filtering = false
                    loadChatsFromDB(query)
                }
                return false
            }
        })
    }

    /**
     * Loads chats from the database based on the given query.
     *
     * @param query The search query string. Default is an empty string.
     */
    private fun loadChatsFromDB(query: String = "") {
        val isNetWorkAvailable = Utils.isNetworkAvailable(requireContext())

        if (!isNetWorkAvailable) {
            binding.swipeRefreshLayoutChats.isRefreshing = false
            binding.tvNoChats.text = getString(R.string.no_access_to_internet)
            binding.tvNoChats.visibility = View.VISIBLE
            return
        }

        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Show Swipe Refresh animation
        binding.swipeRefreshLayoutChats.isRefreshing = true

        val db = Firebase.firestore
        val chatsCollection = db.collection(Constants.COLLECTION_CHATS)

        chatsCollection.get()
            .addOnSuccessListener { querySnapshot ->
                val chatList: ArrayList<Chat> = ArrayList()
                val userTasks = mutableListOf<Deferred<Unit>>()
                val queryWords = query.split(" ")

                for (document in querySnapshot.documents) {
                    val chatId = document.id

                    if (chatId.startsWith(currentUserUid)) {
                        val receiverUid = chatId.substringAfter(currentUserUid)

                        // Find the user in the users collection based on receiverUid
                        val userQuery = db.collection(Constants.COLLECTION_USERS)
                            .whereEqualTo(Constants.USERS_FIELD_UID, receiverUid)

                        val userTask = GlobalScope.async {
                            try {
                                val userQuerySnapshot = userQuery.get().await()

                                if (userQuerySnapshot.documents.size == 1) {
                                    val userDocument = userQuerySnapshot.documents[0]
                                    val user = userDocument.toUser()

                                    // Get the latest message for the sender
                                    val messagesCollection = chatsCollection.document(chatId)
                                        .collection(Constants.CHATS_COLLECTION_MESSAGES)

                                    val latestMessageQuery = messagesCollection
                                        .orderBy(Constants.CHATS_FIELD_TIME, Query.Direction.DESCENDING)
                                        .limit(1)

                                    val messageQuerySnapshot = latestMessageQuery.get().await()
                                    val latestMessageDocument = messageQuerySnapshot.documents.firstOrNull()
                                    val latestMessage = latestMessageDocument?.toMessage()

                                    if (query.isEmpty() || isNameSearchMatch(user.nameSearch, queryWords)) {
                                        val chat = Chat(
                                            chatId,
                                            user,
                                            latestMessage?.content ?: "",
                                            latestMessage?.imageUrl ?: "",
                                            latestMessage?.timeStamp ?: 0L // Store the latest message time
                                        )
                                        chatList.add(chat)
                                    }
                                }
                            } catch (e: Exception) {
                                // Handle exceptions if necessary
                            }
                        }

                        userTasks.add(userTask)
                    }
                }

                GlobalScope.launch(Dispatchers.Main) {
                    // Await completion of all user tasks
                    userTasks.awaitAll()

                    // Sort the chatList based on the latest message time (newest to oldest)
                    val sortedChatList = chatList.sortedByDescending { it.lastMsgTime }

                    adapter.chatsList.apply {
                        clear()
                        addAll(sortedChatList)
                    }

                    adapter.notifyDataSetChanged()

                    handleNoAdsMsg()
                }
            }
            .addOnFailureListener { handleNoAdsMsg() }
    }

    /**
     * Checks if any of the words in the query match any of the elements in the nameSearch array.
     *
     * @param nameSearch The list of strings to search within.
     * @param queryWords The list of words to search for.
     * @return True if any word in the query matches any element in nameSearch, false otherwise.
     */
    private fun isNameSearchMatch(nameSearch: List<String>, queryWords: List<String>): Boolean {
        for (word in queryWords) {
            if (nameSearch.any { it.contains(word, ignoreCase = true) }) {
                return true
            }
        }
        return false
    }

    /**
     * Handles the visibility of the 'No chats' message based on the presence and visibility of chats.
     */
    private fun handleNoAdsMsg() {
        if (!isAdded) {
            return
        }

        with(binding) {
            // Hide Swipe Refresh animation
            swipeRefreshLayoutChats.isRefreshing = false

            tvNoChats.text = requireContext().getString(R.string.no_chats)
            tvNoChats.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
        }
    }
}