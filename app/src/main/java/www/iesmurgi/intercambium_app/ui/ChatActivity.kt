package www.iesmurgi.intercambium_app.ui

import android.app.ProgressDialog
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import www.iesmurgi.intercambium_app.R
import www.iesmurgi.intercambium_app.databinding.ActivityChatBinding
import www.iesmurgi.intercambium_app.utils.DbUtils.Companion.toMessage
import www.iesmurgi.intercambium_app.models.Message
import www.iesmurgi.intercambium_app.models.User
import www.iesmurgi.intercambium_app.models.adapters.MessagesAdapter
import www.iesmurgi.intercambium_app.utils.*
import java.util.*
import kotlin.collections.ArrayList

/**
 * Activity for the chat screen.
 *
 * @author Denis Yushkin
 */
class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var adapter: MessagesAdapter
    private lateinit var messages: ArrayList<Message>
    private lateinit var senderRoom: String
    private lateinit var receiverRoom: String
    private lateinit var database: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var dialog: ProgressDialog
    private lateinit var senderUid: String
    private lateinit var receiverUid: String

    private lateinit var senderUser: User
    private lateinit var receiverUser: User

    private var latestImgUri: Uri? = null
    private lateinit var selectImageFromGalleryResult: ActivityResultLauncher<String>
    private lateinit var takeImageResult: ActivityResultLauncher<Uri>

    private lateinit var selectedMessage: Message

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        senderUser = SharedData.getUser().value!!
        receiverUser = intent.getSerializableExtra("USER") as User
        database = Firebase.firestore
        storage = Firebase.storage
        messages = ArrayList()

        setupHeader()
        setupRecyclerView()
        setupProgressDialog()
        setLaunchers()
        setListeners()

        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menuInflater.inflate(R.menu.delete_message_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.option_delete_for_everyone -> {
                deleteMessageForEveryone()
            }
            R.id.option_delete_for_me -> {
                deleteMessageForMe()
            }
        }
        return super.onContextItemSelected(item)
    }

    /**
     * Updates the user status to indicate whether the user is online or offline.
     *
     * @param online true if the user is online, false otherwise.
     */
    private fun updateUserStatus(online: Boolean) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentEmail = currentUser?.email

        if (currentEmail != null) {
            val statusMap = hashMapOf(
                Constants.USERS_FIELD_ONLINE to online,
                Constants.USERS_FIELD_LAST_ACTIVE to FieldValue.serverTimestamp()
            )

            database.collection(Constants.COLLECTION_USERS)
                .document(currentEmail)
                .update(statusMap)
        }
    }

    override fun onResume() {
        super.onResume()
        updateUserStatus(online = true)
    }

    override fun onPause() {
        super.onPause()
        updateUserStatus(online = false)
    }

    /**
     * Sets up the header of the chat activity.
     */
    private fun setupHeader() {
        setSupportActionBar(binding.chatToolbar)

        // User's name
        binding.chatUserName.text = receiverUser.name

        // Set receiver user's profile picture
        if (receiverUser.photoUrl.isNotEmpty()) {
            // User's image
            Glide.with(this)
                .load(receiverUser.photoUrl)
                .placeholder(R.drawable.default_avatar)
                .into(binding.chatIvProfilePicture)
        }

        // Back
        binding.chatIvBack.setOnClickListener { finish() }
    }

    /**
     * Sets up the RecyclerView for displaying chat messages.
     */
    private fun setupRecyclerView() {
        receiverUid = receiverUser.uid
        senderUid = FirebaseAuth.getInstance().uid.toString()
        senderRoom = senderUid + receiverUid
        receiverRoom = receiverUid + senderUid

        adapter = MessagesAdapter(this, messages, senderRoom, receiverRoom) {
                view: View, message: Message ->
            onItemLongClickListener(view, message)
        }
        binding.rvMessages.layoutManager = LinearLayoutManager(this)
        binding.rvMessages.adapter = adapter
    }

    /**
     * Handles the long click event on a message item.
     */
    private fun onItemLongClickListener(view: View, message: Message) {
        if (message.deleted) {
            return
        }

        selectedMessage = message

        registerForContextMenu(view)
        openContextMenu(view)
    }

    /**
     * Sets up the progress dialog used for displaying image uploading progress.
     */
    private fun setupProgressDialog() {
        dialog = ProgressDialog(this)
        dialog.setMessage(getString(R.string.uploading_image))
        dialog.setCancelable(false)
    }

    /**
     * Sets up the activity result launchers for selecting an image from the gallery and taking
     *  a picture.
     * The selected or captured image URI is stored in the [latestImgUri] property, and the preview
     *  image view is updated accordingly.
     */
    private fun setLaunchers() {
        selectImageFromGalleryResult = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                latestImgUri = uri
                uploadImageMessage(uri)
            }
        }

        takeImageResult = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { isSuccess ->
            if (isSuccess) {
                latestImgUri?.let { uri ->
                    uploadImageMessage(uri)
                }
            }
        }
    }

    /**
     * Sets up the listeners for various UI elements and events.
     */
    private fun setListeners() {
        setUserStatusListener()
        setupSenderMessagesListener()
        setupSendButtonListener()

        // User has selected 'Attachment'
        binding.btnChatAttach.setOnClickListener {
            selectImageFromGallery()
        }

        // User has selected 'Take a photo'
        binding.btnChatCamera.setOnClickListener {
            takeImage()
        }
    }

    /**
     * Sets up a listener to monitor changes in the receiver user's online status.
     * Updates the UI accordingly.
     */
    private fun setUserStatusListener() {
        database.collection(Constants.COLLECTION_USERS)
            .document(receiverUser.email)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    // Handle error
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val online = snapshot.getBoolean(Constants.USERS_FIELD_ONLINE)
                    if (online == false) {
                        binding.chatUserStatus.visibility = View.GONE
                    } else {
                        binding.chatUserStatus.text = getString(R.string.user_online)
                        binding.chatUserStatus.visibility = View.VISIBLE
                    }
                }
            }
    }

    /**
     * Sets up a listener to monitor changes in the sender's chat messages.
     * Updates the message list and scrolls to the bottom of the RecyclerView.
     */
    private fun setupSenderMessagesListener() {
        database.collection(Constants.COLLECTION_CHATS)
            .document(senderRoom)
            .collection(Constants.CHATS_COLLECTION_MESSAGES)
            .orderBy(Constants.CHATS_FIELD_TIME, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    // Handle error
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messageList = mutableListOf<Message>()

                    for (document in snapshot.documents) {
                        val message = document.toMessage()

                        message.type = if (message.senderId == receiverUid) {
                            Constants.CHAT_RECEIVE_ID
                        } else {
                            Constants.CHAT_SEND_ID
                        }

                        messageList.add(message)
                    }

                    messages.clear()
                    messages.addAll(messageList)
                    adapter.notifyDataSetChanged()

                    scrollToBottom()
                }
            }
    }

    /**
     * Scrolls the RecyclerView to the bottom.
     * This is called after the layout is calculated to ensure accurate positioning.
     */
    private fun scrollToBottom() {
        with(binding) {
            // Scroll to the bottom after the layout is calculated
            rvMessages.post {
                rvMessages.layoutManager?.let {
                    val itemCount = adapter.itemCount
                    if (itemCount > 0) {
                        rvMessages.scrollToPosition(itemCount - 1)
                    }
                }
            }
        }
    }

    /**
     * Sets up a listener for the send button click event.
     * Creates a new message document and clears the message box.
     */
    private fun setupSendButtonListener() {
        binding.btnChatSend.setOnClickListener {
            val content: String = binding.chatMessageBox.text.toString().trim()

            // There is no text, can't send
            if (content.isEmpty()) {
                return@setOnClickListener
            }

            // Save the message
            createNewMessageDocument(content)

            // Empty the EditText
            binding.chatMessageBox.setText("")
        }
    }

    /**
     * Creates a new document for the message in the sender and receiver rooms.
     *
     * @param content The content of the message.
     * @param imageUrl The URL of the image attached to the message, if any.
     */
    private fun createNewMessageDocument(content: String = "", imageUrl: String = "") {
        val chatsCollection = database.collection(Constants.COLLECTION_CHATS)

        val senderRoomRef = chatsCollection.document(senderRoom)
        val receiverRoomRef = chatsCollection.document(receiverRoom)
        val participants = listOf(receiverUid, senderUid)
        val data = hashMapOf(Constants.CHATS_FIELD_PARTICIPANTS to participants)

        // Need to add a field to prevent "Este documento no existe, por lo que no aparecerá
        // en las consultas ni en las instantáneas"
        senderRoomRef.get()
            .addOnSuccessListener {
                if (!it.exists()) {
                    senderRoomRef.set(data)
                }
            }

        // Need to add a field to prevent "Este documento no existe, por lo que no aparecerá
        // en las consultas ni en las instantáneas"
        receiverRoomRef.get()
            .addOnSuccessListener {
                if (!it.exists()) {
                    receiverRoomRef.set(data)
                }
            }

        val messageId = senderRoomRef.collection(Constants.CHATS_COLLECTION_MESSAGES).document().id

        val message = Message(messageId, content, senderUid, imageUrl, Date().time)
        val msgData = DbUtils.getMessageData(message)

        senderRoomRef.collection(Constants.CHATS_COLLECTION_MESSAGES).document(messageId).set(msgData)
        receiverRoomRef.collection(Constants.CHATS_COLLECTION_MESSAGES).document(messageId).set(msgData)

        val notificationMsg = if (imageUrl.isEmpty()) {
            getString(R.string.notification_message_sent, senderUser.name, message.content)
        } else {
            getString(R.string.notification_image_sent, senderUser.name)
        }

        // Send the notification
        FCMHelper.sendNotificationToDevice(
            receiverUser.fcmToken,
            notificationMsg,
            Constants.NOTIFICATION_TYPE_CHAT,
            senderUser
        )
    }

    /**
     * Launches the camera to capture a new photo.
     * This function uses the activity result contract to start the camera activity to capture a new
     *  photo. The result is handled in the [takeImageResult] callback.
     */
    private fun takeImage() {
        Utils.getTmpFileUri(this).let { uri ->
            latestImgUri = uri
            takeImageResult.launch(uri)
        }
    }

    /**
     * Launches the activity to select an image from the gallery.
     * This function uses the activity result contract to start an activity to select an image
     *  from the gallery. The result is handled in the [selectImageFromGalleryResult] callback.
     */
    private fun selectImageFromGallery() {
        selectImageFromGalleryResult.launch("image/*")
    }

    /**
     * Uploads an image message to Firebase Storage and creates a new message document in the chat rooms.
     *
     * @param uri The URI of the image to be uploaded.
     */
    private fun uploadImageMessage(uri: Uri) {
        val directory = Constants.STORAGE_CHATS_IMAGES_PATH
        val imgReference = Utils.getImgPath(this, uri, directory)
        val storageReference = FirebaseStorage.getInstance().getReference(imgReference)

        dialog.show()

        storageReference.putFile(uri)
            .addOnFailureListener {
                handleUploadImageMessageFailure()
            }
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.downloadUrl
                    .addOnFailureListener {
                        handleUploadImageMessageFailure()
                    }
                    .addOnSuccessListener { uri ->
                        val downloadUrl = uri.toString()
                        createNewMessageDocument(imageUrl = downloadUrl)
                        dialog.dismiss()
                    }
            }
    }

    /**
     * Handles the failure of uploading an image message.
     * Shows an error toast message and dismisses the progress dialog.
     */
    private fun handleUploadImageMessageFailure() {
        val msg = getString(R.string.error_operation_could_not_be_done)
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

        dialog.dismiss()
    }

    /**
     * Deletes the selected message for everyone in the chat.
     *
     * This function updates the 'deleted' field of the selected message to true for both the sender and receiver rooms.
     * The message will no longer be visible to all participants in the chat.
     */
    private fun deleteMessageForEveryone() {
        val db = Firebase.firestore
        val chatsCollection = db.collection(Constants.COLLECTION_CHATS)

        selectedMessage.messageId?.let { _ ->
            chatsCollection.document(senderRoom)
                .collection(Constants.CHATS_COLLECTION_MESSAGES)
                .document(selectedMessage.messageId.toString())
                .update(Constants.CHATS_FIELD_DELETED, true)
        }

        selectedMessage.messageId?.let { _ ->
            chatsCollection.document(receiverRoom)
                .collection(Constants.CHATS_COLLECTION_MESSAGES)
                .document(selectedMessage.messageId.toString())
                .update(Constants.CHATS_FIELD_DELETED, true)
        }
    }

    /**
     * Deletes the selected message for the current user only.
     *
     * This function deletes the selected message document from the sender room's collection,
     * making it no longer visible to the current user.
     */
    private fun deleteMessageForMe() {
        val db = Firebase.firestore
        val chatsCollection = db.collection(Constants.COLLECTION_CHATS)

        selectedMessage.messageId?.let { _ ->
            chatsCollection.document(senderRoom)
                .collection(Constants.CHATS_COLLECTION_MESSAGES)
                .document(selectedMessage.messageId.toString())
                .delete()
        }
    }
}