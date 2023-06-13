package www.iesmurgi.intercambium_app.ui.chats

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import www.iesmurgi.intercambium_app.R
import www.iesmurgi.intercambium_app.databinding.FragmentChatsBinding
import www.iesmurgi.intercambium_app.utils.DbUtils.Companion.toMessage
import www.iesmurgi.intercambium_app.utils.DbUtils.Companion.toUser
import www.iesmurgi.intercambium_app.models.Chat
import www.iesmurgi.intercambium_app.models.adapters.ChatAdapter
import www.iesmurgi.intercambium_app.ui.ChatActivity
import www.iesmurgi.intercambium_app.utils.Constants
import www.iesmurgi.intercambium_app.utils.Utils

class ChatsFragment : Fragment() {

    private lateinit var binding: FragmentChatsBinding
    private lateinit var adapter: ChatAdapter
    private var filtering = false

    /**
     * Inflates the layout for the [ChatsFragment] and initializes UI components.
     *
     * @param inflater The [LayoutInflater] object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return The root View of the inflated layout for the fragment.
     */
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

    /**
     * Called when the fragment is resumed. Updates the recyclerView.
     */
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
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        println("currentUserUid: $currentUserUid")

        // Show Swipe Refresh animation
        binding.swipeRefreshLayoutChats.isRefreshing = true

        val db = Firebase.firestore
        val chatsCollection = db.collection(Constants.COLLECTION_CHATS)

        chatsCollection.get()
            .addOnSuccessListener { querySnapshot ->
                val chatList: ArrayList<Chat> = ArrayList()
                val userTasks = mutableListOf<Task<QuerySnapshot>>()

                for (document in querySnapshot.documents) {
                    val chatId = document.id

                    if (chatId.startsWith(currentUserUid)) {
                        val receiverUid = chatId.substringAfter(currentUserUid)

                        // Find the user in the users collection based on receiverUid
                        val userQuery = db.collection(Constants.COLLECTION_USERS)
                            .whereEqualTo(Constants.USERS_FIELD_UID, receiverUid)

                        val userTask = userQuery.get()
                            .addOnSuccessListener { userQuerySnapshot ->
                                if (userQuerySnapshot.documents.size == 1) {
                                    val userDocument = userQuerySnapshot.documents[0]
                                    val user = userDocument.toUser()

                                    // Get the latest message for the sender
                                    val messagesCollection = chatsCollection.document(chatId)
                                        .collection(Constants.CHATS_COLLECTION_MESSAGES)

                                    val latestMessageQuery = messagesCollection
                                        .orderBy(Constants.CHATS_FIELD_TIME, Query.Direction.DESCENDING)
                                        .limit(1)

                                    latestMessageQuery.get()
                                        .addOnSuccessListener { messageQuerySnapshot ->
                                            val latestMessageDocument =
                                                messageQuerySnapshot.documents.firstOrNull()

                                            val latestMessage = latestMessageDocument?.toMessage()

                                            val chat = Chat(chatId, user, latestMessage?.content ?: "")
                                            chatList.add(chat)

                                            // Check if all tasks have completed
                                            if (chatList.size == userTasks.size) {
                                                // Clear the existing chat list and add the new chat items
                                                adapter.chatsList.apply {
                                                    clear()
                                                    addAll(chatList)
                                                }

                                                // Notify the adapter of the changes
                                                adapter.notifyDataSetChanged()

                                                handleNoAdsMsg()
                                            }
                                        }
                                        .addOnFailureListener {
                                            val chat = Chat(chatId, user)
                                            chatList.add(chat)

                                            // Check if all tasks have completed
                                            if (chatList.size == userTasks.size) {
                                                // Clear the existing chat list and add the new chat items
                                                adapter.chatsList.apply {
                                                    clear()
                                                    addAll(chatList)
                                                }

                                                // Notify the adapter of the changes
                                                adapter.notifyDataSetChanged()

                                                handleNoAdsMsg()
                                            }
                                        }
                                }
                            }

                        userTasks.add(userTask)
                    }
                }

                // Check if all tasks have completed
                if (chatList.size == userTasks.size) {
                    // Clear the existing chat list and add the new chat items
                    adapter.chatsList.apply {
                        clear()
                        addAll(chatList)
                    }

                    // Notify the adapter of the changes
                    adapter.notifyDataSetChanged()

                    handleNoAdsMsg()
                }
            }
            .addOnFailureListener { handleNoAdsMsg() }
    }

    /**
     * Handles the visibility of the 'No chats' message based on the presence and visibility of chats.
     */
    private fun handleNoAdsMsg() {
        with(binding) {
            // Hide Swipe Refresh animation
            swipeRefreshLayoutChats.isRefreshing = false

            tvNoChats.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
        }
    }
}